package com.gauravbuilds.nexusgate.managers;

import com.gauravbuilds.nexusgate.NexusGate;
import com.gauravbuilds.nexusgate.data.PlayerData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private static HikariDataSource dataSource;

    public static CompletableFuture<Void> initializeDatabase() {
        return CompletableFuture.runAsync(() -> {
            NexusGate plugin = NexusGate.getPlugin();
            
            try {
                Class.forName("com.gauravbuilds.nexusgate.libs.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not find the relocated MySQL driver.", e);
                return;
            }

            HikariConfig config = new HikariConfig();
            String host = plugin.getConfig().getString("database.host", "localhost");
            String fallbackIp = plugin.getConfig().getString("database.host-ip-fallback", "");
            int port = plugin.getConfig().getInt("database.port", 3306);
            String database = plugin.getConfig().getString("database.database", "nexusgate");
            String username = plugin.getConfig().getString("database.username", "user");
            String password = plugin.getConfig().getString("database.password", "pass");

            String finalHost = (fallbackIp != null && !fallbackIp.isEmpty()) ? fallbackIp : host;
            if (fallbackIp != null && !fallbackIp.isEmpty()) {
                plugin.getLogger().info("Using provided host-ip-fallback: " + finalHost);
            }

            String jdbcUrl = "jdbc:mysql://" + finalHost + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false&serverTimezone=UTC";

            try {
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.setMinimumIdle(5);
                config.setMaximumPoolSize(10);
                config.setConnectionTimeout(30000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);

                dataSource = new HikariDataSource(config);

                // --- Schema Initialization and Update ---
                try (Connection connection = getConnection()) {
                    // 1. Create the table if it doesn't exist
                    try (Statement statement = connection.createStatement()) {
                        String createTableSql = "CREATE TABLE IF NOT EXISTS nexusgate_player_data (" +
                                                "UUID VARCHAR(36) PRIMARY KEY," +
                                                "inventoryData LONGTEXT," +
                                                "enderChestData LONGTEXT," +
                                                "vaultBalance DOUBLE," +
                                                "health DOUBLE," +
                                                "food INT" +
                                                ");";
                        statement.execute(createTableSql);
                    }
                    NexusGate.getPlugin().getLogger().info("Database table 'nexusgate_player_data' checked/created.");

                    // 2. Update the schema to add new columns if they are missing
                    updateSchema(connection);

                } catch (SQLException e) {
                    NexusGate.getPlugin().getLogger().log(Level.SEVERE, "Could not execute table creation or schema update: " + e.getMessage(), e);
                }

            } catch (Exception e) {
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "----------------------------------------------------");
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "ACTION REQUIRED: MySQL connection REFUSED or FAILED.");
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "Attempted to connect to: " + finalHost);
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "Please check your external database server's firewall, port (3306), and user permissions.");
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "If using a hostname, ensure it is correct or try the 'host-ip-fallback' option in config.yml.");
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "JDBC URL: " + jdbcUrl.replace(password, "****"));
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "Error: " + e.getMessage());
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "----------------------------------------------------");
            }
        });
    }

    private static void updateSchema(Connection connection) {
        NexusGate.getPlugin().getLogger().info("Checking database schema for required updates...");
        try {
            // Use a single method to check and add a column to reduce boilerplate
            addColumnIfNotExists(connection, "level", "INT");
            addColumnIfNotExists(connection, "totalExperience", "DOUBLE");
            addColumnIfNotExists(connection, "effectData", "LONGTEXT");
            
            NexusGate.getPlugin().getLogger().info("Database schema check complete. Table is structurally sound.");
        } catch (SQLException e) {
            NexusGate.getPlugin().getLogger().log(Level.SEVERE, "An error occurred during schema update.", e);
        }
    }

    private static void addColumnIfNotExists(Connection connection, String columnName, String columnType) throws SQLException {
        ResultSet rs = connection.getMetaData().getColumns(null, null, "nexusgate_player_data", columnName);
        if (!rs.next()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE nexusgate_player_data ADD COLUMN " + columnName + " " + columnType);
                NexusGate.getPlugin().getLogger().info("Added missing column '" + columnName + "' to the database table.");
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database connection pool is not initialized.");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            NexusGate.getPlugin().getLogger().info("Database connection pool closed.");
        }
    }

    public static CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nexusgate_player_data (UUID, inventoryData, enderChestData, vaultBalance, health, food, level, totalExperience, effectData) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                         "inventoryData = VALUES(inventoryData), enderChestData = VALUES(enderChestData), vaultBalance = VALUES(vaultBalance), " +
                         "health = VALUES(health), food = VALUES(food), level = VALUES(level), totalExperience = VALUES(totalExperience), effectData = VALUES(effectData);";
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, data.getUuid());
                ps.setString(2, data.getInventoryData());
                ps.setString(3, data.getEnderChestData());
                ps.setDouble(4, data.getVaultBalance());
                ps.setDouble(5, data.getHealth());
                ps.setInt(6, data.getFood());
                ps.setInt(7, data.getLevel());
                ps.setDouble(8, data.getTotalExperience());
                ps.setString(9, data.getEffectData());
                ps.executeUpdate();
            } catch (SQLException e) {
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "Failed to save player data for " + data.getUuid(), e);
            }
        });
    }

    public static CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM nexusgate_player_data WHERE UUID = ?;";
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new PlayerData(
                                rs.getString("UUID"),
                                rs.getString("inventoryData"),
                                rs.getString("enderChestData"),
                                rs.getDouble("vaultBalance"),
                                rs.getDouble("health"),
                                rs.getInt("food"),
                                rs.getInt("level"),
                                rs.getDouble("totalExperience"),
                                rs.getString("effectData")
                        );
                    }
                }
            } catch (SQLException e) {
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            }
            return null;
        });
    }

    public static CompletableFuture<Void> resetPlayerData(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM nexusgate_player_data WHERE UUID = ?;";
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
                NexusGate.getPlugin().getLogger().info("Player data for " + uuid + " has been reset.");
            } catch (SQLException e) {
                NexusGate.getPlugin().getLogger().log(Level.SEVERE, "Failed to reset player data for " + uuid, e);
            }
        });
    }
}
