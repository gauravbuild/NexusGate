package com.gauravbuilds.nexusgate.listeners;

import com.gauravbuilds.nexusgate.NexusGate;
import com.gauravbuilds.nexusgate.managers.DatabaseManager;
import com.gauravbuilds.nexusgate.managers.LanguageManager;
import com.gauravbuilds.nexusgate.data.PlayerData;
import com.gauravbuilds.nexusgate.util.SerializationUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EditorListener implements Listener {

    private final NexusGate plugin;
    private final Map<UUID, PlayerData> playerProfileCache = new HashMap<>();
    private final Map<UUID, Boolean> awaitingPlayerNameInput = new HashMap<>();
    private final Map<UUID, String> awaitingResetConfirmation = new HashMap<>(); // New map for reset confirmation

    public EditorListener(NexusGate plugin) {
        this.plugin = plugin;
    }

    public void addAwaitingResetConfirmation(UUID playerUUID, String targetPlayerName) {
        this.awaitingResetConfirmation.put(playerUUID, targetPlayerName);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null || clickedItem == null || clickedItem.getType() == Material.AIR) return;

        String inventoryTitle = event.getView().getTitle();
        String editorTitle = ChatColor.translateAlternateColorCodes('&', plugin.getGuiEditorTitle());
        String profileTitlePrefix = ChatColor.translateAlternateColorCodes('&', plugin.getGuiProfileTitle().split("%player%")[0]);
        String confirmResetTitle = ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfirmResetTitle());

        // Handle clicks in the main editor GUI
        if (inventoryTitle.equals(editorTitle)) {
            event.setCancelled(true);

            if (clickedItem.getType() == Material.COMPASS && clickedItem.hasItemMeta() &&
                ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Search Player Data")) {

                player.closeInventory();
                LanguageManager.sendMessage(player, "editor-prompt-player-name");
                awaitingPlayerNameInput.put(player.getUniqueId(), true);
            }
        }

        // Handle clicks in the Player Profile GUI
        else if (inventoryTitle.startsWith(profileTitlePrefix)) {
            event.setCancelled(true);

            PlayerData targetPlayerData = playerProfileCache.get(player.getUniqueId());
            if (targetPlayerData == null) {
                player.sendMessage(ChatColor.RED + "Error: Player data not found in cache. Please search again.");
                player.closeInventory();
                return;
            }

            if (clickedItem.getType() == Material.DIAMOND_CHESTPLATE && clickedItem.hasItemMeta() &&
                ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("View Inventory")) {
                // Open read-only inventory view
                openReadOnlyInventoryView(player, targetPlayerData);
            } else if (clickedItem.getType() == Material.RED_WOOL && clickedItem.hasItemMeta() &&
                       ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Reset Data")) {
                // TODO: Implement reset data logic
                player.sendMessage(ChatColor.YELLOW + "Reset Data clicked for " + targetPlayerData.getUuid() + ". (Not yet implemented)");
            } else if (clickedItem.getType() == Material.PAPER && clickedItem.hasItemMeta() &&
                       ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Unsync Status")) {
                // TODO: Implement unsync status toggle
                player.sendMessage(ChatColor.YELLOW + "Unsync Status clicked for " + targetPlayerData.getUuid() + ". (Not yet implemented)");
            }
        }
        // Handle clicks in the Data Wipe Confirmation GUI
        else if (inventoryTitle.equals(confirmResetTitle)) {
            event.setCancelled(true);

            String targetPlayerName = awaitingResetConfirmation.get(player.getUniqueId());
            if (targetPlayerName == null) {
                player.sendMessage(ChatColor.RED + "Error: No pending data wipe confirmation.");
                player.closeInventory();
                return;
            }

            if (clickedItem.getType() == Material.RED_WOOL && clickedItem.hasItemMeta() &&
                ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).startsWith("Confirm Wipe")) {

                player.closeInventory();
                awaitingResetConfirmation.remove(player.getUniqueId());

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        UUID targetUUID = resolvePlayerUUID(targetPlayerName);
                        if (targetUUID == null) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    LanguageManager.sendMessage(player, "error-player-not-found", targetPlayerName);
                                }
                            }.runTask(plugin);
                            return;
                        }

                        DatabaseManager.resetPlayerData(targetUUID).thenRun(() -> {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    LanguageManager.sendMessage(player, "reset-success-admin", targetPlayerName);
                                }
                            }.runTask(plugin);
                        }).exceptionally(ex -> {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    LanguageManager.sendMessage(player, "error-database");
                                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error resetting data for " + targetUUID, ex);
                                }
                            }.runTask(plugin);
                            return null;
                        });
                    }
                }.runTaskAsynchronously(plugin);

            } else if (clickedItem.getType() == Material.GREEN_WOOL && clickedItem.hasItemMeta() &&
                       ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).equals("Cancel")) {
                LanguageManager.sendMessage(player, "reset-cancelled-admin", targetPlayerName);
                player.closeInventory();
                awaitingResetConfirmation.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (awaitingPlayerNameInput.containsKey(playerUUID) && awaitingPlayerNameInput.get(playerUUID)) {
            event.setCancelled(true);
            awaitingPlayerNameInput.remove(playerUUID);

            String playerName = event.getMessage();
            LanguageManager.sendMessage(player, "editor-searching-player", playerName);

            new BukkitRunnable() {
                @Override
                public void run() {
                    // Resolve UUID asynchronously
                    UUID targetUUID = resolvePlayerUUID(playerName);

                    if (targetUUID == null) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                LanguageManager.sendMessage(player, "error-player-not-found", playerName);
                            }
                        }.runTask(plugin);
                        return;
                    }

                    // Load player data asynchronously
                    CompletableFuture<PlayerData> futureData = DatabaseManager.loadPlayerData(targetUUID);
                    futureData.thenAccept(data -> {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (data != null) {
                                    playerProfileCache.put(playerUUID, data);
                                    openPlayerProfileGUI(player, data, playerName);
                                } else {
                                    LanguageManager.sendMessage(player, "error-data-not-found", playerName);
                                }
                            }
                        }.runTask(plugin);
                    }).exceptionally(ex -> {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                LanguageManager.sendMessage(player, "error-database");
                                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error loading player data for editor: " + targetUUID, ex);
                            }
                        }.runTask(plugin);
                        return null;
                    });
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    private UUID resolvePlayerUUID(String playerName) {
        // This method should ideally use an offline player lookup or Mojang API for accuracy.
        // For simplicity, we'll try to get an online player first.
        Player targetOnlinePlayer = Bukkit.getPlayer(playerName);
        if (targetOnlinePlayer != null) {
            return targetOnlinePlayer.getUniqueId();
        }

        // Fallback for offline players (might not be reliable for all servers/setups)
        // This is a blocking call, consider making it async if using Mojang API
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            return offlinePlayer.getUniqueId();
        }
        return null;
    }

    private void openPlayerProfileGUI(Player player, PlayerData data, String playerName) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getGuiProfileTitle().replace("%player%", playerName));
        Inventory gui = Bukkit.createInventory(null, 27, title);

        // View Inventory
        ItemStack viewInvItem = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta viewInvMeta = viewInvItem.getItemMeta();
        if (viewInvMeta != null) {
            viewInvMeta.setDisplayName(ChatColor.GREEN + "View Inventory");
            viewInvMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to view player's inventory."));
            viewInvItem.setItemMeta(viewInvMeta);
        }
        gui.setItem(10, viewInvItem);

        // Reset Data
        ItemStack resetDataItem = new ItemStack(Material.RED_WOOL);
        ItemMeta resetDataMeta = resetDataItem.getItemMeta();
        if (resetDataMeta != null) {
            resetDataMeta.setDisplayName(ChatColor.RED + "Reset Data");
            resetDataMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to reset all player data."));
            resetDataItem.setItemMeta(resetDataMeta);
        }
        gui.setItem(13, resetDataItem);

        // Unsync Status
        ItemStack unsyncStatusItem = new ItemStack(Material.PAPER);
        ItemMeta unsyncStatusMeta = unsyncStatusItem.getItemMeta();
        if (unsyncStatusMeta != null) {
            unsyncStatusMeta.setDisplayName(ChatColor.YELLOW + "Unsync Status");
            unsyncStatusMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to toggle player's sync status."));
            unsyncStatusItem.setItemMeta(unsyncStatusMeta);
        }
        gui.setItem(16, unsyncStatusItem);

        player.openInventory(gui);
    }

    private void openReadOnlyInventoryView(Player player, PlayerData data) {
        CompletableFuture.supplyAsync(() -> {
            // Perform deserialization asynchronously
            try {
                return SerializationUtil.base64ToItemStackArray(data.getInventoryData());
            } catch (Exception e) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error deserializing inventory for " + data.getUuid(), e);
                return new ItemStack[0];
            }
        }).thenAccept(inventoryContents -> {
            // Open GUI and set contents on the main thread
            new BukkitRunnable() {
                @Override
                public void run() {
                    String playerName = Bukkit.getOfflinePlayer(UUID.fromString(data.getUuid())).getName();
                    String title = ChatColor.translateAlternateColorCodes('&', "&8Inv of &7" + playerName);
                    Inventory invView = Bukkit.createInventory(null, 36, title);
                    invView.setContents(inventoryContents);
                    player.openInventory(invView);
                }
            }.runTask(plugin);
        }).exceptionally(ex -> {
            new BukkitRunnable() {
                @Override
                public void run() {
                    LanguageManager.sendMessage(player, "error-database");
                }
            }.runTask(plugin);
            return null;
        });
    }
}
