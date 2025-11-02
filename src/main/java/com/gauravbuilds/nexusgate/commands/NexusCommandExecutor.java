package com.gauravbuilds.nexusgate.commands;

import com.gauravbuilds.nexusgate.NexusGate;
import com.gauravbuilds.nexusgate.data.PlayerData;
import com.gauravbuilds.nexusgate.listeners.EditorListener;
import com.gauravbuilds.nexusgate.managers.DataManager;
import com.gauravbuilds.nexusgate.managers.DatabaseManager;
import com.gauravbuilds.nexusgate.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class NexusCommandExecutor implements CommandExecutor {

    private final NexusGate plugin;
    private final EditorListener editorListener;

    public NexusCommandExecutor(NexusGate plugin, EditorListener editorListener) {
        this.plugin = plugin;
        this.editorListener = editorListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("sync")) {
                if (!player.hasPermission("nexusgate.sync")) {
                    LanguageManager.sendMessage(player, "no-permission");
                    return true;
                }
                // Manual synchronous save and load
                LanguageManager.sendMessage(player, "sync-in-progress");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        PlayerData dataToSave = DataManager.createPlayerDataFromPlayer(player);
                        DatabaseManager.savePlayerData(dataToSave).join(); // Synchronous save

                        CompletableFuture<PlayerData> futureData = DatabaseManager.loadPlayerData(player.getUniqueId());
                        futureData.thenAccept(loadedData -> {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (loadedData != null) {
                                        DataManager.applyPlayerData(player, loadedData);
                                        LanguageManager.sendMessage(player, "sync-success-manual");
                                    } else {
                                        LanguageManager.sendMessage(player, "error-database");
                                        plugin.getLogger().warning("Failed to load data for " + player.getName() + " after manual sync.");
                                    }
                                }
                            }.runTask(plugin);
                        }).exceptionally(ex -> {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    LanguageManager.sendMessage(player, "error-database");
                                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error during manual sync for " + player.getName(), ex);
                                }
                            }.runTask(plugin);
                            return null;
                        });
                    }
                }.runTaskAsynchronously(plugin);
                return true;
            } else if (args[0].equalsIgnoreCase("editor")) {
                if (!player.hasPermission("nexusgate.admin.editor")) {
                    LanguageManager.sendMessage(player, "no-permission");
                    return true;
                }
                openEditorGUI(player);
                return true;
            } else if (args[0].equalsIgnoreCase("reset")) {
                if (!player.hasPermission("nexusgate.admin.reset")) {
                    LanguageManager.sendMessage(player, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    LanguageManager.sendMessage(player, "error-command-usage", "nexus reset <playername>");
                    return true;
                }
                String targetPlayerName = args[1];
                openResetConfirmationGUI(player, targetPlayerName);
                return true;
            }
        }

        // Placeholder for other /nexus commands
        LanguageManager.sendMessage(player, "error-command-usage", "nexus <sync|editor|reset>");
        return true;
    }

    private void openEditorGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getGuiEditorTitle());
        Inventory gui = Bukkit.createInventory(null, 27, title);

        ItemStack searchItem = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchItem.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName(ChatColor.YELLOW + "Search Player Data");
            searchMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to search for a player's data."));
            searchItem.setItemMeta(searchMeta);
        }

        gui.setItem(13, searchItem); // Center item in a 3x9 GUI (index 13)

        player.openInventory(gui);
    }

    private void openResetConfirmationGUI(Player player, String targetPlayerName) {
        editorListener.addAwaitingResetConfirmation(player.getUniqueId(), targetPlayerName);

        String title = ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfirmResetTitle());
        Inventory gui = Bukkit.createInventory(null, 9, title);

        ItemStack confirmItem = new ItemStack(Material.RED_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(ChatColor.RED + "Confirm Wipe for " + targetPlayerName);
            confirmMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to permanently wipe data for " + targetPlayerName + "."));
            confirmItem.setItemMeta(confirmMeta);
        }

        ItemStack cancelItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(ChatColor.GREEN + "Cancel");
            cancelMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to cancel data wipe."));
            cancelItem.setItemMeta(cancelMeta);
        }

        gui.setItem(3, confirmItem);
        gui.setItem(5, cancelItem);

        player.openInventory(gui);
    }
}
