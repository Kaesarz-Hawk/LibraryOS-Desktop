package com.libraryos.view;

import com.libraryos.core.util.CurrencyFormatUtil;
import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.dto.DashboardMetricsDTO;
import com.libraryos.model.audit.AuditLog;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Main Operational Dashboard View displaying live statistics, quick actions, and recent activity log.
 */
public class DashboardView extends ScrollPane {

    private final VBox container;
    private final Label welcomeLabel;
    private final Label subtitleLabel;
    private final Button refreshButton;

    // Warning Banner
    private final HBox dbWarningBanner;
    private final Label dbWarningText;

    // Stat metric card values
    private final Label booksCountLabel;
    private final Label copiesCountLabel;
    private final Label loansCountLabel;
    private final Label overdueBadgeLabel;
    private final Label membersCountLabel;
    private final Label finesBadgeLabel;

    // Quick action buttons
    private final Button issueBookBtn;
    private final Button returnBookBtn;
    private final Button addMemberBtn;
    private final Button addBookBtn;

    // Activity Table
    private final TableView<AuditLog> activityTable;

    public DashboardView() {
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        getStyleClass().add("dashboard-scroll-pane");

        container = new VBox(24);
        container.getStyleClass().add("content-area");
        container.setPadding(new Insets(24, 28, 28, 28));

        // 1. Page Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        welcomeLabel = new Label("Operational Dashboard / কার্যক্রম ড্যাশবোর্ড");
        welcomeLabel.getStyleClass().add("page-title");

        subtitleLabel = new Label("Real-time circulation metrics and library system activity");
        subtitleLabel.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(welcomeLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        refreshButton = new Button("↻ Refresh Data");
        refreshButton.getStyleClass().add("btn-secondary");

        header.getChildren().addAll(titleBox, spacer, refreshButton);

        // 2. DB Notice Banner (shown if MySQL is offline)
        dbWarningBanner = new HBox(12);
        dbWarningBanner.getStyleClass().add("banner-warning");
        dbWarningBanner.setAlignment(Pos.CENTER_LEFT);
        dbWarningText = new Label();
        dbWarningText.setWrapText(true);
        dbWarningBanner.getChildren().add(dbWarningText);
        dbWarningBanner.setVisible(false);
        dbWarningBanner.setManaged(false);

        // 3. Four Metric Cards Grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20);
        metricsGrid.setVgap(20);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            metricsGrid.getColumnConstraints().add(col);
        }

        // Card 1: Books
        VBox card1 = createMetricCard("TOTAL BOOK TITLES", "বইয়ের শিরোনাম");
        booksCountLabel = new Label("0");
        booksCountLabel.getStyleClass().add("metric-value");
        Label catHint = new Label("Bibliographic Catalog");
        catHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        card1.getChildren().addAll(booksCountLabel, catHint);

        // Card 2: Copies
        VBox card2 = createMetricCard("PHYSICAL COPIES", "মোট বইয়ের কপি");
        copiesCountLabel = new Label("0");
        copiesCountLabel.getStyleClass().add("metric-value");
        Label copyHint = new Label("Unique Barcodes / Accession");
        copyHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        card2.getChildren().addAll(copiesCountLabel, copyHint);

        // Card 3: Active Loans
        VBox card3 = createMetricCard("ACTIVE LOANS", "চলতি ধার");
        loansCountLabel = new Label("0");
        loansCountLabel.getStyleClass().add("metric-value");
        overdueBadgeLabel = new Label("0 Overdue");
        overdueBadgeLabel.getStyleClass().add("badge-available");
        card3.getChildren().addAll(loansCountLabel, overdueBadgeLabel);

        // Card 4: Members & Fines
        VBox card4 = createMetricCard("REGISTERED MEMBERS", "সক্রিয় সদস্য");
        membersCountLabel = new Label("0");
        membersCountLabel.getStyleClass().add("metric-value");
        finesBadgeLabel = new Label("৳ 0.00 Pending Fines");
        finesBadgeLabel.getStyleClass().add("badge-issued");
        card4.getChildren().addAll(membersCountLabel, finesBadgeLabel);

        metricsGrid.add(card1, 0, 0);
        metricsGrid.add(card2, 1, 0);
        metricsGrid.add(card3, 2, 0);
        metricsGrid.add(card4, 3, 0);

        // 4. Quick Actions Panel
        VBox actionsCard = new VBox(14);
        actionsCard.getStyleClass().add("card-panel");

        Label actionsTitle = new Label("Quick Operational Workflows");
        actionsTitle.getStyleClass().add("card-title");

        HBox actionsRow = new HBox(12);
        issueBookBtn = new Button("↗ Issue Book / বই প্রদান");
        issueBookBtn.getStyleClass().add("btn-primary");

        returnBookBtn = new Button("↙ Return Book / বই গ্রহণ");
        returnBookBtn.getStyleClass().add("btn-secondary");

        addMemberBtn = new Button("+ Register Member / সদস্য যোগ");
        addMemberBtn.getStyleClass().add("btn-secondary");

        addBookBtn = new Button("+ Add Book Title / নতুন বই");
        addBookBtn.getStyleClass().add("btn-secondary");

        actionsRow.getChildren().addAll(issueBookBtn, returnBookBtn, addMemberBtn, addBookBtn);
        actionsCard.getChildren().addAll(actionsTitle, actionsRow);

        // 5. Recent System Activity Table
        VBox tableCard = new VBox(14);
        tableCard.getStyleClass().add("card-panel");

        Label tableTitle = new Label("Recent System Activity & Audit Trail");
        tableTitle.getStyleClass().add("card-title");

        activityTable = new TableView<>();
        activityTable.setPrefHeight(260);
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AuditLog, String> timeCol = new TableColumn<>("Time (UTC/Local)");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(DateTimeUtil.formatDateTime(data.getValue().getCreatedAt())));
        timeCol.setMinWidth(160);

        TableColumn<AuditLog, String> actorCol = new TableColumn<>("Actor");
        actorCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getActorUsername() != null ? data.getValue().getActorUsername() : "SYSTEM"));
        actorCol.setMinWidth(110);

        TableColumn<AuditLog, String> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAction()));
        actionCol.setMinWidth(170);
        actionCol.setCellFactory(col -> new TableCell<AuditLog, String>() {
            private final Label badge = new Label();
            {
                badge.setPadding(new Insets(3, 8, 3, 8));
                badge.setStyle("-fx-background-radius: 10px; -fx-font-size: 11px; -fx-font-weight: bold;");
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String action, boolean empty) {
                super.updateItem(action, empty);
                if (empty || action == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setText(action);
                    badge.getStyleClass().clear();
                    String actUpper = action.toUpperCase();
                    if (actUpper.contains("SUCCESS") || actUpper.contains("CREATED") || actUpper.contains("RETURNED") || actUpper.contains("INITIALIZED")) {
                        badge.getStyleClass().add("badge-action-success");
                    } else if (actUpper.contains("WARN") || actUpper.contains("RENEW") || actUpper.contains("HOLD") || actUpper.contains("RESERVATION")) {
                        badge.getStyleClass().add("badge-action-warning");
                    } else if (actUpper.contains("FAILED") || actUpper.contains("LOST") || actUpper.contains("OVERDUE") || actUpper.contains("FINE") || actUpper.contains("DAMAGED")) {
                        badge.getStyleClass().add("badge-action-danger");
                    } else {
                        badge.getStyleClass().add("badge-action-info");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<AuditLog, String> entityCol = new TableColumn<>("Entity");
        entityCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEntityType() + " #" + (data.getValue().getEntityId() != null ? data.getValue().getEntityId() : "")));
        entityCol.setMinWidth(120);

        TableColumn<AuditLog, String> summaryCol = new TableColumn<>("Summary / Reason");
        summaryCol.setCellValueFactory(data -> new SimpleStringProperty(
                (data.getValue().getSummary() != null ? data.getValue().getSummary() : "") +
                (data.getValue().getReason() != null ? " (" + data.getValue().getReason() + ")" : "")));
        summaryCol.setMinWidth(300);

        activityTable.getColumns().addAll(timeCol, actorCol, actionCol, entityCol, summaryCol);
        activityTable.setPlaceholder(new Label("No recent system activity recorded."));

        tableCard.getChildren().addAll(tableTitle, activityTable);

        // Combine into container
        container.getChildren().addAll(header, dbWarningBanner, metricsGrid, actionsCard, tableCard);
        setContent(container);
    }

    private VBox createMetricCard(String titleEn, String titleBn) {
        VBox card = new VBox(8);
        card.getStyleClass().add("metric-card");
        Label titleLabel = new Label(titleEn + " • " + titleBn);
        titleLabel.getStyleClass().add("metric-title");
        card.getChildren().add(titleLabel);
        return card;
    }

    public void updateMetrics(DashboardMetricsDTO metrics) {
        booksCountLabel.setText(String.format("%,d", metrics.getTotalBooks()));
        copiesCountLabel.setText(String.format("%,d", metrics.getTotalCopies()));
        loansCountLabel.setText(String.format("%,d", metrics.getActiveLoans()));

        if (metrics.getOverdueLoans() > 0) {
            overdueBadgeLabel.setText(metrics.getOverdueLoans() + " Overdue");
            overdueBadgeLabel.getStyleClass().setAll("badge-overdue");
        } else {
            overdueBadgeLabel.setText("All On Schedule");
            overdueBadgeLabel.getStyleClass().setAll("badge-available");
        }

        membersCountLabel.setText(String.format("%,d", metrics.getTotalMembers()));
        finesBadgeLabel.setText(CurrencyFormatUtil.format(metrics.getPendingFinesBdt()) + " Pending");
        finesBadgeLabel.getStyleClass().setAll("badge-issued");
    }

    public void showDbWarning(String warning) {
        dbWarningText.setText(warning);
        dbWarningBanner.setVisible(true);
        dbWarningBanner.setManaged(true);
    }

    public void hideDbWarning() {
        dbWarningBanner.setVisible(false);
        dbWarningBanner.setManaged(false);
    }

    public TableView<AuditLog> getActivityTable() {
        return activityTable;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }

    public Button getIssueBookBtn() {
        return issueBookBtn;
    }

    public Button getReturnBookBtn() {
        return returnBookBtn;
    }

    public Button getAddMemberBtn() {
        return addMemberBtn;
    }

    public Button getAddBookBtn() {
        return addBookBtn;
    }

    public Label getWelcomeLabel() {
        return welcomeLabel;
    }

    public Label getSubtitleLabel() {
        return subtitleLabel;
    }
}
