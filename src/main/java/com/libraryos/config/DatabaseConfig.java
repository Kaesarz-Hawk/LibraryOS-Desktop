package com.libraryos.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages HikariCP connection pooling and automatic Flyway database migrations.
 * Handles MySQL connectivity gracefully if the server is offline or credentials require configuration.
 */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private static volatile DataSource dataSource;
    private static volatile boolean initialized = false;
    private static volatile boolean dbAvailable = false;
    private static volatile boolean isEmbedded = false;
    private static volatile String connectionErrorDetail = null;

    private DatabaseConfig() {
    }

    public static synchronized void initialize() {
        if (initialized && dataSource != null) {
            return;
        }

        String url = AppConfig.getDbUrl();
        String username = AppConfig.getDbUsername();
        String password = AppConfig.getDbPassword();

        log.info("Initializing HikariCP DataSource for URL: {}", url);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(AppConfig.getDbMaxPoolSize());
        config.setMinimumIdle(AppConfig.getDbMinIdle());
        config.setPoolName("LibraryOS-HikariPool");
        config.setConnectionTimeout(5000); // 5 sec fail fast for desktop UX
        config.setValidationTimeout(3000);
        config.setMaxLifetime(600000);
        config.setIdleTimeout(30000);

        // Performance & MySQL tuning
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            HikariDataSource ds = new HikariDataSource(config);
            // Quick connectivity test
            try (Connection conn = ds.getConnection()) {
                dbAvailable = true;
                connectionErrorDetail = null;
                log.info("Successfully connected to MySQL database: {}", conn.getCatalog());
            }
            dataSource = ds;
            initialized = true;

            // Execute Flyway migrations
            runMigrations(ds);

        } catch (Exception e) {
            log.warn("MySQL connection could not be established ({}). Falling back to embedded local database...", e.getMessage());
            initializeEmbeddedFallback(e.getMessage());
        }
    }

    private static void initializeEmbeddedFallback(String originalError) {
        try {
            String userHome = System.getProperty("user.home", ".");
            java.io.File dbDir = new java.io.File(userHome, ".libraryos/data");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            String dbPath = new java.io.File(dbDir, "libraryos").getAbsolutePath().replace('\\', '/');
            String h2Url = "jdbc:h2:" + dbPath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;AUTO_SERVER=TRUE";

            log.info("Initializing embedded persistent database at: {}", h2Url);

            HikariConfig h2Config = new HikariConfig();
            h2Config.setJdbcUrl(h2Url);
            h2Config.setUsername("sa");
            h2Config.setPassword("");
            h2Config.setDriverClassName("org.h2.Driver");
            h2Config.setPoolName("LibraryOS-EmbeddedPool");
            h2Config.setMaximumPoolSize(10);
            h2Config.setMinimumIdle(2);
            h2Config.setConnectionTimeout(5000);

            HikariDataSource h2Ds = new HikariDataSource(h2Config);
            runMigrations(h2Ds);

            dataSource = h2Ds;
            initialized = true;
            dbAvailable = true;
            isEmbedded = true;
            connectionErrorDetail = null;
            log.info("Embedded local database initialized successfully at {}", dbDir.getAbsolutePath());
        } catch (Exception ex) {
            log.error("Failed to initialize embedded fallback database: {}", ex.getMessage(), ex);
            dbAvailable = false;
            isEmbedded = false;
            connectionErrorDetail = originalError;
        }
    }

    public static boolean isEmbedded() {
        return isEmbedded;
    }

    public static synchronized void setDataSource(DataSource customDataSource) {
        if (dataSource instanceof HikariDataSource hikariDs && !hikariDs.isClosed()) {
            hikariDs.close();
        }
        dataSource = customDataSource;
        initialized = true;
        dbAvailable = true;
        isEmbedded = false;
        connectionErrorDetail = null;
    }

    public static void runMigrations(DataSource ds) {
        try {
            log.info("Executing Flyway database migrations...");
            Flyway flyway = Flyway.configure()
                    .dataSource(ds)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .cleanDisabled(true)
                    .load();

            int applied = flyway.migrate().migrationsExecuted;
            log.info("Flyway migration completed successfully. Applied {} new migration(s).", applied);
        } catch (Exception e) {
            log.error("Flyway migration failed: {}", e.getMessage(), e);
            throw new RuntimeException("Flyway migration failed: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            initialize();
        }
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized. Connection error: " + connectionErrorDetail);
        }
        return dataSource.getConnection();
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            initialize();
        }
        return dataSource;
    }

    public static boolean isDbAvailable() {
        return dbAvailable;
    }

    public static String getConnectionErrorDetail() {
        return connectionErrorDetail;
    }

    public static synchronized void shutdown() {
        if (dataSource instanceof HikariDataSource hikariDs && !hikariDs.isClosed()) {
            log.info("Closing HikariCP connection pool...");
            hikariDs.close();
            dataSource = null;
            initialized = false;
            dbAvailable = false;
        }
    }
}
