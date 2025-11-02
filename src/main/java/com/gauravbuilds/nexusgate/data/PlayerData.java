package com.gauravbuilds.nexusgate.data;

public class PlayerData {

    private String uuid;
    private String inventoryData;
    private String enderChestData;
    private double vaultBalance;
    private double health;
    private int food;
    private int level;
    private double totalExperience;
    private String effectData;

    public PlayerData(String uuid, String inventoryData, String enderChestData, double vaultBalance, double health, int food, int level, double totalExperience, String effectData) {
        this.uuid = uuid;
        this.inventoryData = inventoryData;
        this.enderChestData = enderChestData;
        this.vaultBalance = vaultBalance;
        this.health = health;
        this.food = food;
        this.level = level;
        this.totalExperience = totalExperience;
        this.effectData = effectData;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getInventoryData() {
        return inventoryData;
    }

    public void setInventoryData(String inventoryData) {
        this.inventoryData = inventoryData;
    }

    public String getEnderChestData() {
        return enderChestData;
    }

    public void setEnderChestData(String enderChestData) {
        this.enderChestData = enderChestData;
    }

    public double getVaultBalance() {
        return vaultBalance;
    }

    public void setVaultBalance(double vaultBalance) {
        this.vaultBalance = vaultBalance;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public int getFood() {
        return food;
    }

    public void setFood(int food) {
        this.food = food;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(double totalExperience) {
        this.totalExperience = totalExperience;
    }

    public String getEffectData() {
        return effectData;
    }

    public void setEffectData(String effectData) {
        this.effectData = effectData;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
               "uuid='" + uuid + '\'' +
               ", inventoryData='" + inventoryData + '\'' +
               ", enderChestData='" + enderChestData + '\'' +
               ", vaultBalance=" + vaultBalance +
               ", health=" + health +
               ", food=" + food +
               ", level=" + level +
               ", totalExperience=" + totalExperience +
               ", effectData='" + effectData + '\'' +
               '}';
    }
}
