package com.libraryos.view;

import com.libraryos.model.circulation.Loan;
import com.libraryos.model.circulation.LoanStatus;
import com.libraryos.model.inventory.ConditionStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class CirculationDeskView extends BorderPane {

    // Header buttons
    private final Button policyButton = new Button("Borrowing Policies / নীতিমালা");
    private final Button refreshButton = new Button("↻ Refresh");

    private final TabPane tabPane = new TabPane();

    // --- Tab 1: Issue Tab Controls ---
    private final Tab issueTab = new Tab("Issue / Checkout (ইস্যু)");
    private final TextField issueBarcodeField = new TextField();
    private final Button issueLookupCopyBtn = new Button("Lookup Copy");
    private final Label issueCopyTitleLabel = new Label("No copy scanned");
    private final Label issueCopyBarcodeLabel = new Label("—");
    private final Label issueCopyAccessionLabel = new Label("—");
    private final Label issueCopyShelfLabel = new Label("—");
    private final Label issueCopyConditionLabel = new Label("—");
    private final Label issueCopyStatusBadge = new Label("IDLE");

    private final TextField issueMemberField = new TextField();
    private final Button issueLookupMemberBtn = new Button("Find Member");
    private final Label issueMemberNameLabel = new Label("No member selected");
    private final Label issueMemberCardLabel = new Label("—");
    private final Label issueMemberTypeLabel = new Label("—");
    private final Label issueMemberLoansLabel = new Label("0 / 3");
    private final Label issueMemberFinesLabel = new Label("৳ 0.00");
    private final Label issueMemberStatusBadge = new Label("IDLE");

    private final Label issueDueDatePreviewLabel = new Label("Calculated upon copy & member lookup");
    private final Button issueConfirmButton = new Button("✓ Issue Book Copy");
    private final Label issueFeedbackLabel = new Label();

    // --- Tab 2: Return Tab Controls ---
    private final Tab returnTab = new Tab("Return & Overdue (ফেরত)");
    private final TextField returnBarcodeField = new TextField();
    private final Button returnLookupBtn = new Button("Lookup Loan");

    private final Label returnBookTitleLabel = new Label("No loan loaded");
    private final Label returnBarcodeLabel = new Label("—");
    private final Label returnMemberLabel = new Label("—");
    private final Label returnIssuedDateLabel = new Label("—");
    private final Label returnDueDateLabel = new Label("—");
    private final ComboBox<ConditionStatus> returnConditionCombo = new ComboBox<>();
    private final TextArea returnNotesArea = new TextArea();

    private final Label returnOverdueDaysLabel = new Label("0 Days");
    private final Label returnDailyRateLabel = new Label("৳ 0.00 / day");
    private final Label returnCalculatedFineLabel = new Label("৳ 0.00");
    private final Label returnFineStatusBadge = new Label("ON TIME");

    private final Button returnConfirmButton = new Button("✓ Process Return");
    private final Label returnFeedbackLabel = new Label();

    // --- Tab 3: Active Loans Table Tab ---
    private final Tab activeLoansTab = new Tab("Active & Historical Loans (তালিকা)");
    private final TextField loansSearchField = new TextField();
    private final ComboBox<String> loansStatusFilter = new ComboBox<>();
    private final CheckBox loansOverdueOnlyCheck = new CheckBox("Overdue Loans Only");
    private final Button loansSearchBtn = new Button("Filter");
    private final Button loansResetBtn = new Button("Reset");
    private final TableView<Loan> loansTable = new TableView<>();
    private final Label loansCountLabel = new Label("Showing 0 loans");

    public CirculationDeskView() {
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
        Label title = new Label("Circulation Desk / ইস্যু ও ফেরত কেন্দ্র");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Manage book loans, track overdue items, and enforce institutional borrowing policies");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        policyButton.getStyleClass().add("btn-secondary");
        refreshButton.getStyleClass().add("btn-secondary");
        headerRow.getChildren().addAll(titleBox, spacer, policyButton, refreshButton);

        topBox.getChildren().add(headerRow);
        return topBox;
    }

    private TabPane buildCenterTabs() {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getStyleClass().add("circulation-tab-pane");

        issueTab.setContent(buildIssueContent());
        returnTab.setContent(buildReturnContent());
        activeLoansTab.setContent(buildActiveLoansContent());

        tabPane.getTabs().addAll(issueTab, returnTab, activeLoansTab);
        return tabPane;
    }

    private ScrollPane buildIssueContent() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        issueFeedbackLabel.getStyleClass().add("banner-info");
        issueFeedbackLabel.setVisible(false);
        issueFeedbackLabel.setManaged(false);

        HBox twoColumns = new HBox(20);
        twoColumns.setAlignment(Pos.TOP_LEFT);

        // Column 1: Copy Card
        VBox copyCard = new VBox(12);
        copyCard.getStyleClass().add("metric-card");
        copyCard.setPrefWidth(420);
        HBox.setHgrow(copyCard, Priority.ALWAYS);

        Label copyStepTitle = new Label("Step 1: Scan Book Copy / কপি স্ক্যান");
        copyStepTitle.getStyleClass().add("card-title");

        HBox copyScanRow = new HBox(8);
        issueBarcodeField.setPromptText("Scan or enter barcode (e.g. BC100001)");
        issueBarcodeField.getStyleClass().add("search-field");
        HBox.setHgrow(issueBarcodeField, Priority.ALWAYS);
        issueLookupCopyBtn.getStyleClass().add("btn-primary");
        copyScanRow.getChildren().addAll(issueBarcodeField, issueLookupCopyBtn);

        GridPane copyDetails = new GridPane();
        copyDetails.setHgap(12);
        copyDetails.setVgap(8);

        copyDetails.add(new Label("Title / বই:"), 0, 0);
        issueCopyTitleLabel.setStyle("-fx-font-weight: bold;");
        copyDetails.add(issueCopyTitleLabel, 1, 0);

        copyDetails.add(new Label("Barcode:"), 0, 1);
        copyDetails.add(issueCopyBarcodeLabel, 1, 1);

        copyDetails.add(new Label("Accession:"), 0, 2);
        copyDetails.add(issueCopyAccessionLabel, 1, 2);

        copyDetails.add(new Label("Shelf:"), 0, 3);
        copyDetails.add(issueCopyShelfLabel, 1, 3);

        copyDetails.add(new Label("Condition:"), 0, 4);
        copyDetails.add(issueCopyConditionLabel, 1, 4);

        copyDetails.add(new Label("Status:"), 0, 5);
        issueCopyStatusBadge.getStyleClass().addAll("badge", "badge-info");
        copyDetails.add(issueCopyStatusBadge, 1, 5);

        copyCard.getChildren().addAll(copyStepTitle, copyScanRow, copyDetails);

        // Column 2: Member Card
        VBox memberCard = new VBox(12);
        memberCard.getStyleClass().add("metric-card");
        memberCard.setPrefWidth(420);
        HBox.setHgrow(memberCard, Priority.ALWAYS);

        Label memberStepTitle = new Label("Step 2: Lookup Member / সদস্য খুঁজুন");
        memberStepTitle.getStyleClass().add("card-title");

        HBox memberScanRow = new HBox(8);
        issueMemberField.setPromptText("Enter Card #, Phone or Roll (e.g. LIB-2026-0001)");
        issueMemberField.getStyleClass().add("search-field");
        HBox.setHgrow(issueMemberField, Priority.ALWAYS);
        issueLookupMemberBtn.getStyleClass().add("btn-primary");
        memberScanRow.getChildren().addAll(issueMemberField, issueLookupMemberBtn);

        GridPane memberDetails = new GridPane();
        memberDetails.setHgap(12);
        memberDetails.setVgap(8);

        memberDetails.add(new Label("Name / নাম:"), 0, 0);
        issueMemberNameLabel.setStyle("-fx-font-weight: bold;");
        memberDetails.add(issueMemberNameLabel, 1, 0);

        memberDetails.add(new Label("Card Number:"), 0, 1);
        memberDetails.add(issueMemberCardLabel, 1, 1);

        memberDetails.add(new Label("Member Type:"), 0, 2);
        memberDetails.add(issueMemberTypeLabel, 1, 2);

        memberDetails.add(new Label("Active Loans:"), 0, 3);
        memberDetails.add(issueMemberLoansLabel, 1, 3);

        memberDetails.add(new Label("Unpaid Fines:"), 0, 4);
        memberDetails.add(issueMemberFinesLabel, 1, 4);

        memberDetails.add(new Label("Status:"), 0, 5);
        issueMemberStatusBadge.getStyleClass().addAll("badge", "badge-info");
        memberDetails.add(issueMemberStatusBadge, 1, 5);

        memberCard.getChildren().addAll(memberStepTitle, memberScanRow, memberDetails);

        twoColumns.getChildren().addAll(copyCard, memberCard);

        // Bottom Action Area
        VBox actionBox = new VBox(12);
        actionBox.getStyleClass().add("metric-card");
        actionBox.setAlignment(Pos.CENTER_LEFT);

        HBox dueRow = new HBox(12);
        dueRow.setAlignment(Pos.CENTER_LEFT);
        Label dueLabel = new Label("Calculated Due Date (Due Date):");
        dueLabel.setStyle("-fx-font-weight: bold;");
        issueDueDatePreviewLabel.getStyleClass().add("metric-value");
        issueDueDatePreviewLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #1d4ed8;");
        dueRow.getChildren().addAll(dueLabel, issueDueDatePreviewLabel);

        issueConfirmButton.getStyleClass().addAll("btn-primary");
        issueConfirmButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 28;");
        issueConfirmButton.setDisable(true);

        actionBox.getChildren().addAll(dueRow, issueConfirmButton);

        layout.getChildren().addAll(issueFeedbackLabel, twoColumns, actionBox);

        ScrollPane scroll = new ScrollPane(layout);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        return scroll;
    }

    private ScrollPane buildReturnContent() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        returnFeedbackLabel.getStyleClass().add("banner-info");
        returnFeedbackLabel.setVisible(false);
        returnFeedbackLabel.setManaged(false);

        // Top Scan Bar
        VBox scanBox = new VBox(8);
        scanBox.getStyleClass().add("metric-card");
        Label scanTitle = new Label("Scan Returned Book Barcode / বইয়ের বারকোড দিন");
        scanTitle.getStyleClass().add("card-title");
        HBox scanRow = new HBox(8);
        returnBarcodeField.setPromptText("Scan or enter copy barcode (e.g. BC100001)");
        returnBarcodeField.getStyleClass().add("search-field");
        HBox.setHgrow(returnBarcodeField, Priority.ALWAYS);
        returnLookupBtn.getStyleClass().add("btn-primary");
        scanRow.getChildren().addAll(returnBarcodeField, returnLookupBtn);
        scanBox.getChildren().addAll(scanTitle, scanRow);

        // Center: Loan Details & Overdue Assessment
        HBox centerRow = new HBox(20);
        centerRow.setAlignment(Pos.TOP_LEFT);

        // Loan Details
        VBox loanCard = new VBox(12);
        loanCard.getStyleClass().add("metric-card");
        loanCard.setPrefWidth(420);
        HBox.setHgrow(loanCard, Priority.ALWAYS);

        Label loanCardTitle = new Label("Active Loan Record / বর্তমান ঋণ তথ্য");
        loanCardTitle.getStyleClass().add("card-title");

        GridPane loanGrid = new GridPane();
        loanGrid.setHgap(12);
        loanGrid.setVgap(8);

        loanGrid.add(new Label("Book Title:"), 0, 0);
        returnBookTitleLabel.setStyle("-fx-font-weight: bold;");
        loanGrid.add(returnBookTitleLabel, 1, 0);

        loanGrid.add(new Label("Barcode:"), 0, 1);
        loanGrid.add(returnBarcodeLabel, 1, 1);

        loanGrid.add(new Label("Borrower:"), 0, 2);
        loanGrid.add(returnMemberLabel, 1, 2);

        loanGrid.add(new Label("Issued Date:"), 0, 3);
        loanGrid.add(returnIssuedDateLabel, 1, 3);

        loanGrid.add(new Label("Due Date:"), 0, 4);
        loanGrid.add(returnDueDateLabel, 1, 4);

        loanGrid.add(new Label("Return Condition:"), 0, 5);
        returnConditionCombo.getItems().addAll(ConditionStatus.values());
        returnConditionCombo.setValue(ConditionStatus.GOOD);
        returnConditionCombo.getStyleClass().add("form-input");
        loanGrid.add(returnConditionCombo, 1, 5);

        loanGrid.add(new Label("Notes:"), 0, 6);
        returnNotesArea.setPromptText("Optional return notes or inspection remarks");
        returnNotesArea.setPrefRowCount(2);
        returnNotesArea.getStyleClass().add("form-input");
        loanGrid.add(returnNotesArea, 1, 6);

        loanCard.getChildren().addAll(loanCardTitle, loanGrid);

        // Fine Preview Card
        VBox fineCard = new VBox(12);
        fineCard.getStyleClass().add("metric-card");
        fineCard.setPrefWidth(420);
        HBox.setHgrow(fineCard, Priority.ALWAYS);

        Label fineCardTitle = new Label("Overdue & Fine Calculation (জরিমানা হিসাব)");
        fineCardTitle.getStyleClass().add("card-title");

        GridPane fineGrid = new GridPane();
        fineGrid.setHgap(12);
        fineGrid.setVgap(10);

        fineGrid.add(new Label("Overdue Status:"), 0, 0);
        returnFineStatusBadge.getStyleClass().addAll("badge", "badge-success");
        fineGrid.add(returnFineStatusBadge, 1, 0);

        fineGrid.add(new Label("Calendar Days Past Due:"), 0, 1);
        fineGrid.add(returnOverdueDaysLabel, 1, 1);

        fineGrid.add(new Label("Daily Fine Rate (Snapshot):"), 0, 2);
        fineGrid.add(returnDailyRateLabel, 1, 2);

        fineGrid.add(new Label("Total Fine Charged (৳):"), 0, 3);
        returnCalculatedFineLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;");
        fineGrid.add(returnCalculatedFineLabel, 1, 3);

        fineCard.getChildren().addAll(fineCardTitle, fineGrid);

        centerRow.getChildren().addAll(loanCard, fineCard);

        // Confirm Return Button
        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        returnConfirmButton.getStyleClass().addAll("btn-primary");
        returnConfirmButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 28;");
        returnConfirmButton.setDisable(true);
        actionRow.getChildren().add(returnConfirmButton);

        layout.getChildren().addAll(returnFeedbackLabel, scanBox, centerRow, actionRow);

        ScrollPane scroll = new ScrollPane(layout);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        return scroll;
    }

    private VBox buildActiveLoansContent() {
        VBox layout = new VBox(16);
        layout.setPadding(new Insets(16, 0, 0, 0));

        // Filters row
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        loansSearchField.setPromptText("Search title, member, card, phone, or barcode...");
        loansSearchField.getStyleClass().add("search-field");
        HBox.setHgrow(loansSearchField, Priority.ALWAYS);

        loansStatusFilter.getItems().addAll("ALL", "ACTIVE", "RETURNED");
        loansStatusFilter.setValue("ACTIVE");
        loansStatusFilter.getStyleClass().add("form-input");

        loansOverdueOnlyCheck.getStyleClass().add("checkbox-custom");

        loansSearchBtn.getStyleClass().add("btn-primary");
        loansResetBtn.getStyleClass().add("btn-secondary");

        filterBar.getChildren().addAll(
                loansSearchField,
                loansStatusFilter,
                loansOverdueOnlyCheck,
                loansSearchBtn,
                loansResetBtn
        );

        // Table
        configureLoansTable();
        VBox.setVgrow(loansTable, Priority.ALWAYS);

        loansCountLabel.getStyleClass().add("table-count-label");

        layout.getChildren().addAll(filterBar, loansTable, loansCountLabel);
        return layout;
    }

    private void configureLoansTable() {
        loansTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loansTable.getStyleClass().add("data-table");

        TableColumn<Loan, Long> colId = new TableColumn<>("#");
        colId.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getId()));
        colId.setMaxWidth(60);
        colId.setMinWidth(50);

        TableColumn<Loan, String> colBarcode = new TableColumn<>("Barcode");
        colBarcode.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCopyBarcode()));
        colBarcode.setMinWidth(110);

        TableColumn<Loan, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getBookTitle()));
        colTitle.setMinWidth(180);

        TableColumn<Loan, String> colMember = new TableColumn<>("Member (Card)");
        colMember.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getMemberName() + " (" + d.getValue().getMembershipNumber() + ")"));
        colMember.setMinWidth(180);

        TableColumn<Loan, String> colIssued = new TableColumn<>("Issued Date");
        colIssued.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getIssuedAt() != null ? com.libraryos.core.util.DateTimeUtil.formatDateTime(d.getValue().getIssuedAt()) : "—"));
        colIssued.setMinWidth(140);

        TableColumn<Loan, String> colDue = new TableColumn<>("Due Date");
        colDue.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                com.libraryos.core.util.DateTimeUtil.formatDate(d.getValue().getDueDate())));
        colDue.setMinWidth(110);

        TableColumn<Loan, Integer> colRenewals = new TableColumn<>("Renewals");
        colRenewals.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getRenewalCount()));
        colRenewals.setMaxWidth(80);
        colRenewals.setMinWidth(70);

        TableColumn<Loan, LoanStatus> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LoanStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Loan loan = getTableRow().getItem();
                    Label badge = new Label(item.name());
                    badge.getStyleClass().add("badge");
                    if (loan != null && loan.isOverdue()) {
                        badge.setText("OVERDUE (" + loan.getDaysOverdue() + "d)");
                        badge.getStyleClass().add("badge-error");
                    } else if (item == LoanStatus.ACTIVE) {
                        badge.getStyleClass().add("badge-success");
                    } else {
                        badge.getStyleClass().add("badge-info");
                    }
                    setGraphic(badge);
                }
            }
        });
        colStatus.setMinWidth(130);

        loansTable.getColumns().addAll(colId, colBarcode, colTitle, colMember, colIssued, colDue, colRenewals, colStatus);
    }

    // --- Getters for Controller ---

    public Button getPolicyButton() { return policyButton; }
    public Button getRefreshButton() { return refreshButton; }
    public TabPane getTabPane() { return tabPane; }

    public Tab getIssueTab() { return issueTab; }
    public TextField getIssueBarcodeField() { return issueBarcodeField; }
    public Button getIssueLookupCopyBtn() { return issueLookupCopyBtn; }
    public Label getIssueCopyTitleLabel() { return issueCopyTitleLabel; }
    public Label getIssueCopyBarcodeLabel() { return issueCopyBarcodeLabel; }
    public Label getIssueCopyAccessionLabel() { return issueCopyAccessionLabel; }
    public Label getIssueCopyShelfLabel() { return issueCopyShelfLabel; }
    public Label getIssueCopyConditionLabel() { return issueCopyConditionLabel; }
    public Label getIssueCopyStatusBadge() { return issueCopyStatusBadge; }

    public TextField getIssueMemberField() { return issueMemberField; }
    public Button getIssueLookupMemberBtn() { return issueLookupMemberBtn; }
    public Label getIssueMemberNameLabel() { return issueMemberNameLabel; }
    public Label getIssueMemberCardLabel() { return issueMemberCardLabel; }
    public Label getIssueMemberTypeLabel() { return issueMemberTypeLabel; }
    public Label getIssueMemberLoansLabel() { return issueMemberLoansLabel; }
    public Label getIssueMemberFinesLabel() { return issueMemberFinesLabel; }
    public Label getIssueMemberStatusBadge() { return issueMemberStatusBadge; }

    public Label getIssueDueDatePreviewLabel() { return issueDueDatePreviewLabel; }
    public Button getIssueConfirmButton() { return issueConfirmButton; }
    public Label getIssueFeedbackLabel() { return issueFeedbackLabel; }

    public Tab getReturnTab() { return returnTab; }
    public TextField getReturnBarcodeField() { return returnBarcodeField; }
    public Button getReturnLookupBtn() { return returnLookupBtn; }
    public Label getReturnBookTitleLabel() { return returnBookTitleLabel; }
    public Label getReturnBarcodeLabel() { return returnBarcodeLabel; }
    public Label getReturnMemberLabel() { return returnMemberLabel; }
    public Label getReturnIssuedDateLabel() { return returnIssuedDateLabel; }
    public Label getReturnDueDateLabel() { return returnDueDateLabel; }
    public ComboBox<ConditionStatus> getReturnConditionCombo() { return returnConditionCombo; }
    public TextArea getReturnNotesArea() { return returnNotesArea; }

    public Label getReturnOverdueDaysLabel() { return returnOverdueDaysLabel; }
    public Label getDailyRateLabel() { return returnDailyRateLabel; }
    public Label getReturnCalculatedFineLabel() { return returnCalculatedFineLabel; }
    public Label getReturnFineStatusBadge() { return returnFineStatusBadge; }
    public Button getReturnConfirmButton() { return returnConfirmButton; }
    public Label getReturnFeedbackLabel() { return returnFeedbackLabel; }

    public Tab getActiveLoansTab() { return activeLoansTab; }
    public TextField getLoansSearchField() { return loansSearchField; }
    public ComboBox<String> getLoansStatusFilter() { return loansStatusFilter; }
    public CheckBox getLoansOverdueOnlyCheck() { return loansOverdueOnlyCheck; }
    public Button getLoansSearchBtn() { return loansSearchBtn; }
    public Button getLoansResetBtn() { return loansResetBtn; }
    public TableView<Loan> getLoansTable() { return loansTable; }
    public Label getLoansCountLabel() { return loansCountLabel; }
}

