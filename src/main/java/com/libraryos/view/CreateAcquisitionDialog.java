package com.libraryos.view;

import com.libraryos.core.util.CurrencyFormatUtil;
import com.libraryos.model.acquisition.AcquisitionItem;
import com.libraryos.model.acquisition.Supplier;
import com.libraryos.model.catalog.Book;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CreateAcquisitionDialog extends Dialog<CreateAcquisitionDialog.Result> {

    public record Result(Long supplierId, String invoiceNumber, LocalDate orderDate, String notes, List<AcquisitionItem> items) {}

    private final ComboBox<Supplier> supplierCombo;
    private final TextField invoiceField;
    private final DatePicker orderDatePicker;
    private final TextArea notesArea;

    private final ComboBox<Book> bookCombo;
    private final Spinner<Integer> qtySpinner;
    private final TextField unitCostField;
    private final Button addItemBtn;
    private final TableView<AcquisitionItem> itemsTable;
    private final Button removeItemBtn;
    private final Label totalCostLabel;

    private final List<AcquisitionItem> lineItems = new ArrayList<>();

    public CreateAcquisitionDialog(Window owner, List<Supplier> suppliers, List<Book> books) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("New Acquisition Order / নতুন সংগ্রহ অর্ডার");
        setHeaderText("Create a book procurement / purchase order with multiple bibliographic titles.");

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("card-panel");
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setText("Create Order / অর্ডার তৈরি");
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setDisable(true);

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));
        content.setPrefWidth(640);

        // Header inputs
        GridPane topGrid = new GridPane();
        topGrid.setHgap(10);
        topGrid.setVgap(10);

        supplierCombo = new ComboBox<>();
        supplierCombo.getItems().addAll(suppliers);
        supplierCombo.setPromptText("Select Supplier / সরবরাহকারী...");
        supplierCombo.setPrefWidth(240);

        invoiceField = new TextField();
        invoiceField.setPromptText("Invoice / Ref Number (Optional)");

        orderDatePicker = new DatePicker(LocalDate.now());

        notesArea = new TextArea();
        notesArea.setPromptText("Purchase notes, procurement reference...");
        notesArea.setPrefRowCount(2);

        topGrid.add(new Label("Supplier: *"), 0, 0);
        topGrid.add(supplierCombo, 1, 0);
        topGrid.add(new Label("Order Date: *"), 2, 0);
        topGrid.add(orderDatePicker, 3, 0);

        topGrid.add(new Label("Invoice #:"), 0, 1);
        topGrid.add(invoiceField, 1, 1);
        topGrid.add(new Label("Notes:"), 2, 1);
        topGrid.add(notesArea, 3, 1);

        content.getChildren().add(topGrid);

        Separator sep = new Separator();
        content.getChildren().add(sep);

        // Line Item Input Form
        Label lineTitle = new Label("Add Book Items / বই লাইন যুক্ত করুন:");
        lineTitle.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A;");

        HBox itemInputBar = new HBox(8);
        itemInputBar.setAlignment(Pos.CENTER_LEFT);

        bookCombo = new ComboBox<>();
        bookCombo.getItems().addAll(books);
        bookCombo.setPromptText("Select Book...");
        bookCombo.setPrefWidth(260);

        qtySpinner = new Spinner<>(1, 1000, 5);
        qtySpinner.setPrefWidth(80);
        qtySpinner.setEditable(true);

        unitCostField = new TextField("350.00");
        unitCostField.setPromptText("Unit BDT");
        unitCostField.setPrefWidth(90);

        addItemBtn = new Button("+ Add Line");
        addItemBtn.getStyleClass().add("btn-secondary");

        itemInputBar.getChildren().addAll(bookCombo, new Label("Qty:"), qtySpinner, new Label("Unit ৳:"), unitCostField, addItemBtn);
        content.getChildren().addAll(lineTitle, itemInputBar);

        // Line Items Table
        itemsTable = new TableView<>();
        itemsTable.setPrefHeight(180);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AcquisitionItem, String> colTitle = new TableColumn<>("Book Title");
        colTitle.setCellValueFactory(i -> new SimpleStringProperty(i.getValue().getBookTitle()));

        TableColumn<AcquisitionItem, Integer> colQty = new TableColumn<>("Quantity");
        colQty.setCellValueFactory(i -> new SimpleObjectProperty<>(i.getValue().getQuantity()));

        TableColumn<AcquisitionItem, String> colUnit = new TableColumn<>("Unit Cost (BDT)");
        colUnit.setCellValueFactory(i -> new SimpleStringProperty(CurrencyFormatUtil.format(i.getValue().getUnitCostBdt())));

        TableColumn<AcquisitionItem, String> colSub = new TableColumn<>("Subtotal (BDT)");
        colSub.setCellValueFactory(i -> new SimpleStringProperty(CurrencyFormatUtil.format(i.getValue().getSubtotalBdt())));

        itemsTable.getColumns().addAll(colTitle, colQty, colUnit, colSub);

        // Bottom summary & remove item
        HBox itemBottomBar = new HBox(12);
        itemBottomBar.setAlignment(Pos.CENTER_LEFT);

        removeItemBtn = new Button("- Remove Selected Item");
        removeItemBtn.getStyleClass().add("btn-danger");
        removeItemBtn.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        totalCostLabel = new Label("Total: ৳ 0.00");
        totalCostLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #1E3A8A;");

        itemBottomBar.getChildren().addAll(removeItemBtn, spacer, totalCostLabel);
        content.getChildren().addAll(itemsTable, itemBottomBar);

        pane.setContent(content);

        // Event wiring
        addItemBtn.setOnAction(e -> {
            Book selBook = bookCombo.getValue();
            if (selBook == null) return;
            int qty = qtySpinner.getValue();
            BigDecimal cost;
            try {
                cost = new BigDecimal(unitCostField.getText().trim());
            } catch (Exception ex) {
                cost = BigDecimal.ZERO;
            }

            AcquisitionItem item = new AcquisitionItem();
            item.setBookId(selBook.getId());
            item.setBookTitle(selBook.getTitle());
            item.setBookIsbn(selBook.getIsbn13() != null ? selBook.getIsbn13() : selBook.getIsbn10());
            item.setQuantity(qty);
            item.setUnitCostBdt(cost);

            lineItems.add(item);
            itemsTable.getItems().setAll(lineItems);
            updateTotal();
            checkValidity(okBtn);
        });

        itemsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            removeItemBtn.setDisable(newVal == null);
        });

        removeItemBtn.setOnAction(e -> {
            AcquisitionItem sel = itemsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                lineItems.remove(sel);
                itemsTable.getItems().setAll(lineItems);
                updateTotal();
                checkValidity(okBtn);
            }
        });

        supplierCombo.valueProperty().addListener((obs, oldVal, newVal) -> checkValidity(okBtn));

        setResultConverter(btnType -> {
            if (btnType == ButtonType.OK && supplierCombo.getValue() != null && !lineItems.isEmpty()) {
                return new Result(
                        supplierCombo.getValue().getId(),
                        invoiceField.getText().trim(),
                        orderDatePicker.getValue(),
                        notesArea.getText().trim(),
                        new ArrayList<>(lineItems)
                );
            }
            return null;
        });
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (AcquisitionItem item : lineItems) {
            total = total.add(item.getSubtotalBdt());
        }
        totalCostLabel.setText("Total: " + CurrencyFormatUtil.format(total));
    }

    private void checkValidity(Button okBtn) {
        boolean valid = supplierCombo.getValue() != null && !lineItems.isEmpty();
        okBtn.setDisable(!valid);
    }
}
