package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.security.SessionManager;
import com.libraryos.model.catalog.Book;
import com.libraryos.model.inventory.*;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.service.CatalogService;
import com.libraryos.service.InventoryService;
import com.libraryos.view.CopyDialog;
import com.libraryos.view.CopyStatusDialog;
import com.libraryos.view.InventoryView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryView view;
    private final InventoryService inventoryService;
    private final CatalogService catalogService;

    public InventoryController(InventoryView view) {
        this.view = view;
        this.inventoryService = AppContext.getInstance().getInventoryService();
        this.catalogService = AppContext.getInstance().getCatalogService();
        initialize();
    }

    private void initialize() {
        view.getAddCopyButton().setDisable(!SessionManager.hasPermission(PermissionCode.COPY_MANAGE));
        view.getAddCopyButton().setOnAction(e -> handleAddCopy());

        view.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> loadCopies());
        view.getStatusFilter().valueProperty().addListener((obs, oldVal, newVal) -> loadCopies());
        view.getShelfFilter().valueProperty().addListener((obs, oldVal, newVal) -> loadCopies());

        view.getResetButton().setOnAction(e -> {
            view.getSearchField().clear();
            view.getStatusFilter().setValue("ALL");
            view.getShelfFilter().setValue(null);
            loadCopies();
        });

        // Context Menu
        ContextMenu cm = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Copy Details / সম্পাদনা");
        editItem.setOnAction(e -> {
            BookCopy selected = view.getCopyTable().getSelectionModel().getSelectedItem();
            if (selected != null) handleEditCopy(selected);
        });

        MenuItem statusItem = new MenuItem("Transition Status / অবস্থা পরিবর্তন");
        statusItem.setOnAction(e -> {
            BookCopy selected = view.getCopyTable().getSelectionModel().getSelectedItem();
            if (selected != null) handleStatusChange(selected);
        });

        MenuItem historyItem = new MenuItem("View Status History / হিস্ট্রি দেখুন");
        historyItem.setOnAction(e -> {
            BookCopy selected = view.getCopyTable().getSelectionModel().getSelectedItem();
            if (selected != null) handleViewHistory(selected);
        });

        MenuItem printBarcodeItem = new MenuItem("Print Barcode Label (PDF) / বারকোড প্রিন্ট");
        printBarcodeItem.setOnAction(e -> {
            BookCopy selected = view.getCopyTable().getSelectionModel().getSelectedItem();
            if (selected != null) handlePrintBarcode(selected);
        });

        cm.getItems().addAll(editItem, statusItem, historyItem, printBarcodeItem);
        view.getCopyTable().setContextMenu(cm);

        loadShelves();
        loadCopies();
    }

    private void loadShelves() {
        Task<List<Shelf>> task = new Task<>() {
            @Override
            protected List<Shelf> call() {
                return inventoryService.getAllShelves();
            }
        };
        task.setOnSucceeded(e -> view.getShelfFilter().setItems(FXCollections.observableArrayList(task.getValue())));
        AppContext.getInstance().getExecutorService().submit(task);
    }

    public void loadCopies() {
        String query = view.getSearchField().getText();
        String statusStr = view.getStatusFilter().getValue();
        CopyStatus status = (statusStr != null && !"ALL".equalsIgnoreCase(statusStr)) ? CopyStatus.valueOf(statusStr) : null;
        Shelf shelf = view.getShelfFilter().getValue();
        Long shelfId = shelf != null ? shelf.getId() : null;

        view.getLoadingSpinner().setVisible(true);

        Task<List<BookCopy>> task = new Task<>() {
            @Override
            protected List<BookCopy> call() {
                return inventoryService.searchCopies(query, status, shelfId);
            }
        };

        task.setOnSucceeded(e -> {
            view.getLoadingSpinner().setVisible(false);
            List<BookCopy> list = task.getValue();
            view.getCopyTable().setItems(FXCollections.observableArrayList(list));
            view.getCountLabel().setText(list.size() + " physical copy item(s) tracked");
        });

        task.setOnFailed(e -> {
            view.getLoadingSpinner().setVisible(false);
            log.error("Failed to load inventory copies", task.getException());
        });

        AppContext.getInstance().getExecutorService().submit(task);
    }

    private void handleAddCopy() {
        List<Book> books = catalogService.getAllBooks(false);
        List<Shelf> shelves = inventoryService.getAllShelves();

        if (books.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Books in Catalog", "Please add at least one book title to the catalog before registering physical copies.");
            return;
        }

        CopyDialog dialog = new CopyDialog(null, books, shelves);
        Optional<BookCopy> result = dialog.showAndWait();

        result.ifPresent(newCopy -> {
            try {
                inventoryService.addCopy(newCopy);
                loadCopies();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Registration Error", ex.getMessage());
            }
        });
    }

    private void handleEditCopy(BookCopy copy) {
        if (!SessionManager.hasPermission(PermissionCode.COPY_MANAGE)) {
            showAlert(Alert.AlertType.WARNING, "Permission Denied", "You do not have permission to modify copies.");
            return;
        }

        List<Book> books = catalogService.getAllBooks(false);
        List<Shelf> shelves = inventoryService.getAllShelves();

        CopyDialog dialog = new CopyDialog(copy, books, shelves);
        Optional<BookCopy> result = dialog.showAndWait();

        result.ifPresent(updatedCopy -> {
            try {
                inventoryService.updateCopy(updatedCopy);
                loadCopies();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Update Error", ex.getMessage());
            }
        });
    }

    private void handleStatusChange(BookCopy copy) {
        if (!SessionManager.hasPermission(PermissionCode.COPY_MANAGE)) {
            showAlert(Alert.AlertType.WARNING, "Permission Denied", "You do not have permission to transition copy status.");
            return;
        }

        CopyStatusDialog dialog = new CopyStatusDialog(copy);
        Optional<CopyStatusDialog.StatusChangeResult> result = dialog.showAndWait();

        result.ifPresent(res -> {
            try {
                inventoryService.changeCopyStatus(copy.getId(), res.newStatus(), res.reason());
                loadCopies();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Transition Error", ex.getMessage());
            }
        });
    }

    private void handleViewHistory(BookCopy copy) {
        List<CopyStatusHistory> history = inventoryService.getCopyStatusHistory(copy.getId());
        StringBuilder sb = new StringBuilder();
        if (history.isEmpty()) {
            sb.append("No status transition history recorded for this copy.");
        } else {
            for (CopyStatusHistory h : history) {
                sb.append("• ").append(h.getChangedAt()).append(" | ")
                        .append(h.getOldStatus() != null ? h.getOldStatus() : "NONE").append(" ➔ ").append(h.getNewStatus())
                        .append(" | By: ").append(h.getChangedByName() != null ? h.getChangedByName() : "System")
                        .append(" | Reason: ").append(h.getReason() != null ? h.getReason() : "—")
                        .append("\n");
            }
        }
        showAlert(Alert.AlertType.INFORMATION, "Status History: " + copy.getAccessionNumber(), sb.toString());
    }

    private void handlePrintBarcode(BookCopy copy) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Barcode Label PDF");
        fc.setInitialFileName("Barcode_Label_" + copy.getBarcode() + ".pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));

        java.io.File file = fc.showSaveDialog(view.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    AppContext.getInstance().getPdfExportService().generateSpineLabelsPdf(List.of(copy), fos);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Spine label PDF saved to:\n" + file.getAbsolutePath()));
        task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Export Failed", task.getException().getMessage()));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("LibraryOS — " + title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

