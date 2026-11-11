package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.security.PermissionGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter BACKUP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AuditService auditService;
    private final File backupDir;

    public static class BackupFileItem {
        private final String fileName;
        private final long sizeBytes;
        private final LocalDateTime modifiedAt;
        private final File file;

        public BackupFileItem(File file) {
            this.file = file;
            this.fileName = file.getName();
            this.sizeBytes = file.length();
            this.modifiedAt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(file.lastModified()), java.time.ZoneId.systemDefault());
        }

        public String getFileName() { return fileName; }
        public long getSizeBytes() { return sizeBytes; }
        public String getSizeFormatted() {
            if (sizeBytes < 1024) return sizeBytes + " B";
            if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
            return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
        }
        public LocalDateTime getModifiedAt() { return modifiedAt; }
        public File getFile() { return file; }
    }

    public BackupService(AuditService auditService) {
        this.auditService = auditService;
        String userHome = System.getProperty("user.home", ".");
        this.backupDir = new File(userHome, ".libraryos" + File.separator + "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    public List<BackupFileItem> listBackups() {
        PermissionGuard.requirePermission("BACKUP_MANAGE");
        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".sql"));
        if (files == null || files.length == 0) return Collections.emptyList();

        List<BackupFileItem> list = new ArrayList<>();
        for (File f : files) {
            list.add(new BackupFileItem(f));
        }
        list.sort((a, b) -> b.getModifiedAt().compareTo(a.getModifiedAt()));
        return list;
    }

    public File createBackup(File targetFile) {
        PermissionGuard.requirePermission("BACKUP_MANAGE");

        File destination = targetFile;
        if (destination == null) {
            String timestamp = LocalDateTime.now().format(BACKUP_FORMATTER);
            destination = new File(backupDir, "libraryos_backup_" + timestamp + ".sql");
        }

        log.info("Starting database backup to: {}", destination.getAbsolutePath());

        try (Connection conn = DatabaseConfig.getConnection();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destination), StandardCharsets.UTF_8))) {

            writer.write("-- ============================================================\n");
            writer.write("-- LibraryOS SQL Database Snapshot\n");
            writer.write("-- Exported: " + LocalDateTime.now() + "\n");
            writer.write("-- ============================================================\n\n");
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");

            DatabaseMetaData meta = conn.getMetaData();
            List<String> tableNames = new ArrayList<>();

            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tName = rs.getString("TABLE_NAME");
                    // Exclude internal flyway history from manual restore dumps
                    if (!tName.equalsIgnoreCase("flyway_schema_history")) {
                        tableNames.add(tName);
                    }
                }
            }

            for (String table : tableNames) {
                dumpTableData(conn, table, writer);
            }

            writer.write("\nSET FOREIGN_KEY_CHECKS = 1;\n");
            writer.write("-- End of LibraryOS Database Snapshot\n");
            writer.flush();

            auditService.log("BACKUP_CREATED", "DATABASE", destination.getName(),
                    "Created database backup: " + destination.getName() + " (" + destination.length() + " bytes)", null);

            log.info("Database backup completed successfully ({} bytes)", destination.length());
            return destination;

        } catch (Exception e) {
            log.error("Database backup failed", e);
            throw new RuntimeException("Failed to create database backup: " + e.getMessage(), e);
        }
    }

    public void restoreBackup(File backupFile) {
        PermissionGuard.requirePermission("BACKUP_MANAGE");

        if (backupFile == null || !backupFile.exists()) {
            throw new IllegalArgumentException("Backup file does not exist: " + (backupFile != null ? backupFile.getAbsolutePath() : "null"));
        }

        log.info("Starting database restore from: {}", backupFile.getAbsolutePath());

        try (Connection conn = DatabaseConfig.getConnection();
             BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(backupFile), StandardCharsets.UTF_8));
             Statement stmt = conn.createStatement()) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isBlank()) continue;

                sb.append(line).append(" ");
                if (trimmed.endsWith(";")) {
                    String sql = sb.toString().trim();
                    if (!sql.isBlank()) {
                        try {
                            stmt.execute(sql);
                        } catch (SQLException ex) {
                            log.debug("Notice on statement execution during restore: {}", ex.getMessage());
                        }
                    }
                    sb.setLength(0);
                }
            }

            auditService.log("BACKUP_RESTORED", "DATABASE", backupFile.getName(),
                    "Restored database from snapshot: " + backupFile.getName(), null);

            log.info("Database restore from {} completed successfully", backupFile.getName());

        } catch (Exception e) {
            log.error("Database restore failed", e);
            throw new RuntimeException("Failed to restore database: " + e.getMessage(), e);
        }
    }

    private void dumpTableData(Connection conn, String table, BufferedWriter writer) throws SQLException, IOException {
        String countSql = "SELECT COUNT(*) FROM " + table;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                return; // skip empty tables
            }
        } catch (Exception ignored) {
            return;
        }

        writer.write("-- Data for table `" + table + "`\n");
        String selectSql = "SELECT * FROM " + table;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(selectSql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            while (rs.next()) {
                StringBuilder insert = new StringBuilder("INSERT INTO `").append(table).append("` (");
                for (int i = 1; i <= colCount; i++) {
                    insert.append("`").append(meta.getColumnName(i)).append("`");
                    if (i < colCount) insert.append(", ");
                }
                insert.append(") VALUES (");

                for (int i = 1; i <= colCount; i++) {
                    Object val = rs.getObject(i);
                    if (val == null) {
                        insert.append("NULL");
                    } else if (val instanceof Number || val instanceof Boolean) {
                        insert.append(val);
                    } else {
                        String s = val.toString().replace("'", "''").replace("\\", "\\\\");
                        insert.append("'").append(s).append("'");
                    }
                    if (i < colCount) insert.append(", ");
                }
                insert.append(");\n");
                writer.write(insert.toString());
            }
        }
        writer.write("\n");
    }
}

