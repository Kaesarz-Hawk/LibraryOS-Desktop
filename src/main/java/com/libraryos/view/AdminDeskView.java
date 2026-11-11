package com.libraryos.view;

import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.audit.AuditLog;
import com.libraryos.service.BackupService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AdminDeskView extends BorderPane {

    private final Button refreshBtn = new Button("↻ Refresh");
    private final TabPane tabPane = new TabPane();

    // --- Tab 1: Audit Logs ---
    private final Tab auditTab = new Tab("Audit Trail & Compliance (অডিট ট্রেইল)");
    private final TextField auditSearchField = new TextField();
    private final ComboBox<String> auditActionFilter = new ComboBox<>();
    private final ComboBox<String> auditEntityFilter = new ComboBox<>();
    private final Button auditFilterBtn = new Button("Filter Logs");
    private final Button auditResetBtn = new Button("Reset");
    private final TableView<AuditLog> auditTable = new TableView<>();
    private final Label auditCountLabel = new Label("Showing 0 audit events");
    private final Label auditDetailText = new Label("Select an audit entry above to view full action details and rationale.");

    // --- Tab 2: Database Backup & Restore ---
    private final Tab backupTab = new Tab("Database Backup & Recovery (ডাটাবেজ ব্যাকআপ)");
    private final Button createBackupBtn = new Button("+ Create Backup Snapshot (নতুন ব্যাকআপ)");
    private final Button restoreBackupBtn = new Button("⚠️ Restore Selected Backup (পুনরুদ্ধার)");
    private final Button refreshBackupsBtn = new Button("↻ Refresh List");
    private final TableView<BackupService.BackupFileItem> backupsTable = new TableView<>();
    private final Label backupsCountLabel = new Label("0 backup snapshots found");

    // --- Tab 3: System Profile ---
    private final Tab profileTab = new Tab("Institutional Profile & System Info (তথ্য)");

    public AdminDeskView() {
        getStyleClass().add("catalog-view");
        setPadding(new Insets(24));

        setTop(buildHeader());
        setCenter(buildCenterTabs());
    }

    private VBox buildHeader() {
        VBox topBox = new VBox(12);

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("System Administration & Maintenance / প্রশাসন ও ডাটাবেজ");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Monitor institutional audit events, create on-demand SQL database backups, and inspect system integrity");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        refreshBtn.getStyleClass().add("btn-secondary");
        headerRow.getChildren().addAll(titleBox, spacer, refreshBtn);

        topBox.getChildren().add(headerRow);
        return topBox;
    }

    private Node buildCenterTabs() {
        tabPane.getStyleClass().add("sub-tabpane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        auditTab.setContent(buildAuditTabContent());
        backupTab.setContent(buildBackupTabContent());
        profileTab.setContent(buildProfileTabContent());

        tabPane.getTabs().addAll(auditTab, backupTab, profileTab);
        return tabPane;
    }

    // --- TAB 1: AUDIT LOGS ---
    private Node buildAuditTabContent() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(16, 0, 0, 0));

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(12, 16, 12, 16));
        filterBar.getStyleClass().add("filter-container");

        auditSearchField.setPromptText("Search actor, summary, reason...");
        auditSearchField.getStyleClass().add("form-input");
        auditSearchField.setPrefWidth(260);

        auditActionFilter.getItems().addAll("ALL", "LOAN_ISSUED", "LOAN_RETURNED", "LOAN_RENEWED", "FINE_CREATED", "FINE_WAIVED", "PAYMENT_RECORDED", "PAYMENT_REVERSED", "BACKUP_CREATED", "BACKUP_RESTORED");
        auditActionFilter.setValue("ALL");
        auditActionFilter.getStyleClass().add("form-input");

        auditEntityFilter.getItems().addAll("ALL", "LOANS", "FINES", "PAYMENTS", "MEMBERS", "CATALOG", "DATABASE", "AUTH");
        auditEntityFilter.setValue("ALL");
        auditEntityFilter.getStyleClass().add("form-input");

        auditFilterBtn.getStyleClass().add("btn-primary");
        auditResetBtn.getStyleClass().add("btn-secondary");

        filterBar.getChildren().addAll(
                new Label("Search:"), auditSearchField,
                new Label("Action:"), auditActionFilter,
                new Label("Entity:"), auditEntityFilter,
                auditFilterBtn, auditResetBtn
        );

        TableColumn<AuditLog, String> colId = new TableColumn<>("#");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(55);

        TableColumn<AuditLog, String> colDate = new TableColumn<>("Timestamp");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? DateTimeUtil.formatDateTime(d.getValue().getCreatedAt()) : "—"
        ));
        colDate.setPrefWidth(150);

        TableColumn<AuditLog, String> colActor = new TableColumn<>("Actor / ব্যবহারকারী");
        colActor.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getActorUsername() != null ? d.getValue().getActorUsername() : "System / User #" + d.getValue().getActorUserId()
        ));
        colActor.setStyle("-fx-font-weight: bold;");
        colActor.setPrefWidth(140);

        TableColumn<AuditLog, String> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAction()));
        colAction.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String action, boolean empty) {
                super.updateItem(action, empty);
                if (empty || action == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(action);
                    if (action.contains("WAIVE") || action.contains("REVERSE")) {
                        badge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 10px;");
                    } else if (action.contains("RETURN") || action.contains("PAYMENT")) {
                        badge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 10px;");
                    } else if (action.contains("ISSUE") || action.contains("BACKUP")) {
                        badge.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 10px;");
                    } else {
                        badge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-size: 10px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colAction.setPrefWidth(140);

        TableColumn<AuditLog, String> colEntity = new TableColumn<>("Entity");
        colEntity.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEntityType() + (d.getValue().getEntityId() != null ? " #" + d.getValue().getEntityId() : "")
        ));
        colEntity.setPrefWidth(120);

        TableColumn<AuditLog, String> colSummary = new TableColumn<>("Summary / বিবরণ");
        colSummary.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSummary()));
        colSummary.setPrefWidth(280);

        TableColumn<AuditLog, String> colReason = new TableColumn<>("Reason / কারণ");
        colReason.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReason() != null ? d.getValue().getReason() : "—"));
        colReason.setPrefWidth(180);

        auditTable.getColumns().addAll(colId, colDate, colActor, colAction, colEntity, colSummary, colReason);
        auditTable.setPlaceholder(new Label("No audit logs matching query."));
        auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(auditTable, Priority.ALWAYS);

        // Detail Inspector Card
        VBox detailCard = new VBox(4);
        detailCard.setPadding(new Insets(10, 14, 10, 14));
        detailCard.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 6px; -fx-border-color: #E2E8F0; -fx-border-radius: 6px;");
        Label detailTitle = new Label("AUDIT EVENT INSPECTOR / বিস্তারিত তথ্য");
        detailTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        auditDetailText.setWrapText(true);
        auditDetailText.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E293B;");
        detailCard.getChildren().addAll(detailTitle, auditDetailText);

        HBox footer = new HBox(auditCountLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        auditCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        layout.getChildren().addAll(filterBar, auditTable, detailCard, footer);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    // --- TAB 2: DATABASE BACKUP & RESTORE ---
    private Node buildBackupTabContent() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(16, 0, 0, 0));

        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.getStyleClass().add("toolbar-panel");

        createBackupBtn.getStyleClass().add("btn-primary");
        createBackupBtn.setStyle("-fx-background-color: #16A34A; -fx-text-fill: white; -fx-font-weight: bold;");

        restoreBackupBtn.getStyleClass().add("btn-secondary");
        restoreBackupBtn.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        restoreBackupBtn.setDisable(true);

        refreshBackupsBtn.getStyleClass().add("btn-secondary");

        toolbar.getChildren().addAll(createBackupBtn, restoreBackupBtn, refreshBackupsBtn);

        // Security Notice Banner
        VBox noticeBox = new VBox(4);
        noticeBox.setPadding(new Insets(12));
        noticeBox.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #BFDBFE; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        Label nTitle = new Label("ℹ Automated Snapshot Architecture");
        nTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4ED8;");
        Label nDesc = new Label("Backups capture the full relational database structure and table data as UTF-8 SQL DDL/DML statements.\nBackups are saved by default to ~/.libraryos/backups/ and logged permanently in the institutional audit log.");
        nDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E40AF;");
        noticeBox.getChildren().addAll(nTitle, nDesc);

        // Backups Table
        TableColumn<BackupService.BackupFileItem, String> colName = new TableColumn<>("Backup File Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFileName()));
        colName.setStyle("-fx-font-weight: bold;");
        colName.setPrefWidth(280);

        TableColumn<BackupService.BackupFileItem, String> colSize = new TableColumn<>("File Size");
        colSize.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSizeFormatted()));
        colSize.setPrefWidth(110);

        TableColumn<BackupService.BackupFileItem, String> colDate = new TableColumn<>("Created Timestamp");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(DateTimeUtil.formatDateTime(d.getValue().getModifiedAt())));
        colDate.setPrefWidth(160);

        TableColumn<BackupService.BackupFileItem, String> colPath = new TableColumn<>("Disk Location");
        colPath.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFile().getAbsolutePath()));
        colPath.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        colPath.setPrefWidth(300);

        backupsTable.getColumns().addAll(colName, colSize, colDate, colPath);
        backupsTable.setPlaceholder(new Label("No database backups found in ~/.libraryos/backups/"));
        backupsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(backupsTable, Priority.ALWAYS);

        HBox footer = new HBox(backupsCountLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        backupsCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        layout.getChildren().addAll(toolbar, noticeBox, backupsTable, footer);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    // --- TAB 3: SYSTEM PROFILE ---
    private Node buildProfileTabContent() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        VBox card = new VBox(14);
        card.setPadding(new Insets(24));
        card.getStyleClass().add("card-panel");

        Label cardTitle = new Label("Institutional Profile & System Status");
        cardTitle.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);

        addProfileRow(grid, 0, "Institution / প্রতিষ্ঠান:", "Bangladesh University of Business and Technology (BUBT) Central Library / বিইউবিটি কেন্দ্রীয় গ্রন্থাগার");
        addProfileRow(grid, 1, "Software Platform:", "LibraryOS — BUBT CSE Edition (Developed by Kawsar Hoque, Dept. of CSE, BUBT)");
        addProfileRow(grid, 2, "Runtime Environment:", "Java 25 LTS • JavaFX 21.0.2 • OpenPDF • ZXing • Apache POI");
        addProfileRow(grid, 3, "Database Persistence:", "MySQL 8.x (InnoDB, utf8mb4_unicode_ci) • HikariCP Connection Pool");
        addProfileRow(grid, 4, "Weekly Holiday Calendar:", "Friday (Configured Weekly Holiday — Overdue fine exclusion active)");
        addProfileRow(grid, 5, "Official Currency Standard:", "Bangladeshi Taka (৳ BDT) • Strict BigDecimal Precision");

        card.getChildren().addAll(cardTitle, grid);
        layout.getChildren().add(card);
        return layout;
    }

    private void addProfileRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        Label v = new Label(value);
        v.getStyleClass().add("profile-value-label");
        v.setStyle("-fx-font-weight: 600;");
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    public Button getRefreshBtn() { return refreshBtn; }
    public TabPane getTabPane() { return tabPane; }

    public Tab getAuditTab() { return auditTab; }
    public TextField getAuditSearchField() { return auditSearchField; }
    public ComboBox<String> getAuditActionFilter() { return auditActionFilter; }
    public ComboBox<String> getAuditEntityFilter() { return auditEntityFilter; }
    public Button getAuditFilterBtn() { return auditFilterBtn; }
    public Button getAuditResetBtn() { return auditResetBtn; }
    public TableView<AuditLog> getAuditTable() { return auditTable; }
    public Label getAuditCountLabel() { return auditCountLabel; }
    public Label getAuditDetailText() { return auditDetailText; }

    public Tab getBackupTab() { return backupTab; }
    public Button getCreateBackupBtn() { return createBackupBtn; }
    public Button getRestoreBackupBtn() { return restoreBackupBtn; }
    public Button getRefreshBackupsBtn() { return refreshBackupsBtn; }
    public TableView<BackupService.BackupFileItem> getBackupsTable() { return backupsTable; }
    public Label getBackupsCountLabel() { return backupsCountLabel; }
}
