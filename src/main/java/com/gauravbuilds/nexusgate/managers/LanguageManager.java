package com.gauravbuilds.nexusgate.managers;

import com.gauravbuilds.nexusgate.NexusGate;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LanguageManager {

    private static final Map<String, YamlConfiguration> languages = new HashMap<>();
    private static final String DEFAULT_LANGUAGE = "en_US";

    public static void loadLanguages() {
        NexusGate plugin = NexusGate.getPlugin();
        File localeFolder = new File(plugin.getDataFolder(), "locale");

        if (!localeFolder.exists()) {
            localeFolder.mkdirs();
            plugin.getLogger().info("Created locale folder.");
        }

        // Save default en_US.yml if it doesn't exist
        File enUSFile = new File(localeFolder, "en_US.yml");
        if (!enUSFile.exists()) {
            plugin.saveResource("locale/en_US.yml", false);
        }

        // Ensure default language is loaded first and always present
        try {
            YamlConfiguration defaultLangConfig = YamlConfiguration.loadConfiguration(enUSFile);
            languages.put(DEFAULT_LANGUAGE, defaultLangConfig);
            plugin.getLogger().info("Loaded default language file: " + enUSFile.getName());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load default language file: " + enUSFile.getName(), e);
        }

        File[] files = localeFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            plugin.getLogger().warning("No additional language files found in locale folder.");
            return;
        }

        for (File file : files) {
            String langCode = file.getName().replace(".yml", "");
            // Skip default language as it's already loaded
            if (langCode.equals(DEFAULT_LANGUAGE)) {
                continue;
            }
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                languages.put(langCode, config);
                plugin.getLogger().info("Loaded language file: " + file.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load language file: " + file.getName(), e);
            }
        }

        if (!languages.containsKey(DEFAULT_LANGUAGE)) {
            plugin.getLogger().severe("Default language " + DEFAULT_LANGUAGE + " is still missing after loading! Plugin may not function correctly.");
        }
    }

    public static String getMessage(String key, String langCode) {
        YamlConfiguration langConfig = languages.getOrDefault(langCode, languages.get(DEFAULT_LANGUAGE));
        if (langConfig == null) {
            return "Missing language config for key: " + key;
        }
        String message = langConfig.getString(key);
        if (message == null) {
            YamlConfiguration defaultLangConfig = languages.get(DEFAULT_LANGUAGE);
            if (defaultLangConfig != null) {
                message = defaultLangConfig.getString(key);
            }
        }
        return message != null ? message : "Missing message for key: " + key;
    }

    public static String getMessage(String key, String langCode, Object... args) {
        String message = getMessage(key, langCode);
        if (message != null) {
            return String.format(message, args);
        }
        return "Missing message for key: " + key;
    }

    public static void sendMessage(Player player, String key) {
        NexusGate plugin = NexusGate.getPlugin();
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getChatPrefix());
        String message = getMessage(key, DEFAULT_LANGUAGE); // Using default language for now
        if (message != null) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    public static void sendMessage(Player player, String key, Object... args) {
        NexusGate plugin = NexusGate.getPlugin();
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getChatPrefix());
        String message = getMessage(key, DEFAULT_LANGUAGE, args);
        if (message != null) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}
