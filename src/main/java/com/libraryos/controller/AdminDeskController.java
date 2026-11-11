package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.audit.AuditLog;
import com.libraryos.service.AuditService;
import com.libraryos.service.BackupService;
import com.libraryos.view.AdminDeskView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AdminDeskController {

    private static final Logger log = LoggerFactory.getLogger(AdminDeskController.class);

    private final AdminDeskView view;
    private final AuditService auditService;
    private final BackupService backupService;

    public AdminDeskController(AdminDeskView view) {
        this.view = view;
        this.auditService = AppContext.getInstance().getAuditService();
        this.backupService = AppContext.getInstance().getBackupService();

        initialize();
    }

    private void initialize() {
        boolean canAudit = SessionManager.hasPermission("AUDIT_VIEW");
        boolean canBackup = SessionManager.hasPermission("BACKUP_MANAGE");

        view.getCreateBackupBtn().setDisable(!canBackup);
        view.getRestoreBackupBtn().setDisable(true);

        // Header
        view.getRefreshBtn().setOnAction(e -> handleRefreshActiveTab());

        // Tab 1: Audit Wiring
        view.getAuditFilterBtn().setOnAction(e -> loadAuditLogs());
        view.getAuditSearchField().setOnAction(e -> loadAuditLogs());
        view.getAuditActionFilter().valueProperty().addListener((obs, o, n) -> loadAuditLogs());
        view.getAuditEntityFilter().valueProperty().addListener((obs, o, n) -> loadAuditLogs());
        view.getAuditResetBtn().setOnAction(e -> {
            view.getAuditSearchField().clear();
            view.getAuditActionFilter().setValue("ALL");
            view.getAuditEntityFilter().setValue("ALL");
            loadAuditLogs();
        });

        view.getAuditTable().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, item) -> {
            if (item != null) {
                String detail = "Action: " + item.getAction() +
                        " | Actor: " + (item.getActorUsername() != null ? item.getActorUsername() : "User #" + item.getActorUserId()) +
                        " | Time: " + (item.getCreatedAt() != null ? DateTimeUtil.formatDateTime(item.getCreatedAt()) : "—") +
                        " | Target: " + item.getEntityType() + (item.getEntityId() != null ? " #" + item.getEntityId() : "") +
                        "\nSummary: " + item.getSummary() +
                        (item.getReason() != null ? "\nJustification: " + item.getReason() : "");
                view.getAuditDetailText().setText(detail);
            } else {
                view.getAuditDetailText().setText("Select an audit entry above to view full action details and rationale.");
            }
        });

        // Tab 2: Backup Wiring
        view.getCreateBackupBtn().setOnAction(e -> handleCreateBackup());
        view.getRestoreBackupBtn().setOnAction(e -> handleRestoreBackup());
        view.getRefreshBackupsBtn().setOnAction(e -> loadBackups());

        view.getBackupsTable().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, item) -> {
            view.getRestoreBackupBtn().setDisable(item == null || !canBackup);
        });

        // Tab switch listener
        view.getTabPane().getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == view.getBackupTab() && view.getBackupsTable().getItems().isEmpty()) {
                loadBackups();
            }
        });

        // Initial Load on default tab (Audit)
        loadAuditLogs();
    }

    private void handleRefreshActiveTab() {
        if (view.getTabPane().getSelectionModel().getSelectedItem() == view.getAuditTab()) {
            loadAuditLogs();
        } else if (view.getTabPane().getSelectionModel().getSelectedItem() == view.getBackupTab()) {
            loadBackups();
        }
    }

    public void loadAuditLogs() {
        String q = view.getAuditSearchField().getText() != null ? view.getAuditSearchField().getText().trim() : "";
        String action = view.getAuditActionFilter().getValue();
        String entity = view.getAuditEntityFilter().getValue();

        Task<List<AuditLog>> task = new Task<>() {
            @Override
            protected List<AuditLog> call() {
                return auditService.searchLogs(q, action, entity, 200);
            }
        };

        task.setOnSucceeded(e -> {
            List<AuditLog> logs = task.getValue();
            view.getAuditTable().setItems(FXCollections.observableArrayList(logs));
            view.getAuditCountLabel().setText("Showing " + logs.size() + " audit event(s)");
        });

        task.setOnFailed(e -> {
            log.error("Failed to load audit logs", task.getException());
            showError("Audit Load Error", task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    public void loadBackups() {
        Task<List<BackupService.BackupFileItem>> task = new Task<>() {
            @Override
            protected List<BackupService.BackupFileItem> call() {
                return backupService.listBackups();
            }
        };

        task.setOnSucceeded(e -> {
            List<BackupService.BackupFileItem> backups = task.getValue();
            view.getBackupsTable().setItems(FXCollections.observableArrayList(backups));
            view.getBackupsCountLabel().setText(backups.size() + " backup snapshot(s) stored in ~/.libraryos/backups/");
        });

        task.setOnFailed(e -> {
            log.error("Failed to list backups", task.getException());
            showError("Backup Error", task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void handleCreateBackup() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Create Database Snapshot Backup");
        String defaultName = "libraryos_backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        fc.setInitialFileName(defaultName);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Script (*.sql)", "*.sql"));

        File file = fc.showSaveDialog(view.getScene().getWindow());
        if (file == null) return;

        Task<File> task = new Task<>() {
            @Override
            protected File call() {
                return backupService.createBackup(file);
            }
        };

        task.setOnSucceeded(e -> {
            File created = task.getValue();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Backup Successful");
            alert.setHeaderText("Database Snapshot Created");
            alert.setContentText("Backup successfully saved to:\n" + created.getAbsolutePath() +
                    "\nFile size: " + String.format("%.2f KB", created.length() / 1024.0));
            alert.showAndWait();
            loadBackups();
            if (!view.getAuditTable().getItems().isEmpty()) {
                loadAuditLogs();
            }
        });

        task.setOnFailed(e -> {
            log.error("Failed to create database backup", task.getException());
            showError("Backup Failed", task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void handleRestoreBackup() {
        BackupService.BackupFileItem selected = view.getBackupsTable().getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restore Database Snapshot");
        confirm.setHeaderText("DANGER: Overwrite database with " + selected.getFileName() + "?");
        confirm.setContentText("Restoring this database snapshot will overwrite all current tables and revert the database state to " +
                DateTimeUtil.formatDateTime(selected.getModifiedAt()) + ".\n\nAre you sure you wish to proceed?");

        Optional<ButtonType> resp = confirm.showAndWait();
        if (resp.isPresent() && resp.get() == ButtonType.OK) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    backupService.restoreBackup(selected.getFile());
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Restore Complete");
                info.setHeaderText("Database Restored Successfully");
                info.setContentText("The database has been restored from " + selected.getFileName() + ".");
                info.showAndWait();
                loadAuditLogs();
            });

            task.setOnFailed(e -> {
                log.error("Failed to restore database", task.getException());
                showError("Restore Failed", task.getException().getMessage());
            });

            AppContext.getInstance().getExecutorService().execute(task);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

