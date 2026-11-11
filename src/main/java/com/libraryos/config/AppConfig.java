package com.libraryos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Global application configuration holder with hierarchical property resolution:
 * Environment variables -> config/application.properties -> classpath:application.properties -> defaults.
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private AppConfig() {
    }

    private static void loadProperties() {
        // 1. Load from classpath
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                properties.load(in);
                log.info("Loaded default application properties from classpath");
            }
        } catch (Exception e) {
            log.warn("Could not load classpath application.properties: {}", e.getMessage());
        }

        // 2. Load from external file config/application.properties if present
        File[] searchLocations = new File[] {
                new File("config/application.properties"),
                new File("../config/application.properties"),
                new File(System.getProperty("user.home"), ".libraryos/config/application.properties")
        };

        for (File externalFile : searchLocations) {
            if (externalFile.exists()) {
                try (InputStream in = new FileInputStream(externalFile)) {
                    properties.load(in);
                    log.info("Loaded external override properties from {}", externalFile.getAbsolutePath());
                    break;
                } catch (Exception e) {
                    log.warn("Failed reading external properties from {}: {}", externalFile.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }

    public static String get(String key, String defaultValue) {
        // Check environment variable first (e.g. db.url -> LIBRARYOS_DB_URL)
        String envKey = "LIBRARYOS_" + key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }

        // Direct property
        String val = properties.getProperty(key);
        if (val != null && !val.isBlank()) {
            return resolvePlaceholders(val);
        }

        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    private static String resolvePlaceholders(String value) {
        if (value.contains("${user.home}")) {
            return value.replace("${user.home}", System.getProperty("user.home"));
        }
        return value;
    }

    // Specific typed getters
    public static String getDbUrl() {
        return get("db.url", "jdbc:mysql://localhost:3306/libraryos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
    }

    public static String getDbUsername() {
        return get("db.username", "root");
    }

    public static String getDbPassword() {
        return get("db.password", "");
    }

    public static int getDbMaxPoolSize() {
        return getInt("db.pool.maximumPoolSize", 10);
    }

    public static int getDbMinIdle() {
        return getInt("db.pool.minimumIdle", 2);
    }

    public static String getInstitutionName() {
        return get("institution.name", "Bangladesh University of Business and Technology (BUBT) Central Library");
    }

    public static String getInstitutionNameBn() {
        return get("institution.name_bn", "বিইউবিটি কেন্দ্রীয় গ্রন্থাগার");
    }

    public static String getCurrencySymbol() {
        return get("institution.currency", "৳");
    }

    public static int getMaxFailedLoginAttempts() {
        return getInt("auth.max_failed_attempts", 5);
    }

    public static int getLockoutMinutes() {
        return getInt("auth.lockout_minutes", 15);
    }

    public static String getBaseStorageDir() {
        return get("storage.base_dir", System.getProperty("user.home") + "/.libraryos/storage");
    }
}
