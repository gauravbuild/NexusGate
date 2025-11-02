package com.gauravbuilds.nexusgate.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class PlayerDataGUI implements InventoryHolder {

    private final Inventory inventory;

    public PlayerDataGUI() {
        inventory = Bukkit.createInventory(this, 27, ChatColor.translateAlternateColorCodes('&', "&8&l:: &9&lNexus Core - Player Data &8&l::"));
        initializeItems();
    }

    private void initializeItems() {
        // Center a compass item for searching
        ItemStack searchItem = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchItem.getItemMeta();
        searchMeta.setDisplayName(ChatColor.AQUA + "Search Player Data");
        searchMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to search for a player's data."));
        searchItem.setItemMeta(searchMeta);

        // Place it in the center slot (slot 13 for a 3x9 GUI)
        inventory.setItem(13, searchItem);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
