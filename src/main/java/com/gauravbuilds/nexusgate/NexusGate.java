package com.gauravbuilds.nexusgate;

import com.gauravbuilds.nexusgate.commands.NexusCommandExecutor;
import com.gauravbuilds.nexusgate.listeners.EditorListener;
import com.gauravbuilds.nexusgate.listeners.SyncListener;
import com.gauravbuilds.nexusgate.managers.DatabaseManager;
import com.gauravbuilds.nexusgate.managers.LanguageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public final class NexusGate extends JavaPlugin {

    private static NexusGate plugin;

    // --- Configuration Fields ---
    private boolean pluginEnabled;
    private String chatPrefix;

    private boolean syncOnJoin;
    private boolean syncOnQuit;
    private boolean syncOnWorldChange;
    private List<String> syncExemptWorlds;
    private boolean syncInventory;
    private boolean syncEnderchest;
    private boolean syncHealth;
    private boolean syncFood;
    private boolean syncLevel;
    private boolean syncExperience;
    private boolean syncEffects;
    private boolean syncVaultBalance;

    private String guiEditorTitle;
    private String guiProfileTitle;
    private String guiConfirmResetTitle;

    private boolean sendSyncSuccessMessage;
    private boolean sendSyncSkipWorldMessage;

    @Override
    public void onEnable() {
        plugin = this;
        getLogger().log(Level.INFO, "NexusGate has been enabled!");

        // Save default config if it doesn't exist and reload it
        saveDefaultConfig();
        reloadConfig();

        // --- Load Configuration Values ---
        this.pluginEnabled = getConfig().getBoolean("general.enabled", true);
        if (!this.pluginEnabled) {
            getLogger().log(Level.WARNING, "NexusGate is disabled via config.yml. Plugin will not function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.chatPrefix = getConfig().getString("general.chat-prefix", "&8[&9NexusGate&8] &7");

        this.syncOnJoin = getConfig().getBoolean("sync.on-join", true);
        this.syncOnQuit = getConfig().getBoolean("sync.on-quit", true);
        this.syncOnWorldChange = getConfig().getBoolean("sync.on-world-change", true);
        this.syncExemptWorlds = getConfig().getStringList("sync.exempt-worlds");
        this.syncInventory = getConfig().getBoolean("sync.data-types.inventory", true);
        this.syncEnderchest = getConfig().getBoolean("sync.data-types.enderchest", true);
        this.syncHealth = getConfig().getBoolean("sync.data-types.health", true);
        this.syncFood = getConfig().getBoolean("sync.data-types.food", true);
        this.syncLevel = getConfig().getBoolean("sync.data-types.level", true);
        this.syncExperience = getConfig().getBoolean("sync.data-types.experience", true);
        this.syncEffects = getConfig().getBoolean("sync.data-types.effects", true);
        this.syncVaultBalance = getConfig().getBoolean("sync.data-types.vault-balance", true);

        this.guiEditorTitle = getConfig().getString("gui.editor-title", "&8&l:: &9&lNexus Core - Player Data &8&l::");
        this.guiProfileTitle = getConfig().getString("gui.profile-title", "&8&l:: &9&lProfile of %player% &8&l::");
        this.guiConfirmResetTitle = getConfig().getString("gui.confirm-reset-title", "&4&lConfirm Data Wipe");

        this.sendSyncSuccessMessage = getConfig().getBoolean("messages.send-sync-success", true);
        this.sendSyncSkipWorldMessage = getConfig().getBoolean("messages.send-sync-skip-world", true);

        // Initialize LanguageManager first
        LanguageManager.loadLanguages();

        // Attempting to initialize DatabaseManager with relocated MySQL driver
        getLogger().log(Level.INFO, "Attempting to initialize DatabaseManager with relocated MySQL driver...");
        // Initialize DatabaseManager asynchronously and wait for it to complete
        DatabaseManager.initializeDatabase().join();

        // Register listeners first, as command executor needs EditorListener instance
        SyncListener syncListener = new SyncListener(this);
        EditorListener editorListener = new EditorListener(this);
        getServer().getPluginManager().registerEvents(syncListener, this);
        getServer().getPluginManager().registerEvents(editorListener, this);

        // Register commands
        getCommand("nexus").setExecutor(new NexusCommandExecutor(this, editorListener));
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "NexusGate has been disabled!");
        DatabaseManager.close();
    }

    public static NexusGate getPlugin() {
        return plugin;
    }

    // --- Getters for Configuration Fields ---
    public boolean isPluginEnabled() { return pluginEnabled; }
    public String getChatPrefix() { return chatPrefix; }

    public boolean isSyncOnJoin() { return syncOnJoin; }
    public boolean isSyncOnQuit() { return syncOnQuit; }
    public boolean isSyncOnWorldChange() { return syncOnWorldChange; }
    public List<String> getSyncExemptWorlds() { return syncExemptWorlds; }
    public boolean isSyncInventory() { return syncInventory; }
    public boolean isSyncEnderchest() { return syncEnderchest; }
    public boolean isSyncHealth() { return syncHealth; }
    public boolean isSyncFood() { return syncFood; }
    public boolean isSyncLevel() { return syncLevel; }
    public boolean isSyncExperience() { return syncExperience; }
    public boolean isSyncEffects() { return syncEffects; }
    public boolean isSyncVaultBalance() { return syncVaultBalance; }

    public String getGuiEditorTitle() { return guiEditorTitle; }
    public String getGuiProfileTitle() { return guiProfileTitle; }
    public String getGuiConfirmResetTitle() { return guiConfirmResetTitle; }

    public boolean isSendSyncSuccessMessage() { return sendSyncSuccessMessage; }
    public boolean isSendSyncSkipWorldMessage() { return sendSyncSkipWorldMessage; }
}
