package com.gauravbuilds.nexusgate.util;

import com.gauravbuilds.nexusgate.NexusGate;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;

public class SerializationUtil {

    /**
     * Converts an array of ItemStacks to a Base64 encoded string.
     *
     * @param items The array of ItemStacks to serialize.
     * @return A Base64 encoded string representing the ItemStacks, or null if an error occurs.
     */
    public static String itemStackArrayToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            NexusGate.getPlugin().getLogger().severe("Failed to serialize ItemStack array to Base64: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts a Base64 encoded string back to an array of ItemStacks.
     *
     * @param data The Base64 encoded string to deserialize.
     * @return An array of ItemStacks, or an empty array if an error occurs or data is null/empty.
     */
    public static ItemStack[] base64ToItemStackArray(String data) {
        if (data == null || data.isEmpty()) {
            return new ItemStack[0];
        }
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (ClassNotFoundException | IOException e) {
            NexusGate.getPlugin().getLogger().severe("Failed to deserialize Base64 to ItemStack array: " + e.getMessage());
            e.printStackTrace();
            return new ItemStack[0];
        }
    }

    /**
     * Converts a collection of PotionEffects to a Base64 encoded string.
     *
     * @param effects The collection of PotionEffects to serialize.
     * @return A Base64 encoded string representing the PotionEffects, or null if an error occurs.
     */
    public static String potionEffectCollectionToBase64(Collection<PotionEffect> effects) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(effects.size());
            for (PotionEffect effect : effects) {
                dataOutput.writeObject(effect);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            NexusGate.getPlugin().getLogger().severe("Failed to serialize PotionEffect collection to Base64: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts a Base64 encoded string back to a collection of PotionEffects.
     *
     * @param data The Base64 encoded string to deserialize.
     * @return A collection of PotionEffects, or an empty collection if an error occurs or data is null/empty.
     */
    public static Collection<PotionEffect> base64ToPotionEffectCollection(String data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int size = dataInput.readInt();
            Collection<PotionEffect> effects = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                effects.add((PotionEffect) dataInput.readObject());
            }
            dataInput.close();
            return effects;
        } catch (ClassNotFoundException | IOException e) {
            NexusGate.getPlugin().getLogger().severe("Failed to deserialize Base64 to PotionEffect collection: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
