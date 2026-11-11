package com.libraryos.view;

import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.finance.Fine;
import com.libraryos.model.finance.FineStatus;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentMethod;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;

public class FinanceDeskView extends BorderPane {

    private final Button refreshBtn = new Button("↻ Refresh");
    private final TabPane tabPane = new TabPane();

    // --- Tab 1: Member Collection ---
    private final Tab memberTab = new Tab("Member Fines & Collection (ফি আদায়)");
    private final TextField memberSearchField = new TextField();
    private final Button memberLookupBtn = new Button("Find Member");

    private final Label memberNameLabel = new Label("No member loaded");
    private final Label memberCardLabel = new Label("—");
    private final Label memberContactLabel = new Label("—");
    private final Label memberStatusBadge = new Label("IDLE");

    private final Label totalUnpaidBalanceLabel = new Label("৳ 0.00");
    private final Label unpaidCountLabel = new Label("0 Unpaid fines");

    private final Button collectPaymentBtn = new Button("✓ Collect Payment (টাকা জমা নিন)");
    private final Button waiveFineBtn = new Button("⚖ Waive Fine (মওকুফ)");
    private final Button assessFineBtn = new Button("+ Manual Fine (জরিমানা আরোপ)");

    private final TableView<Fine> memberFinesTable = new TableView<>();
    private final Label memberFinesCountLabel = new Label("No fines for member");

    // --- Tab 2: Payments History ---
    private final Tab paymentsTab = new Tab("Payments & Receipts History (রসিদ খতিয়ান)");
    private final TextField paymentsSearchField = new TextField();
    private final Button paymentsSearchBtn = new Button("Search");
    private final Button paymentsResetBtn = new Button("Reset");
    private final TableView<Payment> paymentsTable = new TableView<>();
    private final Label paymentsCountLabel = new Label("Showing 0 payments");

    // --- Tab 3: All Fines Ledger ---
    private final Tab allFinesTab = new Tab("All Fines Ledger (সার্বিক খতিয়ান)");
    private final TextField allFinesSearchField = new TextField();
    private final ComboBox<String> allFinesStatusFilter = new ComboBox<>();
    private final Button allFinesSearchBtn = new Button("Filter");
    private final Button allFinesResetBtn = new Button("Reset");
    private final TableView<Fine> allFinesTable = new TableView<>();
    private final Label allFinesCountLabel = new Label("Showing 0 fines");

    public FinanceDeskView() {
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
        Label title = new Label("Finance & Payments / অর্থ ও জরিমানা ব্যবস্থাপনা");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Enforce overdue policies, collect BDT fine payments via Cash & MFS, record audited waivers, and review receipts");
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

        memberTab.setContent(buildMemberTabContent());
        paymentsTab.setContent(buildPaymentsTabContent());
        allFinesTab.setContent(buildAllFinesTabContent());

        tabPane.getTabs().addAll(memberTab, paymentsTab, allFinesTab);
        return tabPane;
    }

    // --- TAB 1: MEMBER FINES & COLLECTION ---
    private Node buildMemberTabContent() {
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(16, 0, 0, 0));

        // Member Search Bar
        HBox searchBar = new HBox(12);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(12, 16, 12, 16));
        searchBar.getStyleClass().add("filter-container");

        memberSearchField.setPromptText("Enter Membership Number or Member Name...");
        memberSearchField.getStyleClass().add("form-input");
        memberSearchField.setPrefWidth(340);
        memberLookupBtn.getStyleClass().add("btn-primary");

        searchBar.getChildren().addAll(new Label("Select Member:"), memberSearchField, memberLookupBtn);

        // Member Overview Cards
        HBox overviewCards = new HBox(16);

        // Left Card: Member Info
        VBox memberCard = new VBox(6);
        memberCard.setPadding(new Insets(14));
        memberCard.getStyleClass().add("card-panel");
        memberCard.setPrefWidth(300);

        Label memberCardHeader = new Label("MEMBER PROFILE");
        memberCardHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        memberNameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        memberStatusBadge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");

        HBox cardNumRow = new HBox(6, new Label("Card #:"), memberCardLabel);
        HBox contactRow = new HBox(6, new Label("Contact:"), memberContactLabel);
        memberCard.getChildren().addAll(memberCardHeader, memberNameLabel, memberStatusBadge, cardNumRow, contactRow);

        // Center Card: Outstanding Balance KPI
        VBox balanceCard = new VBox(6);
        balanceCard.setAlignment(Pos.CENTER_LEFT);
        balanceCard.setPadding(new Insets(14));
        balanceCard.setStyle("-fx-background-color: #FEF2F2; -fx-background-radius: 8px; -fx-border-color: #FECACA; -fx-border-radius: 8px;");
        balanceCard.setPrefWidth(260);

        Label balanceHeader = new Label("OUTSTANDING FINE BALANCE");
        balanceHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #991B1B;");
        totalUnpaidBalanceLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #DC2626;");
        unpaidCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7F1D1D;");
        balanceCard.getChildren().addAll(balanceHeader, totalUnpaidBalanceLabel, unpaidCountLabel);

        // Right Card: Actions
        VBox actionsCard = new VBox(8);
        actionsCard.setAlignment(Pos.CENTER);
        actionsCard.setPadding(new Insets(14));
        actionsCard.getStyleClass().add("card-panel");
        HBox.setHgrow(actionsCard, Priority.ALWAYS);

        collectPaymentBtn.getStyleClass().add("btn-primary");
        collectPaymentBtn.setStyle("-fx-background-color: #16A34A; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 16px;");
        collectPaymentBtn.setMaxWidth(Double.MAX_VALUE);
        collectPaymentBtn.setDisable(true);

        waiveFineBtn.getStyleClass().add("btn-secondary");
        waiveFineBtn.setMaxWidth(Double.MAX_VALUE);
        waiveFineBtn.setDisable(true);

        assessFineBtn.getStyleClass().add("btn-secondary");
        assessFineBtn.setMaxWidth(Double.MAX_VALUE);

        actionsCard.getChildren().addAll(collectPaymentBtn, waiveFineBtn, assessFineBtn);

        overviewCards.getChildren().addAll(memberCard, balanceCard, actionsCard);

        // Unpaid Fines Table
        VBox tableBox = new VBox(8);

        TableColumn<Fine, String> colId = new TableColumn<>("#");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(50);

        TableColumn<Fine, String> colType = new TableColumn<>("Fine Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFineType().name()));
        colType.setPrefWidth(120);

        TableColumn<Fine, String> colBook = new TableColumn<>("Book / Loan Reference");
        colBook.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBookTitle() != null ? d.getValue().getBookTitle() : (d.getValue().getDescription() != null ? d.getValue().getDescription() : "—")
        ));
        colBook.setPrefWidth(220);

        TableColumn<Fine, String> colAmount = new TableColumn<>("Amount (৳)");
        colAmount.setCellValueFactory(d -> new SimpleStringProperty("৳ " + d.getValue().getAmount().toPlainString()));
        colAmount.setPrefWidth(110);

        TableColumn<Fine, String> colPaid = new TableColumn<>("Paid (৳)");
        colPaid.setCellValueFactory(d -> new SimpleStringProperty("৳ " + d.getValue().getPaidAmount().toPlainString()));
        colPaid.setPrefWidth(110);

        TableColumn<Fine, String> colBalance = new TableColumn<>("Unpaid Balance (৳)");
        colBalance.setCellValueFactory(d -> new SimpleStringProperty("৳ " + d.getValue().getUnpaidBalance().toPlainString()));
        colBalance.setStyle("-fx-font-weight: bold; -fx-text-fill: #DC2626;");
        colBalance.setPrefWidth(130);

        TableColumn<Fine, FineStatus> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(FineStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status.name());
                    switch (status) {
                        case UNPAID -> badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        case PARTIALLY_PAID -> badge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        case PAID -> badge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        case WAIVED -> badge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(120);

        TableColumn<Fine, String> colDate = new TableColumn<>("Assessed At");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? DateTimeUtil.formatMediumDateTime(d.getValue().getCreatedAt()) : "—"
        ));
        colDate.setPrefWidth(140);

        TableColumn<Fine, String> colDesc = new TableColumn<>("Description / Notes");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription() != null ? d.getValue().getDescription() : ""));
        colDesc.setPrefWidth(160);

        memberFinesTable.getColumns().addAll(colId, colType, colBook, colAmount, colPaid, colBalance, colStatus, colDate, colDesc);
        memberFinesTable.setPlaceholder(new Label("Select a member or search by membership number to view unpaid fines."));
        memberFinesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(memberFinesTable, Priority.ALWAYS);

        HBox footer = new HBox(memberFinesCountLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        memberFinesCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        tableBox.getChildren().addAll(memberFinesTable, footer);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        layout.getChildren().addAll(searchBar, overviewCards, tableBox);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    // --- TAB 2: PAYMENTS & RECEIPTS HISTORY ---
    private Node buildPaymentsTabContent() {
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(16, 0, 0, 0));

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(12, 16, 12, 16));
        filterBar.getStyleClass().add("filter-container");

        paymentsSearchField.setPromptText("Search by Receipt # (REC-...), Member name, card, or TrxID...");
        paymentsSearchField.getStyleClass().add("form-input");
        paymentsSearchField.setPrefWidth(380);

        paymentsSearchBtn.getStyleClass().add("btn-primary");
        paymentsResetBtn.getStyleClass().add("btn-secondary");

        filterBar.getChildren().addAll(new Label("Search:"), paymentsSearchField, paymentsSearchBtn, paymentsResetBtn);

        TableColumn<Payment, String> colReceipt = new TableColumn<>("Receipt #");
        colReceipt.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReceiptNumber()));
        colReceipt.setStyle("-fx-font-weight: bold;");
        colReceipt.setPrefWidth(160);

        TableColumn<Payment, String> colMember = new TableColumn<>("Member / সদস্য");
        colMember.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().getMemberName() != null ? d.getValue().getMemberName() : "—")
                + " (" + (d.getValue().getMembershipNumber() != null ? d.getValue().getMembershipNumber() : "—") + ")"
        ));
        colMember.setPrefWidth(200);

        TableColumn<Payment, String> colAmount = new TableColumn<>("Amount (৳)");
        colAmount.setCellValueFactory(d -> new SimpleStringProperty("৳ " + d.getValue().getAmount().toPlainString()));
        colAmount.setStyle("-fx-font-weight: bold; -fx-text-fill: #16A34A;");
        colAmount.setPrefWidth(120);

        TableColumn<Payment, PaymentMethod> colMethod = new TableColumn<>("Method");
        colMethod.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getPaymentMethod()));
        colMethod.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(PaymentMethod method, boolean empty) {
                super.updateItem(method, empty);
                if (empty || method == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(method.name());
                    badge.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-padding: 2px 6px; -fx-background-radius: 4px; -fx-font-size: 11px;");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colMethod.setPrefWidth(110);

        TableColumn<Payment, String> colTrx = new TableColumn<>("Transaction Ref");
        colTrx.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTransactionReference() != null ? d.getValue().getTransactionReference() : "—"
        ));
        colTrx.setPrefWidth(140);

        TableColumn<Payment, String> colStaff = new TableColumn<>("Recorded By");
        colStaff.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getReceivedByUsername() != null ? d.getValue().getReceivedByUsername() : "Staff #" + d.getValue().getReceivedByUserId()
        ));
        colStaff.setPrefWidth(120);

        TableColumn<Payment, String> colDate = new TableColumn<>("Paid At");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? DateTimeUtil.formatMediumDateTime(d.getValue().getCreatedAt()) : "—"
        ));
        colDate.setPrefWidth(150);

        TableColumn<Payment, Boolean> colStatus = new TableColumn<>("Ledger Status");
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleBooleanProperty(d.getValue().isReversed()).asObject());
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean reversed, boolean empty) {
                super.updateItem(reversed, empty);
                if (empty || reversed == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(reversed ? "REVERSED" : "COMPLETED");
                    badge.setStyle(reversed
                            ? "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;"
                            : "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(120);

        TableColumn<Payment, String> colNotes = new TableColumn<>("Notes / Reversal Reason");
        colNotes.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isReversed() ? "REVERSED: " + d.getValue().getReversalReason() : (d.getValue().getNotes() != null ? d.getValue().getNotes() : "")
        ));
        colNotes.setPrefWidth(180);

        paymentsTable.getColumns().addAll(colReceipt, colMember, colAmount, colMethod, colTrx, colStaff, colDate, colStatus, colNotes);
        paymentsTable.setPlaceholder(new Label("No payments found."));
        paymentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(paymentsTable, Priority.ALWAYS);

        HBox footer = new HBox(paymentsCountLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        paymentsCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        layout.getChildren().addAll(filterBar, paymentsTable, footer);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    // --- TAB 3: ALL FINES LEDGER ---
    private Node buildAllFinesTabContent() {
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(16, 0, 0, 0));

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(12, 16, 12, 16));
        filterBar.getStyleClass().add("filter-container");

        allFinesSearchField.setPromptText("Search by member name, card #, or description...");
        allFinesSearchField.getStyleClass().add("form-input");
        allFinesSearchField.setPrefWidth(340);

        allFinesStatusFilter.getItems().addAll("ALL", "UNPAID", "PARTIALLY_PAID", "PAID", "WAIVED");
        allFinesStatusFilter.setValue("ALL");
        allFinesStatusFilter.getStyleClass().add("form-input");
        allFinesStatusFilter.setPrefWidth(160);

        allFinesSearchBtn.getStyleClass().add("btn-primary");
        allFinesResetBtn.getStyleClass().add("btn-secondary");

        filterBar.getChildren().addAll(
                new Label("Search:"), allFinesSearchField,
                new Label("Status:"), allFinesStatusFilter,
                allFinesSearchBtn, allFinesResetBtn
        );

        TableColumn<Fine, String> colId = new TableColumn<>("#");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(50);

        TableColumn<Fine, String> colMember = new TableColumn<>("Member / সদস্য");
        colMember.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().getMemberName() != null ? d.getValue().getMemberName() : "—")
                + " (" + (d.getValue().getMembershipNumber() != null ? d.getValue().getMembershipNumber() : "—") + ")"
        ));
        colMember.setPrefWidth(200);

        TableColumn<Fine, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFineType().name()));
        colType.setPrefWidth(110);

        TableColumn<Fine, String> colBook = new TableColumn<>("Book / Description");
        colBook.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBookTitle() != null ? d.getValue().getBookTitle() : (d.getValue().getDescription() != null ? d.getValue().getDescription() : "—")
        ));
        colBook.setPrefWidth(220);

        TableColumn<Fine, String> colAmount = new TableColumn<>("Total (৳)");
        colAmount.setCellValueFactory(d -> new SimpleStringProperty("৳ " + d.getValue().getAmount().toPlainString()));
        colAmount.setPrefWidth(100);

        TableColumn<Fine, String> colPaid = new TableColumn<>("Paid (৳)");
        colPaid.setCellValueFactory(d -> new SimpleStringProperty("৳ " + d.getValue().getPaidAmount().toPlainString()));
        colPaid.setPrefWidth(100);

        TableColumn<Fine, String> colBalance = new TableColumn<>("Unpaid (৳)");
        colBalance.setCellValueFactory(d -> new SimpleStringProperty("৳ " + d.getValue().getUnpaidBalance().toPlainString()));
        colBalance.setStyle("-fx-font-weight: bold; -fx-text-fill: #DC2626;");
        colBalance.setPrefWidth(110);

        TableColumn<Fine, FineStatus> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(FineStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status.name());
                    switch (status) {
                        case UNPAID -> badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        case PARTIALLY_PAID -> badge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        case PAID -> badge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        case WAIVED -> badge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(120);

        TableColumn<Fine, String> colDate = new TableColumn<>("Assessed At");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? DateTimeUtil.formatMediumDateTime(d.getValue().getCreatedAt()) : "—"
        ));
        colDate.setPrefWidth(150);

        allFinesTable.getColumns().addAll(colId, colMember, colType, colBook, colAmount, colPaid, colBalance, colStatus, colDate);
        allFinesTable.setPlaceholder(new Label("No fines found in system."));
        allFinesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(allFinesTable, Priority.ALWAYS);

        HBox footer = new HBox(allFinesCountLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        allFinesCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        layout.getChildren().addAll(filterBar, allFinesTable, footer);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    public Button getRefreshBtn() { return refreshBtn; }
    public TabPane getTabPane() { return tabPane; }
    public Tab getMemberTab() { return memberTab; }
    public Tab getPaymentsTab() { return paymentsTab; }
    public Tab getAllFinesTab() { return allFinesTab; }

    public TextField getMemberSearchField() { return memberSearchField; }
    public Button getMemberLookupBtn() { return memberLookupBtn; }
    public Label getMemberNameLabel() { return memberNameLabel; }
    public Label getMemberCardLabel() { return memberCardLabel; }
    public Label getMemberContactLabel() { return memberContactLabel; }
    public Label getMemberStatusBadge() { return memberStatusBadge; }
    public Label getTotalUnpaidBalanceLabel() { return totalUnpaidBalanceLabel; }
    public Label getUnpaidCountLabel() { return unpaidCountLabel; }
    public Button getCollectPaymentBtn() { return collectPaymentBtn; }
    public Button getWaiveFineBtn() { return waiveFineBtn; }
    public Button getAssessFineBtn() { return assessFineBtn; }
    public TableView<Fine> getMemberFinesTable() { return memberFinesTable; }
    public Label getMemberFinesCountLabel() { return memberFinesCountLabel; }

    public TextField getPaymentsSearchField() { return paymentsSearchField; }
    public Button getPaymentsSearchBtn() { return paymentsSearchBtn; }
    public Button getPaymentsResetBtn() { return paymentsResetBtn; }
    public TableView<Payment> getPaymentsTable() { return paymentsTable; }
    public Label getPaymentsCountLabel() { return paymentsCountLabel; }

    public TextField getAllFinesSearchField() { return allFinesSearchField; }
    public ComboBox<String> getAllFinesStatusFilter() { return allFinesStatusFilter; }
    public Button getAllFinesSearchBtn() { return allFinesSearchBtn; }
    public Button getAllFinesResetBtn() { return allFinesResetBtn; }
    public TableView<Fine> getAllFinesTable() { return allFinesTable; }
    public Label getAllFinesCountLabel() { return allFinesCountLabel; }
}
