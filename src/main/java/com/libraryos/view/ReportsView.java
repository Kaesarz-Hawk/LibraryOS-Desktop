package com.libraryos.view;

import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.dto.CatalogInventoryReportItem;
import com.libraryos.dto.CirculationStatsDTO;
import com.libraryos.dto.OverdueReportItem;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentMethod;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ReportsView extends BorderPane {

    private final Button refreshBtn = new Button("↻ Refresh");
    private final TabPane tabPane = new TabPane();

    // --- Tab 1: Overdue Report ---
    private final Tab overdueTab = new Tab("Overdue Loans & Penalties (মেয়াদোত্তীর্ণ বই)");
    private final ComboBox<String> overdueDaysFilter = new ComboBox<>();
    private final Button overdueFilterBtn = new Button("Apply Filter");
    private final Button overduePdfBtn = new Button("Export PDF (পিডিএফ)");
    private final Button overdueExcelBtn = new Button("Export Excel (এক্সেল)");
    private final TableView<OverdueReportItem> overdueTable = new TableView<>();
    private final Label overdueSummaryLabel = new Label("Showing 0 overdue loans");

    // --- Tab 2: Fine Collections Report ---
    private final Tab collectionsTab = new Tab("Fine Collections & Receipts (ফি ও রাজস্ব)");
    private final DatePicker collectionsStartDate = new DatePicker();
    private final DatePicker collectionsEndDate = new DatePicker();
    private final ComboBox<String> collectionsMethodFilter = new ComboBox<>();
    private final Button collectionsFilterBtn = new Button("Filter Range");
    private final Button collectionsResetBtn = new Button("Reset");
    private final Button collectionsPdfBtn = new Button("Export PDF (পিডিএফ)");
    private final Button collectionsExcelBtn = new Button("Export Excel (এক্সেল)");
    private final Label collectionsTotalKpi = new Label("৳ 0.00");
    private final Label collectionsCountKpi = new Label("0 receipts");
    private final TableView<Payment> collectionsTable = new TableView<>();
    private final Label collectionsCountLabel = new Label("Showing 0 payments");

    // --- Tab 3: Circulation Stats & Trends ---
    private final Tab statsTab = new Tab("Circulation Statistics & Trends (পরিসংখ্যান)");
    private final Label statActiveLoans = new Label("0");
    private final Label statOverdueLoans = new Label("0");
    private final Label statReturnedLoans = new Label("0");
    private final Label statCollectedFines = new Label("৳ 0.00");
    private final TableView<CirculationStatsDTO.PopularBookItem> topBooksTable = new TableView<>();

    // --- Tab 4: Catalog Inventory Audit ---
    private final Tab catalogTab = new Tab("Catalog & Physical Copy Audit (ইনভেন্টরি)");
    private final TextField catalogSearchField = new TextField();
    private final Button catalogFilterBtn = new Button("Filter");
    private final Button catalogPdfBtn = new Button("Export PDF (পিডিএফ)");
    private final Button catalogExcelBtn = new Button("Export Excel (এক্সেল)");
    private final TableView<CatalogInventoryReportItem> catalogTable = new TableView<>();
    private final Label catalogSummaryLabel = new Label("Showing 0 catalog titles");

    public ReportsView() {
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
        Label title = new Label("Reports & Institutional Analytics / প্রতিবেদন ও তথ্য");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("Generate, preview, and export institutional circulation reports, overdue analyses, and fine ledgers to PDF and Excel");
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

        overdueTab.setContent(buildOverdueTabContent());
        collectionsTab.setContent(buildCollectionsTabContent());
        statsTab.setContent(buildStatsTabContent());
        catalogTab.setContent(buildCatalogTabContent());

        tabPane.getTabs().addAll(overdueTab, collectionsTab, statsTab, catalogTab);
        return tabPane;
    }

    // --- TAB 1: OVERDUE ITEMS ---
    private Node buildOverdueTabContent() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(16, 0, 0, 0));

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.getStyleClass().add("toolbar-panel");

        overdueDaysFilter.getItems().addAll("All Overdue (1+ days)", "7+ Days Overdue", "14+ Days Overdue", "30+ Days Overdue");
        overdueDaysFilter.setValue("All Overdue (1+ days)");
        overdueDaysFilter.getStyleClass().add("form-input");

        overdueFilterBtn.getStyleClass().add("btn-primary");
        overduePdfBtn.getStyleClass().add("btn-secondary");
        overdueExcelBtn.getStyleClass().add("btn-secondary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(
                new Label("Threshold:"), overdueDaysFilter, overdueFilterBtn,
                spacer, overduePdfBtn, overdueExcelBtn
        );

        TableColumn<OverdueReportItem, String> colLoanId = new TableColumn<>("Loan #");
        colLoanId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getLoanId())));
        colLoanId.setPrefWidth(70);

        TableColumn<OverdueReportItem, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookTitle()));
        colTitle.setPrefWidth(220);

        TableColumn<OverdueReportItem, String> colBarcode = new TableColumn<>("Barcode");
        colBarcode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBarcode()));
        colBarcode.setPrefWidth(120);

        TableColumn<OverdueReportItem, String> colMember = new TableColumn<>("Member Name");
        colMember.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMemberName()));
        colMember.setPrefWidth(160);

        TableColumn<OverdueReportItem, String> colCard = new TableColumn<>("Card #");
        colCard.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMembershipNumber()));
        colCard.setPrefWidth(120);

        TableColumn<OverdueReportItem, String> colPhone = new TableColumn<>("Phone / যোগাযোগ");
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMemberPhone() != null ? d.getValue().getMemberPhone() : "—"));
        colPhone.setPrefWidth(120);

        TableColumn<OverdueReportItem, String> colDueDate = new TableColumn<>("Due Date");
        colDueDate.setCellValueFactory(d -> new SimpleStringProperty(DateTimeUtil.formatDate(d.getValue().getDueDate())));
        colDueDate.setPrefWidth(110);

        TableColumn<OverdueReportItem, String> colDays = new TableColumn<>("Days Late");
        colDays.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOverdueDays() + " days"));
        colDays.setStyle("-fx-font-weight: bold; -fx-text-fill: #DC2626;");
        colDays.setPrefWidth(100);

        TableColumn<OverdueReportItem, String> colFine = new TableColumn<>("Accrued Fine (৳)");
        colFine.setCellValueFactory(d -> new SimpleStringProperty("৳ " + d.getValue().getFineAmount().toPlainString()));
        colFine.setStyle("-fx-font-weight: bold; -fx-text-fill: #DC2626;");
        colFine.setPrefWidth(120);

        overdueTable.getColumns().addAll(colLoanId, colTitle, colBarcode, colMember, colCard, colPhone, colDueDate, colDays, colFine);
        overdueTable.setPlaceholder(new Label("No overdue loans matching current filter."));
        overdueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(overdueTable, Priority.ALWAYS);

        HBox footer = new HBox(overdueSummaryLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        overdueSummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        layout.getChildren().addAll(toolbar, overdueTable, footer);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    // --- TAB 2: FINE COLLECTIONS ---
    private Node buildCollectionsTabContent() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(16, 0, 0, 0));

        // Filter bar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.getStyleClass().add("toolbar-panel");

        collectionsStartDate.setPromptText("Start Date");
        collectionsEndDate.setPromptText("End Date");

        collectionsMethodFilter.getItems().addAll("ALL", "CASH", "BKASH", "NAGAD", "ROCKET", "BANK_TRANSFER");
        collectionsMethodFilter.setValue("ALL");
        collectionsMethodFilter.getStyleClass().add("form-input");

        collectionsFilterBtn.getStyleClass().add("btn-primary");
        collectionsResetBtn.getStyleClass().add("btn-secondary");
        collectionsPdfBtn.getStyleClass().add("btn-secondary");
        collectionsExcelBtn.getStyleClass().add("btn-secondary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(
                new Label("From:"), collectionsStartDate,
                new Label("To:"), collectionsEndDate,
                new Label("Method:"), collectionsMethodFilter,
                collectionsFilterBtn, collectionsResetBtn,
                spacer, collectionsPdfBtn, collectionsExcelBtn
        );

        // KPI Summary Header Row
        HBox kpiRow = new HBox(16);
        VBox kpi1 = createKpiBox("TOTAL COLLECTED (BDT)", collectionsTotalKpi, "#15803D", "#DCFCE7", "#BBF7D0");
        VBox kpi2 = createKpiBox("TOTAL RECEIPTS ISSUED", collectionsCountKpi, "#1E293B", "#F8FAFC", "#E2E8F0");
        kpiRow.getChildren().addAll(kpi1, kpi2);

        // Payments Table
        TableColumn<Payment, String> colRec = new TableColumn<>("Receipt #");
        colRec.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReceiptNumber()));
        colRec.setStyle("-fx-font-weight: bold;");
        colRec.setPrefWidth(160);

        TableColumn<Payment, String> colMember = new TableColumn<>("Member Name");
        colMember.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().getMemberName() != null ? d.getValue().getMemberName() : "—")
                + " (" + (d.getValue().getMembershipNumber() != null ? d.getValue().getMembershipNumber() : "—") + ")"
        ));
        colMember.setPrefWidth(200);

        TableColumn<Payment, String> colAmt = new TableColumn<>("Amount (৳)");
        colAmt.setCellValueFactory(d -> new SimpleStringProperty("৳ " + d.getValue().getAmount().toPlainString()));
        colAmt.setStyle("-fx-font-weight: bold; -fx-text-fill: #16A34A;");
        colAmt.setPrefWidth(120);

        TableColumn<Payment, String> colMeth = new TableColumn<>("Method");
        colMeth.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentMethod().name()));
        colMeth.setPrefWidth(110);

        TableColumn<Payment, String> colTrx = new TableColumn<>("Trx Ref");
        colTrx.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTransactionReference() != null ? d.getValue().getTransactionReference() : "—"));
        colTrx.setPrefWidth(130);

        TableColumn<Payment, String> colDate = new TableColumn<>("Paid Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? DateTimeUtil.formatDateTime(d.getValue().getCreatedAt()) : "—"
        ));
        colDate.setPrefWidth(150);

        TableColumn<Payment, String> colStatus = new TableColumn<>("Ledger Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isReversed() ? "REVERSED" : "COMPLETED"));
        colStatus.setPrefWidth(120);

        collectionsTable.getColumns().addAll(colRec, colMember, colAmt, colMeth, colTrx, colDate, colStatus);
        collectionsTable.setPlaceholder(new Label("No payment collections found for selected range."));
        collectionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(collectionsTable, Priority.ALWAYS);

        HBox footer = new HBox(collectionsCountLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        collectionsCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        layout.getChildren().addAll(toolbar, kpiRow, collectionsTable, footer);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    // --- TAB 3: CIRCULATION STATS & TRENDS ---
    private Node buildStatsTabContent() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(16, 0, 0, 0));

        // 4 KPI Cards
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            grid.getColumnConstraints().add(cc);
        }

        VBox c1 = createKpiBox("ACTIVE BOOK LOANS", statActiveLoans, "#1D4ED8", "#EFF6FF", "#BFDBFE");
        VBox c2 = createKpiBox("OVERDUE BORROWED ITEMS", statOverdueLoans, "#DC2626", "#FEF2F2", "#FECACA");
        VBox c3 = createKpiBox("TOTAL RETURNED LOANS", statReturnedLoans, "#15803D", "#DCFCE7", "#BBF7D0");
        VBox c4 = createKpiBox("CUMULATIVE FINES COLLECTED", statCollectedFines, "#0F172A", "#F8FAFC", "#E2E8F0");

        grid.add(c1, 0, 0);
        grid.add(c2, 1, 0);
        grid.add(c3, 2, 0);
        grid.add(c4, 3, 0);

        // Top Borrowed Titles Table
        VBox tableBox = new VBox(8);
        Label topBooksLabel = new Label("Top Most Frequently Borrowed Books (জনপ্রিয় বই)");
        topBooksLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        TableColumn<CirculationStatsDTO.PopularBookItem, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colTitle.setPrefWidth(300);

        TableColumn<CirculationStatsDTO.PopularBookItem, String> colIsbn = new TableColumn<>("ISBN");
        colIsbn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIsbn() != null ? d.getValue().getIsbn() : "—"));
        colIsbn.setPrefWidth(150);

        TableColumn<CirculationStatsDTO.PopularBookItem, String> colCount = new TableColumn<>("Total Times Borrowed");
        colCount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBorrowCount() + " times"));
        colCount.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563EB;");
        colCount.setPrefWidth(160);

        topBooksTable.getColumns().addAll(colTitle, colIsbn, colCount);
        topBooksTable.setPlaceholder(new Label("No circulation history recorded yet."));
        topBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(topBooksTable, Priority.ALWAYS);

        tableBox.getChildren().addAll(topBooksLabel, topBooksTable);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        layout.getChildren().addAll(grid, tableBox);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    // --- TAB 4: CATALOG INVENTORY AUDIT ---
    private Node buildCatalogTabContent() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(16, 0, 0, 0));

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 16, 12, 16));
        toolbar.getStyleClass().add("toolbar-panel");

        catalogSearchField.setPromptText("Filter catalog by title or ISBN...");
        catalogSearchField.getStyleClass().add("form-input");
        catalogSearchField.setPrefWidth(300);

        catalogFilterBtn.getStyleClass().add("btn-primary");
        catalogPdfBtn.getStyleClass().add("btn-secondary");
        catalogExcelBtn.getStyleClass().add("btn-secondary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(
                new Label("Search:"), catalogSearchField, catalogFilterBtn,
                spacer, catalogPdfBtn, catalogExcelBtn
        );

        TableColumn<CatalogInventoryReportItem, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getBookId())));
        colId.setPrefWidth(60);

        TableColumn<CatalogInventoryReportItem, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayName()));
        colTitle.setPrefWidth(260);

        TableColumn<CatalogInventoryReportItem, String> colIsbn = new TableColumn<>("ISBN");
        colIsbn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIsbn() != null ? d.getValue().getIsbn() : "—"));
        colIsbn.setPrefWidth(130);

        TableColumn<CatalogInventoryReportItem, String> colPub = new TableColumn<>("Publisher");
        colPub.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPublisher() != null ? d.getValue().getPublisher() : "—"));
        colPub.setPrefWidth(160);

        TableColumn<CatalogInventoryReportItem, String> colTotal = new TableColumn<>("Total Copies");
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getTotalCopies())));
        colTotal.setPrefWidth(90);

        TableColumn<CatalogInventoryReportItem, String> colAvail = new TableColumn<>("Available");
        colAvail.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAvailableCopies())));
        colAvail.setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold;");
        colAvail.setPrefWidth(90);

        TableColumn<CatalogInventoryReportItem, String> colIssued = new TableColumn<>("Issued");
        colIssued.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getIssuedCopies())));
        colIssued.setStyle("-fx-text-fill: #1D4ED8; -fx-font-weight: bold;");
        colIssued.setPrefWidth(90);

        TableColumn<CatalogInventoryReportItem, String> colDamaged = new TableColumn<>("Damaged/Lost");
        colDamaged.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getDamagedOrLostCopies())));
        colDamaged.setStyle("-fx-text-fill: #B91C1C;");
        colDamaged.setPrefWidth(110);

        catalogTable.getColumns().addAll(colId, colTitle, colIsbn, colPub, colTotal, colAvail, colIssued, colDamaged);
        catalogTable.setPlaceholder(new Label("No catalog titles found."));
        catalogTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(catalogTable, Priority.ALWAYS);

        HBox footer = new HBox(catalogSummaryLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        catalogSummaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        layout.getChildren().addAll(toolbar, catalogTable, footer);
        VBox.setVgrow(layout, Priority.ALWAYS);
        return layout;
    }

    private VBox createKpiBox(String title, Label valLabel, String textColor, String bgHex, String borderHex) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: " + bgHex + "; -fx-border-color: " + borderHex + "; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        valLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + textColor + ";");
        box.getChildren().addAll(t, valLabel);
        return box;
    }

    public Button getRefreshBtn() { return refreshBtn; }
    public TabPane getTabPane() { return tabPane; }

    public Tab getOverdueTab() { return overdueTab; }
    public ComboBox<String> getOverdueDaysFilter() { return overdueDaysFilter; }
    public Button getOverdueFilterBtn() { return overdueFilterBtn; }
    public Button getOverduePdfBtn() { return overduePdfBtn; }
    public Button getOverdueExcelBtn() { return overdueExcelBtn; }
    public TableView<OverdueReportItem> getOverdueTable() { return overdueTable; }
    public Label getOverdueSummaryLabel() { return overdueSummaryLabel; }

    public Tab getCollectionsTab() { return collectionsTab; }
    public DatePicker getCollectionsStartDate() { return collectionsStartDate; }
    public DatePicker getCollectionsEndDate() { return collectionsEndDate; }
    public ComboBox<String> getCollectionsMethodFilter() { return collectionsMethodFilter; }
    public Button getCollectionsFilterBtn() { return collectionsFilterBtn; }
    public Button getCollectionsResetBtn() { return collectionsResetBtn; }
    public Button getCollectionsPdfBtn() { return collectionsPdfBtn; }
    public Button getCollectionsExcelBtn() { return collectionsExcelBtn; }
    public Label getCollectionsTotalKpi() { return collectionsTotalKpi; }
    public Label getCollectionsCountKpi() { return collectionsCountKpi; }
    public TableView<Payment> getCollectionsTable() { return collectionsTable; }
    public Label getCollectionsCountLabel() { return collectionsCountLabel; }

    public Tab getStatsTab() { return statsTab; }
    public Label getStatActiveLoans() { return statActiveLoans; }
    public Label getStatOverdueLoans() { return statOverdueLoans; }
    public Label getStatReturnedLoans() { return statReturnedLoans; }
    public Label getStatCollectedFines() { return statCollectedFines; }
    public TableView<CirculationStatsDTO.PopularBookItem> getTopBooksTable() { return topBooksTable; }

    public Tab getCatalogTab() { return catalogTab; }
    public TextField getCatalogSearchField() { return catalogSearchField; }
    public Button getCatalogFilterBtn() { return catalogFilterBtn; }
    public Button getCatalogPdfBtn() { return catalogPdfBtn; }
    public Button getCatalogExcelBtn() { return catalogExcelBtn; }
    public TableView<CatalogInventoryReportItem> getCatalogTable() { return catalogTable; }
    public Label getCatalogSummaryLabel() { return catalogSummaryLabel; }
}

