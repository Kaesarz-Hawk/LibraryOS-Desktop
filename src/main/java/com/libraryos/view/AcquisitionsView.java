package com.libraryos.view;

import com.libraryos.core.util.CurrencyFormatUtil;
import com.libraryos.model.acquisition.Acquisition;
import com.libraryos.model.acquisition.AcquisitionItem;
import com.libraryos.model.acquisition.AcquisitionStatus;
import com.libraryos.model.acquisition.Supplier;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AcquisitionsView extends BorderPane {

    private final TabPane tabPane;
    private final Tab ordersTab;
    private final Tab suppliersTab;

    // Tab 1: Orders controls
    private final ComboBox<String> statusFilterCombo;
    private final Button refreshOrdersBtn;
    private final Button newOrderBtn;
    private final TableView<Acquisition> ordersTable;
    private final TableView<AcquisitionItem> orderItemsTable;
    private final Button receiveOrderBtn;
    private final Button cancelOrderBtn;
    private final Label ordersStatusLabel;

    // Tab 2: Suppliers controls
    private final TextField supplierSearchField;
    private final Button supplierSearchBtn;
    private final Button refreshSuppliersBtn;
    private final Button addSupplierBtn;
    private final TableView<Supplier> suppliersTable;
    private final Button editSupplierBtn;
    private final Button deleteSupplierBtn;
    private final Label suppliersStatusLabel;

    public AcquisitionsView() {
        setPadding(new Insets(24));
        setStyle("-fx-background-color: #F8FAFC;");

        // 1. Header
        VBox headerBox = new VBox(6);
        headerBox.setPadding(new Insets(0, 0, 16, 0));

        Label titleLabel = new Label("Suppliers & Acquisitions / সংগ্রহ ও সরবরাহকারী");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        Label subtitleLabel = new Label("Manage publisher procurement orders, track book supply batches, and atomically generate barcoded physical copies.");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        headerBox.getChildren().addAll(titleLabel, subtitleLabel);
        setTop(headerBox);

        // 2. TabPane
        tabPane = new TabPane();
        tabPane.getStyleClass().add("card-panel");

        // ==========================================
        // TAB 1: Purchase Orders & Receiving
        // ==========================================
        ordersTab = new Tab("Acquisition Orders / সংগ্রহ অর্ডার");
        ordersTab.setClosable(false);

        VBox ordersContent = new VBox(12);
        ordersContent.setPadding(new Insets(16));

        // Toolbar
        HBox ordersToolbar = new HBox(10);
        ordersToolbar.setAlignment(Pos.CENTER_LEFT);

        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.getItems().addAll("ALL / সকল", "ORDERED / অর্ডারকৃত", "RECEIVED / গৃহীত", "CANCELLED / বাতিল");
        statusFilterCombo.setValue("ALL / সকল");
        statusFilterCombo.setPrefWidth(180);

        refreshOrdersBtn = new Button("↻ Refresh");
        refreshOrdersBtn.getStyleClass().add("btn-secondary");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        newOrderBtn = new Button("+ New Acquisition PO / নতুন সংগ্রহ অর্ডার");
        newOrderBtn.getStyleClass().add("btn-primary");

        ordersToolbar.getChildren().addAll(new Label("Filter Status:"), statusFilterCombo, refreshOrdersBtn, spacer1, newOrderBtn);

        // Orders Table
        ordersTable = new TableView<>();
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ordersTable.setPrefHeight(260);

        TableColumn<Acquisition, String> colPo = new TableColumn<>("PO Number / নম্বর");
        colPo.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getAcquisitionNumber()));
        colPo.setPrefWidth(140);

        TableColumn<Acquisition, String> colSup = new TableColumn<>("Supplier / সরবরাহকারী");
        colSup.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getSupplierName()));
        colSup.setPrefWidth(180);

        TableColumn<Acquisition, String> colInv = new TableColumn<>("Invoice #");
        colInv.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getInvoiceNumber() != null ? a.getValue().getInvoiceNumber() : "—"));
        colInv.setPrefWidth(100);

        TableColumn<Acquisition, String> colDate = new TableColumn<>("Order Date");
        colDate.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getOrderDate() != null ? a.getValue().getOrderDate().toString() : ""));
        colDate.setPrefWidth(100);

        TableColumn<Acquisition, String> colRecDate = new TableColumn<>("Received Date");
        colRecDate.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getReceivedDate() != null ? a.getValue().getReceivedDate().toString() : "—"));
        colRecDate.setPrefWidth(110);

        TableColumn<Acquisition, Integer> colUnits = new TableColumn<>("Total Units");
        colUnits.setCellValueFactory(a -> new SimpleObjectProperty<>(a.getValue().getTotalQuantity()));
        colUnits.setPrefWidth(90);

        TableColumn<Acquisition, String> colCost = new TableColumn<>("Total Cost (BDT)");
        colCost.setCellValueFactory(a -> new SimpleStringProperty(CurrencyFormatUtil.format(a.getValue().getTotalCostBdt())));
        colCost.setPrefWidth(120);

        TableColumn<Acquisition, String> colStatus = new TableColumn<>("Status / অবস্থা");
        colStatus.setCellValueFactory(a -> new SimpleStringProperty(a.getValue().getStatus().getDisplayName()));
        colStatus.setPrefWidth(130);

        ordersTable.getColumns().addAll(colPo, colSup, colInv, colDate, colRecDate, colUnits, colCost, colStatus);

        // Line Items section
        Label itemHeader = new Label("Order Line Items / অর্ডারের বই বিবরণ:");
        itemHeader.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-padding: 4 0 0 0;");

        orderItemsTable = new TableView<>();
        orderItemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        orderItemsTable.setPrefHeight(160);

        TableColumn<AcquisitionItem, String> colItemBook = new TableColumn<>("Book Title");
        colItemBook.setCellValueFactory(i -> new SimpleStringProperty(i.getValue().getBookTitle()));

        TableColumn<AcquisitionItem, String> colItemIsbn = new TableColumn<>("ISBN");
        colItemIsbn.setCellValueFactory(i -> new SimpleStringProperty(i.getValue().getBookIsbn() != null ? i.getValue().getBookIsbn() : "—"));

        TableColumn<AcquisitionItem, Integer> colItemQty = new TableColumn<>("Quantity");
        colItemQty.setCellValueFactory(i -> new SimpleObjectProperty<>(i.getValue().getQuantity()));

        TableColumn<AcquisitionItem, String> colItemUnit = new TableColumn<>("Unit Cost (BDT)");
        colItemUnit.setCellValueFactory(i -> new SimpleStringProperty(CurrencyFormatUtil.format(i.getValue().getUnitCostBdt())));

        TableColumn<AcquisitionItem, String> colItemSub = new TableColumn<>("Subtotal (BDT)");
        colItemSub.setCellValueFactory(i -> new SimpleStringProperty(CurrencyFormatUtil.format(i.getValue().getSubtotalBdt())));

        orderItemsTable.getColumns().addAll(colItemBook, colItemIsbn, colItemQty, colItemUnit, colItemSub);

        // Actions bar
        HBox ordersBottomBar = new HBox(12);
        ordersBottomBar.setAlignment(Pos.CENTER_LEFT);

        receiveOrderBtn = new Button("📦 Receive Order & Generate Copies / পণ্য গ্রহণ ও কপি তৈরি");
        receiveOrderBtn.getStyleClass().add("btn-primary");
        receiveOrderBtn.setDisable(true);

        cancelOrderBtn = new Button("✕ Cancel Order / অর্ডার বাতিল");
        cancelOrderBtn.getStyleClass().add("btn-danger");
        cancelOrderBtn.setDisable(true);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        ordersStatusLabel = new Label("Ready.");
        ordersStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        ordersBottomBar.getChildren().addAll(receiveOrderBtn, cancelOrderBtn, spacer2, ordersStatusLabel);

        ordersContent.getChildren().addAll(ordersToolbar, ordersTable, itemHeader, orderItemsTable, ordersBottomBar);
        ordersTab.setContent(ordersContent);

        // ==========================================
        // TAB 2: Suppliers Directory
        // ==========================================
        suppliersTab = new Tab("Suppliers Directory / সরবরাহকারী তালিকা");
        suppliersTab.setClosable(false);

        VBox suppliersContent = new VBox(12);
        suppliersContent.setPadding(new Insets(16));

        HBox suppliersToolbar = new HBox(10);
        suppliersToolbar.setAlignment(Pos.CENTER_LEFT);

        supplierSearchField = new TextField();
        supplierSearchField.setPromptText("Search supplier name, phone, email, contact person...");
        supplierSearchField.setPrefWidth(350);
        supplierSearchField.getStyleClass().add("search-field");

        supplierSearchBtn = new Button("Search / খুঁজুন");
        supplierSearchBtn.getStyleClass().add("btn-secondary");

        refreshSuppliersBtn = new Button("↻ Refresh");
        refreshSuppliersBtn.getStyleClass().add("btn-secondary");

        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);

        addSupplierBtn = new Button("+ Add Supplier / নতুন সরবরাহকারী");
        addSupplierBtn.getStyleClass().add("btn-primary");

        suppliersToolbar.getChildren().addAll(supplierSearchField, supplierSearchBtn, refreshSuppliersBtn, spacer3, addSupplierBtn);

        // Suppliers Table
        suppliersTable = new TableView<>();
        suppliersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(suppliersTable, Priority.ALWAYS);

        TableColumn<Supplier, String> colSName = new TableColumn<>("Supplier Name / নাম");
        colSName.setCellValueFactory(s -> new SimpleStringProperty(s.getValue().getName()));
        colSName.setPrefWidth(220);

        TableColumn<Supplier, String> colSContact = new TableColumn<>("Contact Person");
        colSContact.setCellValueFactory(s -> new SimpleStringProperty(s.getValue().getContactPerson()));
        colSContact.setPrefWidth(150);

        TableColumn<Supplier, String> colSPhone = new TableColumn<>("Phone / ফোন");
        colSPhone.setCellValueFactory(s -> new SimpleStringProperty(s.getValue().getPhone()));
        colSPhone.setPrefWidth(130);

        TableColumn<Supplier, String> colSEmail = new TableColumn<>("Email");
        colSEmail.setCellValueFactory(s -> new SimpleStringProperty(s.getValue().getEmail()));
        colSEmail.setPrefWidth(160);

        TableColumn<Supplier, String> colSAddress = new TableColumn<>("Address / ঠিকানা");
        colSAddress.setCellValueFactory(s -> new SimpleStringProperty(s.getValue().getAddress()));
        colSAddress.setPrefWidth(220);

        TableColumn<Supplier, String> colSStatus = new TableColumn<>("Status");
        colSStatus.setCellValueFactory(s -> new SimpleStringProperty(s.getValue().isActive() ? "Active / সক্রিয়" : "Inactive / নিষ্ক্রিয়"));
        colSStatus.setPrefWidth(100);

        suppliersTable.getColumns().addAll(colSName, colSContact, colSPhone, colSEmail, colSAddress, colSStatus);

        HBox suppliersBottomBar = new HBox(12);
        suppliersBottomBar.setAlignment(Pos.CENTER_LEFT);

        editSupplierBtn = new Button("✏ Edit Supplier / সম্পাদনা");
        editSupplierBtn.getStyleClass().add("btn-secondary");
        editSupplierBtn.setDisable(true);

        deleteSupplierBtn = new Button("Deactivate / নিষ্ক্রিয় করুন");
        deleteSupplierBtn.getStyleClass().add("btn-danger");
        deleteSupplierBtn.setDisable(true);

        Region spacer4 = new Region();
        HBox.setHgrow(spacer4, Priority.ALWAYS);

        suppliersStatusLabel = new Label("Ready.");
        suppliersStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        suppliersBottomBar.getChildren().addAll(editSupplierBtn, deleteSupplierBtn, spacer4, suppliersStatusLabel);

        suppliersContent.getChildren().addAll(suppliersToolbar, suppliersTable, suppliersBottomBar);
        suppliersTab.setContent(suppliersContent);

        tabPane.getTabs().addAll(ordersTab, suppliersTab);
        setCenter(tabPane);
    }

    // Getters for Controller
    public TabPane getTabPane() { return tabPane; }
    public Tab getOrdersTab() { return ordersTab; }
    public Tab getSuppliersTab() { return suppliersTab; }

    public ComboBox<String> getStatusFilterCombo() { return statusFilterCombo; }
    public Button getRefreshOrdersBtn() { return refreshOrdersBtn; }
    public Button getNewOrderBtn() { return newOrderBtn; }
    public TableView<Acquisition> getOrdersTable() { return ordersTable; }
    public TableView<AcquisitionItem> getOrderItemsTable() { return orderItemsTable; }
    public Button getReceiveOrderBtn() { return receiveOrderBtn; }
    public Button getCancelOrderBtn() { return cancelOrderBtn; }
    public Label getOrdersStatusLabel() { return ordersStatusLabel; }

    public TextField getSupplierSearchField() { return supplierSearchField; }
    public Button getSupplierSearchBtn() { return supplierSearchBtn; }
    public Button getRefreshSuppliersBtn() { return refreshSuppliersBtn; }
    public Button getAddSupplierBtn() { return addSupplierBtn; }
    public TableView<Supplier> getSuppliersTable() { return suppliersTable; }
    public Button getEditSupplierBtn() { return editSupplierBtn; }
    public Button getDeleteSupplierBtn() { return deleteSupplierBtn; }
    public Label getSuppliersStatusLabel() { return suppliersStatusLabel; }
}
