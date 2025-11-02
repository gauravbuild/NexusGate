package com.gauravbuilds.nexusgate.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class InventoryViewGUI implements InventoryHolder {

    private final Inventory inventory;

    public InventoryViewGUI(ItemStack[] contents, String playerName) {
        String title = ChatColor.translateAlternateColorCodes('&', "&8&l:: &9&lInventory of " + playerName + " &8&l::");
        // A standard player inventory is 36 slots (main inventory) + 4 armor slots. Ender chest is 27.
        // For a general view, 6 rows (54 slots) is usually sufficient to display most inventories.
        inventory = Bukkit.createInventory(this, 54, title);
        setContents(contents);
    }

    private void setContents(ItemStack[] contents) {
        if (contents != null) {
            for (int i = 0; i < contents.length && i < inventory.getSize(); i++) {
                inventory.setItem(i, contents[i]);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
