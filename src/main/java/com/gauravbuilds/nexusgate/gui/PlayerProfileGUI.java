package com.gauravbuilds.nexusgate.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class PlayerProfileGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Player targetPlayer;

    public PlayerProfileGUI(Player targetPlayer) {
        this.targetPlayer = targetPlayer;
        String title = ChatColor.translateAlternateColorCodes('&', "&8&l:: &9&lProfile of " + targetPlayer.getName() + " &8&l::");
        inventory = Bukkit.createInventory(this, 27, title);
        initializeItems();
    }

    private void initializeItems() {
        // View Inventory Item
        ItemStack viewInventoryItem = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta viewInventoryMeta = viewInventoryItem.getItemMeta();
        viewInventoryMeta.setDisplayName(ChatColor.AQUA + "View Inventory");
        viewInventoryMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to view " + targetPlayer.getName() + "'s inventory."));
        viewInventoryItem.setItemMeta(viewInventoryMeta);
        inventory.setItem(10, viewInventoryItem); // Slot 10 (second row, second column)

        // Reset Data Item
        ItemStack resetDataItem = new ItemStack(Material.RED_WOOL);
        ItemMeta resetDataMeta = resetDataItem.getItemMeta();
        resetDataMeta.setDisplayName(ChatColor.RED + "Reset Player Data");
        resetDataMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to reset all data for " + targetPlayer.getName() + "."));
        resetDataItem.setItemMeta(resetDataMeta);
        inventory.setItem(13, resetDataItem); // Slot 13 (center)

        // Unsync Status Item
        ItemStack unsyncStatusItem = new ItemStack(Material.PAPER);
        ItemMeta unsyncStatusMeta = unsyncStatusItem.getItemMeta();
        unsyncStatusMeta.setDisplayName(ChatColor.YELLOW + "Unsync Status");
        unsyncStatusMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to toggle synchronization for " + targetPlayer.getName() + "."));
        unsyncStatusItem.setItemMeta(unsyncStatusMeta);
        inventory.setItem(16, unsyncStatusItem); // Slot 16 (second row, eighth column)
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }
}
