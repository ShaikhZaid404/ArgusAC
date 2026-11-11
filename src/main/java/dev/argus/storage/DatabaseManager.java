package dev.argus.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.argus.ArgusPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages high-performance connection pooling and schema migrations using SQLite.
 */
public final class DatabaseManager {

    private final ArgusPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ArgusPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void initialize() {
        File dbFile = new File(plugin.getDataFolder(), "argus.db");
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("Argus-SQLite-Pool");
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setMaximumPoolSize(8);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setMaxLifetime(60000);
        hikariConfig.setConnectionTimeout(10000);

        this.dataSource = new HikariDataSource(hikariConfig);
        createSchema();
    }

    private void createSchema() {
        String createTableQuery = """
            CREATE TABLE IF NOT EXISTS argus_players (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                ip_address VARCHAR(45) NOT NULL,
                first_seen BIGINT NOT NULL,
                last_seen BIGINT NOT NULL,
                trust_score INT DEFAULT 50,
                flags_count INT DEFAULT 0
            );
            CREATE INDEX IF NOT EXISTS idx_argus_ip ON argus_players (ip_address);
            """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableQuery);
            plugin.getLogger().info("[ArgusAC] SQLite database schema verified.");
        } catch (SQLException e) {
            plugin.getLogger().severe("[ArgusAC] Failed to initialize database schema: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource is closed or not initialized.");
        }
        return dataSource.getConnection();
    }

    public synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("[ArgusAC] Database connection pool closed.");
        }
    }
}
