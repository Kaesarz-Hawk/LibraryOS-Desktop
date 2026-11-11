package com.libraryos.view;

import com.libraryos.core.util.CurrencyFormatUtil;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.model.inventory.Shelf;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Modern JavaFX Physical Inventory Screen with barcode/accession scanning, status badges,
 * and shelf tracking.
 */
public class InventoryView extends BorderPane {

    private final TextField searchField;
    private final ComboBox<String> statusFilter;
    private final ComboBox<Shelf> shelfFilter;
    private final Button resetButton;
    private final Button addCopyButton;
    private final TableView<BookCopy> copyTable;
    private final Label countLabel;
    private final ProgressIndicator loadingSpinner;

    public InventoryView() {
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        // Top Section
        VBox topBox = new VBox(16);

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Physical Copies & Inventory / বই কপি");
        title.getStyleClass().add("page-title");

        countLabel = new Label("Loading physical inventory...");
        countLabel.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, countLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(20, 20);
        loadingSpinner.setVisible(false);

        addCopyButton = new Button("+ Add Physical Copy / নতুন কপি");
        addCopyButton.getStyleClass().add("btn-primary");

        headerRow.getChildren().addAll(titleBox, spacer, loadingSpinner, addCopyButton);

        // Filter Toolbar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().addAll("card-panel", "filter-container");

        searchField = new TextField();
        searchField.setPromptText("Scan barcode, accession #, or title...");
        searchField.getStyleClass().add("form-input");
        searchField.setPrefWidth(320);

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("ALL", "AVAILABLE", "ISSUED", "RESERVED", "LOST", "DAMAGED", "UNDER_MAINTENANCE", "WITHDRAWN");
        statusFilter.setValue("ALL");
        statusFilter.getStyleClass().add("form-input");
        statusFilter.setPrefWidth(180);

        shelfFilter = new ComboBox<>();
        shelfFilter.setPromptText("All Shelves");
        shelfFilter.getStyleClass().add("form-input");
        shelfFilter.setPrefWidth(200);

        resetButton = new Button("Reset");
        resetButton.getStyleClass().add("btn-secondary");

        filterBar.getChildren().addAll(searchField, statusFilter, shelfFilter, resetButton);
        topBox.getChildren().addAll(headerRow, filterBar);
        setTop(topBox);

        // Center Table View
        copyTable = new TableView<>();
        copyTable.getStyleClass().add("table-view");
        copyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BookCopy, Number> idCol = new TableColumn<>("#");
        idCol.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        idCol.setMaxWidth(60);

        TableColumn<BookCopy, String> accessionCol = new TableColumn<>("Accession No.");
        accessionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAccessionNumber()));
        accessionCol.setMinWidth(140);

        TableColumn<BookCopy, String> barcodeCol = new TableColumn<>("Barcode");
        barcodeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBarcode()));
        barcodeCol.setMinWidth(130);

        TableColumn<BookCopy, String> bookCol = new TableColumn<>("Book Title / শিরোনাম");
        bookCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getBookTitle() != null ? data.getValue().getBookTitle() : "Book #" + data.getValue().getBookId()));
        bookCol.setMinWidth(260);

        TableColumn<BookCopy, String> shelfCol = new TableColumn<>("Shelf Location");
        shelfCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getShelfCode() != null ? data.getValue().getShelfCode() : "Unassigned"));
        shelfCol.setMinWidth(130);

        TableColumn<BookCopy, String> condCol = new TableColumn<>("Condition");
        condCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConditionStatus().name()));
        condCol.setMinWidth(100);

        TableColumn<BookCopy, String> costCol = new TableColumn<>("Cost (BDT)");
        costCol.setCellValueFactory(data -> new SimpleStringProperty(CurrencyFormatUtil.format(data.getValue().getPurchaseCost())));
        costCol.setMinWidth(110);

        TableColumn<BookCopy, String> statusCol = new TableColumn<>("Status / অবস্থা");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        statusCol.setMinWidth(140);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    switch (item) {
                        case "AVAILABLE" -> badge.getStyleClass().add("badge-available");
                        case "ISSUED" -> badge.getStyleClass().add("badge-issued");
                        case "RESERVED" -> badge.getStyleClass().add("badge-reserved");
                        default -> badge.getStyleClass().add("badge-overdue");
                    }
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        copyTable.getColumns().addAll(idCol, accessionCol, barcodeCol, bookCol, shelfCol, condCol, costCol, statusCol);
        copyTable.setPlaceholder(new Label("No physical copies found."));

        VBox centerBox = new VBox(copyTable);
        centerBox.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(copyTable, Priority.ALWAYS);
        setCenter(centerBox);
    }

    public TextField getSearchField() {
        return searchField;
    }

    public ComboBox<String> getStatusFilter() {
        return statusFilter;
    }

    public ComboBox<Shelf> getShelfFilter() {
        return shelfFilter;
    }

    public Button getResetButton() {
        return resetButton;
    }

    public Button getAddCopyButton() {
        return addCopyButton;
    }

    public TableView<BookCopy> getCopyTable() {
        return copyTable;
    }

    public Label getCountLabel() {
        return countLabel;
    }

    public ProgressIndicator getLoadingSpinner() {
        return loadingSpinner;
    }
}

