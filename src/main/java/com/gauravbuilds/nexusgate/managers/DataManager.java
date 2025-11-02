package com.gauravbuilds.nexusgate.managers;

import com.gauravbuilds.nexusgate.NexusGate;
import com.gauravbuilds.nexusgate.data.PlayerData;
import com.gauravbuilds.nexusgate.util.SerializationUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataManager {

    public static CompletableFuture<PlayerData> loadPlayerData(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = DatabaseManager.getConnection();
                 PreparedStatement ps = connection.prepareStatement("SELECT * FROM nexusgate_player_data WHERE UUID = ?")) {

                ps.setString(1, playerUUID.toString());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String inventoryData = rs.getString("inventoryData");
                    String enderChestData = rs.getString("enderChestData");
                    double vaultBalance = rs.getDouble("vaultBalance");
                    double health = rs.getDouble("health");
                    int food = rs.getInt("food");
                    int level = rs.getInt("level");
                    double totalExperience = rs.getDouble("totalExperience");
                    String effectData = rs.getString("effectData");

                    return new PlayerData(playerUUID.toString(), inventoryData, enderChestData, vaultBalance, health, food, level, totalExperience, effectData);
                }
            } catch (SQLException e) {
                NexusGate.getPlugin().getLogger().severe("Could not load player data for " + playerUUID);
                e.printStackTrace();
            }
            return null;
        });
    }

    public static CompletableFuture<Void> savePlayerData(PlayerData playerData) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nexusgate_player_data (UUID, inventoryData, enderChestData, vaultBalance, health, food, level, totalExperience, effectData) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE " +
                         "inventoryData = VALUES(inventoryData), " +
                         "enderChestData = VALUES(enderChestData), " +
                         "vaultBalance = VALUES(vaultBalance), " +
                         "health = VALUES(health), " +
                         "food = VALUES(food), " +
                         "level = VALUES(level), " +
                         "totalExperience = VALUES(totalExperience), " +
                         "effectData = VALUES(effectData)";

            try (Connection connection = DatabaseManager.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, playerData.getUuid());
                ps.setString(2, playerData.getInventoryData());
                ps.setString(3, playerData.getEnderChestData());
                ps.setDouble(4, playerData.getVaultBalance());
                ps.setDouble(5, playerData.getHealth());
                ps.setInt(6, playerData.getFood());
                ps.setInt(7, playerData.getLevel());
                ps.setDouble(8, playerData.getTotalExperience());
                ps.setString(9, playerData.getEffectData());

                ps.executeUpdate();
            } catch (SQLException e) {
                NexusGate.getPlugin().getLogger().severe("Could not save player data for " + playerData.getUuid());
                e.printStackTrace();
            }
        });
    }

    public static PlayerData createPlayerDataFromPlayer(Player player) {
        NexusGate plugin = NexusGate.getPlugin();

        String inventoryData = plugin.isSyncInventory() ? SerializationUtil.itemStackArrayToBase64(player.getInventory().getContents()) : null;
        String enderChestData = plugin.isSyncEnderchest() ? SerializationUtil.itemStackArrayToBase64(player.getEnderChest().getContents()) : null;
        double vaultBalance = plugin.isSyncVaultBalance() ? 0.0 : 0.0; // Placeholder for Vault API, will be 0.0 if not synced
        double health = plugin.isSyncHealth() ? player.getHealth() : 20.0; // Default health if not synced
        int food = plugin.isSyncFood() ? player.getFoodLevel() : 20; // Default food if not synced
        int level = plugin.isSyncLevel() ? player.getLevel() : 0; // Default level if not synced
        double totalExperience = plugin.isSyncExperience() ? getTotalExperience(player) : 0.0; // Default XP if not synced
        String effectData = plugin.isSyncEffects() ? SerializationUtil.potionEffectCollectionToBase64(player.getActivePotionEffects()) : null;

        return new PlayerData(
            player.getUniqueId().toString(),
            inventoryData,
            enderChestData,
            vaultBalance,
            health,
            food,
            level,
            totalExperience,
            effectData
        );
    }

    public static CompletableFuture<Void> applyPlayerData(Player player, PlayerData data) {
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            NexusGate plugin = NexusGate.getPlugin();

            // Perform deserialization asynchronously based on config
            ItemStack[] inventoryContents = plugin.isSyncInventory() ? SerializationUtil.base64ToItemStackArray(data.getInventoryData()) : null;
            ItemStack[] enderChestContents = plugin.isSyncEnderchest() ? SerializationUtil.base64ToItemStackArray(data.getEnderChestData()) : null;
            Collection<PotionEffect> effectsToApply = plugin.isSyncEffects() ? SerializationUtil.base64ToPotionEffectCollection(data.getEffectData()) : null;

            // Apply data on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (plugin.isSyncInventory() && inventoryContents != null) {
                        player.getInventory().setContents(inventoryContents);
                    }
                    if (plugin.isSyncEnderchest() && enderChestContents != null) {
                        player.getEnderChest().setContents(enderChestContents);
                    }
                    if (plugin.isSyncHealth()) {
                        player.setHealth(data.getHealth());
                    }
                    if (plugin.isSyncFood()) {
                        player.setFoodLevel(data.getFood());
                    }

                    if (plugin.isSyncExperience()) {
                        // Reset current experience and then grant the total experience points
                        // to precisely restore the player's XP progress (level and progress bar).
                        player.setLevel(0);
                        player.setExp(0);
                        player.setTotalExperience(0);
                        player.giveExp((int) Math.round(data.getTotalExperience()));
                    } else if (plugin.isSyncLevel()) { // If only level is synced, set level directly
                        player.setLevel(data.getLevel());
                    }

                    if (plugin.isSyncEffects() && effectsToApply != null) {
                        for (PotionEffect effect : player.getActivePotionEffects()) {
                            player.removePotionEffect(effect.getType());
                        }
                        for (PotionEffect effect : effectsToApply) {
                            player.addPotionEffect(effect);
                        }
                    }
                    // Vault balance application would go here if Vault is integrated

                } catch (Exception e) {
                    NexusGate.getPlugin().getLogger().severe("Failed to apply player data for " + player.getName());
                    e.printStackTrace();
                }
            });
        });
    }

    private static double getTotalExperience(Player player) {
        int level = player.getLevel();
        float exp = player.getExp();
        double total = 0.0;

        if (level >= 0 && level <= 15) {
            total = (Math.pow(level, 2) + 6 * level);
            total += (2 * level + 7) * exp;
        } else if (level >= 16 && level <= 30) {
            total = (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
            total += (5 * level - 38) * exp;
        } else if (level >= 31) {
            total = (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
            total += (9 * level - 158) * exp;
        }
        return total;
    }
}
