package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.repository.impl.JdbcAuditLogRepository;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceIntegrationTest {

    private static JdbcDataSource dataSource;
    private static Flyway flyway;
    private BackupService backupService;
    private File tempBackupFile;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_backup_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        DatabaseConfig.setDataSource(dataSource);
    }

    @BeforeEach
    void setUp() throws Exception {
        flyway.clean();
        flyway.migrate();

        AuditService auditService = new AuditService(new JdbcAuditLogRepository());
        backupService = new BackupService(auditService);

        UserSession adminSession = new UserSession(1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("BACKUP_MANAGE", "AUDIT_VIEW"));
        SessionManager.setSession(adminSession);

        tempBackupFile = File.createTempFile("libraryos_test_backup_", ".sql");
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
        if (tempBackupFile != null && tempBackupFile.exists()) {
            tempBackupFile.delete();
        }
    }

    @Test
    @DisplayName("Should create SQL database backup snapshot and restore successfully")
    void testCreateAndRestoreBackup() {
        File created = backupService.createBackup(tempBackupFile);

        assertNotNull(created);
        assertTrue(created.exists());
        assertTrue(created.length() > 500); // Contains schema DDL and seed table rows

        // Test restore
        assertDoesNotThrow(() -> backupService.restoreBackup(created));
    }

    @Test
    @DisplayName("Should list backup files in backup directory")
    void testListBackups() {
        List<BackupService.BackupFileItem> backups = backupService.listBackups();
        assertNotNull(backups);
    }
}

