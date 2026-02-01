package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.common.NetworkConfig;

import javax.sql.DataSource;

/**
 * Database connection manager using HikariCP connection pooling.
 * Supports sharding logic for routing queries to different databases.
 */
public class DatabaseConnection {

    private static final HikariDataSource dataSource;
    private static final String DB_URL;
    private static final String DB_USER;

    static {
        // Configuration from NetworkConfig (reads from config.properties or env vars)
        DB_URL = NetworkConfig.getDbUrl();
        DB_USER = NetworkConfig.getDbUser();
        String dbPassword = NetworkConfig.getDbPassword();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(dbPassword);

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);

        // MySQL specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        dataSource = new HikariDataSource(config);
    }

    /**
     * Gets the HikariCP data source for database connections.
     */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Gets database connection info for logging.
     */
    public static String getDatabaseInfo() {
        return DB_URL + " (user: " + DB_USER + ")";
    }

    /**
     * Determines which shard to use based on ID (simple modulo sharding).
     * Even IDs -> Shard A, Odd IDs -> Shard B
     */
    public static String getShardForId(long id) {
        return (id % 2 == 0) ? "SHARD_A" : "SHARD_B";
    }

    /**
     * Shuts down the connection pool gracefully.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
