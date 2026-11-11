package com.libraryos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Manages local persistent file storage paths for book covers, digital library PDFs, and database backups.
 */
public final class FileStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(FileStorageConfig.class);

    private static final String BASE_DIR = AppConfig.getBaseStorageDir();
    private static final String COVERS_DIR = BASE_DIR + File.separator + "covers";
    private static final String DIGITAL_DIR = BASE_DIR + File.separator + "digital";
    private static final String BACKUPS_DIR = AppConfig.get("storage.backups_dir", System.getProperty("user.home") + "/.libraryos/backups");
    private static final String LOGS_DIR = System.getProperty("user.home") + File.separator + ".libraryos" + File.separator + "logs";

    private FileStorageConfig() {
    }

    public static void initializeDirectories() {
        ensureDir(BASE_DIR);
        ensureDir(COVERS_DIR);
        ensureDir(DIGITAL_DIR);
        ensureDir(BACKUPS_DIR);
        ensureDir(LOGS_DIR);
    }

    private static void ensureDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("Created managed directory: {}", dir.getAbsolutePath());
            }
        }
    }

    public static String getBaseDir() {
        return BASE_DIR;
    }

    public static String getCoversDir() {
        return COVERS_DIR;
    }

    public static String getDigitalDir() {
        return DIGITAL_DIR;
    }

    public static String getBackupsDir() {
        return BACKUPS_DIR;
    }
}
