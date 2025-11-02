package com.gauravbuilds.nexusgate.listeners;

import com.gauravbuilds.nexusgate.NexusGate;
import com.gauravbuilds.nexusgate.data.PlayerData;
import com.gauravbuilds.nexusgate.managers.DataManager;
import com.gauravbuilds.nexusgate.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class SyncListener implements Listener {

    private final NexusGate plugin;

    public SyncListener(NexusGate plugin) {
        this.plugin = plugin;
    }

    private boolean isWorldExempt(String worldName) {
        List<String> exemptWorlds = plugin.getSyncExemptWorlds();
        return exemptWorlds.contains(worldName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isSyncOnJoin()) return;

        Player player = event.getPlayer();
        if (isWorldExempt(player.getWorld().getName())) {
            if (plugin.isSendSyncSkipWorldMessage()) {
                LanguageManager.sendMessage(player, "sync-skip-world");
            }
            plugin.getLogger().info("Skipping data load for " + player.getName() + " in exempt world: " + player.getWorld().getName());
            return;
        }

        plugin.getLogger().info("Attempting to load data for " + player.getName() + "...");
        DataManager.loadPlayerData(player.getUniqueId()).thenAccept(playerData -> {
            if (playerData != null) {
                DataManager.applyPlayerData(player, playerData);
                if (plugin.isSendSyncSuccessMessage()) {
                    LanguageManager.sendMessage(player, "sync-success-load");
                }
                plugin.getLogger().info("Successfully loaded and applied data for " + player.getName());
            } else {
                if (player.hasPlayedBefore()) {
                    plugin.getLogger().severe("CRITICAL: Failed to load data for existing player " + player.getName() + ". Kicking to prevent data loss.");
                    Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(LanguageManager.getMessage("error-sync-fail", "en_US")));
                } else {
                    plugin.getLogger().info("No data found for new player " + player.getName() + ". A new profile will be created on disconnect.");
                }
            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("CRITICAL: An exception occurred while loading data for " + player.getName() + ". Kicking to prevent data loss.");
            ex.printStackTrace();
            Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(LanguageManager.getMessage("error-sync-fail", "en_US")));
            return null;
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.isSyncOnQuit()) return;

        Player player = event.getPlayer();
        if (isWorldExempt(player.getWorld().getName())) {
            plugin.getLogger().info("Skipping data save for " + player.getName() + " in exempt world: " + player.getWorld().getName());
            return;
        }

        plugin.getLogger().info("Saving data for " + player.getName() + "...");
        PlayerData playerDataToSave = DataManager.createPlayerDataFromPlayer(player);

        DataManager.savePlayerData(playerDataToSave).thenRun(() -> {
            plugin.getLogger().info("Successfully saved data for " + player.getName());
        }).exceptionally(ex -> {
            plugin.getLogger().severe("An error occurred while saving data for " + player.getName() + ": " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!plugin.isSyncOnWorldChange()) return;

        Player player = event.getPlayer();
        boolean fromExempt = isWorldExempt(event.getFrom().getName());
        boolean toExempt = isWorldExempt(player.getWorld().getName());

        // Case 1: Moving from a synced world to an exempt world -> SAVE data
        if (!fromExempt && toExempt) {
            plugin.getLogger().info("Player " + player.getName() + " moved to an exempt world. Saving data...");
            PlayerData playerDataToSave = DataManager.createPlayerDataFromPlayer(player);
            DataManager.savePlayerData(playerDataToSave);
            if (plugin.isSendSyncSkipWorldMessage()) {
                LanguageManager.sendMessage(player, "sync-skip-world");
            }
        }
        // Case 2: Moving from an exempt world to a synced world -> LOAD data
        else if (fromExempt && !toExempt) {
            plugin.getLogger().info("Player " + player.getName() + " moved to a synced world. Loading data...");
            DataManager.loadPlayerData(player.getUniqueId()).thenAccept(playerData -> {
                if (playerData != null) {
                    DataManager.applyPlayerData(player, playerData);
                    if (plugin.isSendSyncSuccessMessage()) {
                        LanguageManager.sendMessage(player, "sync-success-load");
                    }
                }
            });
        }
        // Case 3: Moving between two synced worlds or two exempt worlds -> DO NOTHING
    }
}
