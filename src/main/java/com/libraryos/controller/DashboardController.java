package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.dto.DashboardMetricsDTO;
import com.libraryos.model.audit.AuditLog;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.service.AuditService;
import com.libraryos.service.DashboardService;
import com.libraryos.view.DashboardView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardView view;
    private final DashboardService dashboardService;
    private final AuditService auditService;

    public DashboardController(DashboardView view) {
        this.view = view;
        this.dashboardService = AppContext.getInstance().getDashboardService();
        this.auditService = AppContext.getInstance().getAuditService();
        initialize();
    }

    private void initialize() {
        // Personalize greeting
        SessionManager.getCurrentSession().ifPresent(session -> {
            String greeting = "Welcome back, " + session.getFullName();
            if (session.getFullNameBn() != null && !session.getFullNameBn().isBlank()) {
                greeting += " (" + session.getFullNameBn() + ")";
            }
            view.getSubtitleLabel().setText(greeting + " • Role: " + session.getPrimaryRoleName());
        });

        // Enforce permissions on Quick Actions
        view.getIssueBookBtn().setDisable(!SessionManager.hasPermission(PermissionCode.LOAN_ISSUE));
        view.getReturnBookBtn().setDisable(!SessionManager.hasPermission(PermissionCode.LOAN_RETURN));
        view.getAddMemberBtn().setDisable(!SessionManager.hasPermission(PermissionCode.MEMBER_CREATE));
        view.getAddBookBtn().setDisable(!SessionManager.hasPermission(PermissionCode.BOOK_CREATE));

        // Wire quick action placeholders for Phase 1
        view.getIssueBookBtn().setOnAction(e -> showPhaseNotice("Circulation: Issue Book", "Scheduled for Phase 3 (Circulation Desk)"));
        view.getReturnBookBtn().setOnAction(e -> showPhaseNotice("Circulation: Return Book", "Scheduled for Phase 3 (Circulation Desk)"));
        view.getAddMemberBtn().setOnAction(e -> showPhaseNotice("Members: Register Member", "Scheduled for Phase 2 (Members & Policies)"));
        view.getAddBookBtn().setOnAction(e -> showPhaseNotice("Catalog: Add Book Title", "Scheduled for Phase 2 (Catalog & Inventory)"));

        // Refresh action
        view.getRefreshButton().setOnAction(e -> loadDashboardData());

        // Check DB connection status
        if (!DatabaseConfig.isDbAvailable()) {
            String detail = DatabaseConfig.getConnectionErrorDetail();
            view.showDbWarning("Database Notice: MySQL server is not connected. " +
                    (detail != null ? detail : "") + "\nPlease configure your credentials in config/application.properties or environment variables (LIBRARYOS_DB_USER / LIBRARYOS_DB_PASS).");
        } else {
            view.hideDbWarning();
        }

        // Initial background load
        loadDashboardData();
    }

    public void loadDashboardData() {
        Task<DashboardData> loadTask = new Task<>() {
            @Override
            protected DashboardData call() {
                DashboardMetricsDTO metrics;
                try {
                    metrics = dashboardService.getMetrics();
                } catch (Exception e) {
                    log.warn("Could not load metrics: {}", e.getMessage());
                    metrics = new DashboardMetricsDTO();
                }

                List<AuditLog> recentLogs;
                try {
                    if (SessionManager.hasPermission(PermissionCode.AUDIT_VIEW)) {
                        recentLogs = auditService.getRecentLogs(15);
                    } else {
                        recentLogs = Collections.emptyList();
                    }
                } catch (Exception e) {
                    log.warn("Could not load audit logs: {}", e.getMessage());
                    recentLogs = Collections.emptyList();
                }

                return new DashboardData(metrics, recentLogs);
            }
        };

        loadTask.setOnSucceeded(event -> {
            DashboardData data = loadTask.getValue();
            view.updateMetrics(data.metrics);
            view.getActivityTable().setItems(FXCollections.observableArrayList(data.logs));
        });

        loadTask.setOnFailed(event -> {
            log.error("Failed to load dashboard data", loadTask.getException());
        });

        AppContext.getInstance().getExecutorService().submit(loadTask);
    }

    private void showPhaseNotice(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("LibraryOS — " + title);
        alert.setHeaderText(title);
        alert.setContentText(message + "\nCore identity, permissions, and foundation services are verified in Phase 1.");
        alert.showAndWait();
    }

    private static class DashboardData {
        final DashboardMetricsDTO metrics;
        final List<AuditLog> logs;

        DashboardData(DashboardMetricsDTO metrics, List<AuditLog> logs) {
            this.metrics = metrics;
            this.logs = logs;
        }
    }
}

