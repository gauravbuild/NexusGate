package com.gauravbuilds.nexusgate.commands;

import com.gauravbuilds.nexusgate.NexusGate;
import com.gauravbuilds.nexusgate.data.PlayerData;
import com.gauravbuilds.nexusgate.managers.DataManager;
import com.gauravbuilds.nexusgate.managers.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ExecutionException;

public class SyncCommand implements CommandExecutor {

    private final NexusGate plugin;

    public SyncCommand(NexusGate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nexusgate.sync")) {
            LanguageManager.sendMessage(player, "no-permission"); // Assuming a generic no-permission message exists
            return true;
        }

        // Perform synchronous save and load
        try {
            // Save data
            PlayerData playerDataToSave = DataManager.createPlayerDataFromPlayer(player);
            DataManager.savePlayerData(playerDataToSave).get(); // .get() makes it synchronous
            plugin.getLogger().info("Manually saved data for " + player.getName());

            // Load data
            PlayerData loadedPlayerData = DataManager.loadPlayerData(player.getUniqueId()).get(); // .get() makes it synchronous
            if (loadedPlayerData != null) {
                DataManager.applyPlayerData(player, loadedPlayerData);
                LanguageManager.sendMessage(player, "sync-success-manual");
                plugin.getLogger().info("Manually loaded and applied data for " + player.getName());
            } else {
                // This case should ideally not happen after a save, but handle defensively.
                LanguageManager.sendMessage(player, "error-sync-fail");
                plugin.getLogger().severe("Failed to load data immediately after saving for " + player.getName());
            }
        } catch (InterruptedException | ExecutionException e) {
            LanguageManager.sendMessage(player, "error-sync-fail");
            plugin.getLogger().severe("Error during manual sync for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
