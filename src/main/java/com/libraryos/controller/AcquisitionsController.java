package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.model.acquisition.Acquisition;
import com.libraryos.model.acquisition.AcquisitionItem;
import com.libraryos.model.acquisition.AcquisitionStatus;
import com.libraryos.model.acquisition.Supplier;
import com.libraryos.model.catalog.Book;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.Shelf;
import com.libraryos.service.AcquisitionService;
import com.libraryos.service.CatalogService;
import com.libraryos.service.InventoryService;
import com.libraryos.view.AcquisitionsView;
import com.libraryos.view.CreateAcquisitionDialog;
import com.libraryos.view.ReceiveAcquisitionDialog;
import com.libraryos.view.SupplierDialog;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AcquisitionsController {

    private static final Logger log = LoggerFactory.getLogger(AcquisitionsController.class);

    private final AcquisitionsView view;
    private final AcquisitionService acquisitionService;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;

    public AcquisitionsController(AcquisitionsView view) {
        this.view = view;
        this.acquisitionService = AppContext.getInstance().getAcquisitionService();
        this.catalogService = AppContext.getInstance().getCatalogService();
        this.inventoryService = AppContext.getInstance().getInventoryService();
        initialize();
    }

    private void initialize() {
        // Tab 1: Orders wiring
        view.getRefreshOrdersBtn().setOnAction(e -> loadOrders());
        view.getStatusFilterCombo().setOnAction(e -> loadOrders());
        view.getNewOrderBtn().setOnAction(e -> handleNewOrder());

        view.getOrdersTable().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                view.getOrderItemsTable().getItems().setAll(newVal.getItems());
                boolean isOrdered = newVal.getStatus() == AcquisitionStatus.ORDERED;
                view.getReceiveOrderBtn().setDisable(!isOrdered);
                view.getCancelOrderBtn().setDisable(!isOrdered);
            } else {
                view.getOrderItemsTable().getItems().clear();
                view.getReceiveOrderBtn().setDisable(true);
                view.getCancelOrderBtn().setDisable(true);
            }
        });

        view.getReceiveOrderBtn().setOnAction(e -> {
            Acquisition selected = view.getOrdersTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleReceiveOrder(selected);
            }
        });

        view.getCancelOrderBtn().setOnAction(e -> {
            Acquisition selected = view.getOrdersTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleCancelOrder(selected);
            }
        });

        // Tab 2: Suppliers wiring
        view.getSupplierSearchBtn().setOnAction(e -> loadSuppliers());
        view.getSupplierSearchField().setOnAction(e -> loadSuppliers());
        view.getRefreshSuppliersBtn().setOnAction(e -> {
            view.getSupplierSearchField().clear();
            loadSuppliers();
        });

        view.getAddSupplierBtn().setOnAction(e -> handleAddSupplier());

        view.getSuppliersTable().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            view.getEditSupplierBtn().setDisable(newVal == null);
            view.getDeleteSupplierBtn().setDisable(newVal == null);
        });

        view.getEditSupplierBtn().setOnAction(e -> {
            Supplier selected = view.getSuppliersTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleEditSupplier(selected);
            }
        });

        view.getDeleteSupplierBtn().setOnAction(e -> {
            Supplier selected = view.getSuppliersTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleDeleteSupplier(selected);
            }
        });

        loadOrders();
        loadSuppliers();
    }

    public void loadOrders() {
        String filter = view.getStatusFilterCombo().getValue();
        view.getOrdersStatusLabel().setText("Loading orders...");

        Task<List<Acquisition>> task = new Task<>() {
            @Override
            protected List<Acquisition> call() {
                if (filter != null && filter.startsWith("ORDERED")) {
                    return acquisitionService.getAcquisitionsByStatus(AcquisitionStatus.ORDERED);
                } else if (filter != null && filter.startsWith("RECEIVED")) {
                    return acquisitionService.getAcquisitionsByStatus(AcquisitionStatus.RECEIVED);
                } else if (filter != null && filter.startsWith("CANCELLED")) {
                    return acquisitionService.getAcquisitionsByStatus(AcquisitionStatus.CANCELLED);
                } else {
                    return acquisitionService.getAllAcquisitions();
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<Acquisition> list = task.getValue();
            view.getOrdersTable().getItems().setAll(list);
            view.getOrdersStatusLabel().setText("Total: " + list.size() + " acquisition order(s).");
        });

        task.setOnFailed(e -> {
            log.error("Failed to load acquisitions", task.getException());
            view.getOrdersStatusLabel().setText("Failed to load orders.");
        });

        AppContext.getInstance().getExecutorService().submit(task);
    }

    public void loadSuppliers() {
        String query = view.getSupplierSearchField().getText();
        view.getSuppliersStatusLabel().setText("Loading suppliers...");

        Task<List<Supplier>> task = new Task<>() {
            @Override
            protected List<Supplier> call() {
                if (query == null || query.isBlank()) {
                    return acquisitionService.getAllSuppliers();
                } else {
                    return acquisitionService.searchSuppliers(query);
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<Supplier> list = task.getValue();
            view.getSuppliersTable().getItems().setAll(list);
            view.getSuppliersStatusLabel().setText("Total: " + list.size() + " supplier(s) registered.");
        });

        task.setOnFailed(e -> {
            log.error("Failed to load suppliers", task.getException());
            view.getSuppliersStatusLabel().setText("Failed to load suppliers.");
        });

        AppContext.getInstance().getExecutorService().submit(task);
    }

    private void handleNewOrder() {
        Task<Void> prepTask = new Task<>() {
            List<Supplier> suppliers;
            List<Book> books;

            @Override
            protected Void call() {
                suppliers = acquisitionService.getAllSuppliers();
                books = catalogService.getAllBooks();
                return null;
            }

            @Override
            protected void succeeded() {
                CreateAcquisitionDialog dialog = new CreateAcquisitionDialog(view.getScene().getWindow(), suppliers, books);
                dialog.showAndWait().ifPresent(result -> {
                    view.getOrdersStatusLabel().setText("Creating acquisition order...");

                    Task<Acquisition> createTask = new Task<>() {
                        @Override
                        protected Acquisition call() {
                            return acquisitionService.createAcquisitionOrder(
                                    result.supplierId(),
                                    result.invoiceNumber(),
                                    result.orderDate(),
                                    result.notes(),
                                    result.items()
                            );
                        }
                    };

                    createTask.setOnSucceeded(ev -> {
                        showAlert(Alert.AlertType.INFORMATION, "Order Created",
                                "Acquisition Order #" + createTask.getValue().getAcquisitionNumber() + " successfully created.");
                        loadOrders();
                    });

                    createTask.setOnFailed(ev -> {
                        log.error("Failed to create acquisition order", createTask.getException());
                        showAlert(Alert.AlertType.ERROR, "Creation Failed", createTask.getException().getMessage());
                    });

                    AppContext.getInstance().getExecutorService().submit(createTask);
                });
            }
        };

        AppContext.getInstance().getExecutorService().submit(prepTask);
    }

    private void handleReceiveOrder(Acquisition order) {
        Task<List<Shelf>> shelfTask = new Task<>() {
            @Override
            protected List<Shelf> call() {
                return inventoryService.getAllShelves();
            }
        };

        shelfTask.setOnSucceeded(e -> {
            List<Shelf> shelves = shelfTask.getValue();
            ReceiveAcquisitionDialog dialog = new ReceiveAcquisitionDialog(view.getScene().getWindow(), order, shelves);
            dialog.showAndWait().ifPresent(shelfId -> {
                view.getOrdersStatusLabel().setText("Receiving shipment & generating physical copies...");

                Task<List<BookCopy>> receiveTask = new Task<>() {
                    @Override
                    protected List<BookCopy> call() {
                        return acquisitionService.receiveAcquisitionBatch(order.getId(), shelfId);
                    }
                };

                receiveTask.setOnSucceeded(ev -> {
                    List<BookCopy> generated = receiveTask.getValue();
                    showAlert(Alert.AlertType.INFORMATION, "Shipment Received & Inventory Generated",
                            "Order #" + order.getAcquisitionNumber() + " successfully received into inventory.\n\n" +
                                    "Generated " + generated.size() + " physical copies with unique Accession numbers and Code-128 barcodes.");
                    loadOrders();
                });

                receiveTask.setOnFailed(ev -> {
                    log.error("Failed to receive acquisition order", receiveTask.getException());
                    showAlert(Alert.AlertType.ERROR, "Receiving Failed", receiveTask.getException().getMessage());
                });

                AppContext.getInstance().getExecutorService().submit(receiveTask);
            });
        });

        AppContext.getInstance().getExecutorService().submit(shelfTask);
    }

    private void handleCancelOrder(Acquisition order) {
        TextInputDialog dialog = new TextInputDialog("Supplier cancelled / out of stock");
        dialog.setTitle("Cancel Acquisition Order");
        dialog.setHeaderText("Cancel order #" + order.getAcquisitionNumber());
        dialog.setContentText("Reason for cancellation / বাতিলের কারণ:");

        dialog.showAndWait().ifPresent(reason -> {
            Task<Boolean> cancelTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return acquisitionService.cancelAcquisitionOrder(order.getId(), reason);
                }
            };

            cancelTask.setOnSucceeded(e -> {
                showAlert(Alert.AlertType.INFORMATION, "Order Cancelled", "Acquisition order #" + order.getAcquisitionNumber() + " was cancelled.");
                loadOrders();
            });

            cancelTask.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Cancel Failed", cancelTask.getException().getMessage()));

            AppContext.getInstance().getExecutorService().submit(cancelTask);
        });
    }

    private void handleAddSupplier() {
        SupplierDialog dialog = new SupplierDialog(view.getScene().getWindow(), null);
        dialog.showAndWait().ifPresent(supplier -> {
            Task<Supplier> task = new Task<>() {
                @Override
                protected Supplier call() {
                    return acquisitionService.createSupplier(supplier);
                }
            };
            task.setOnSucceeded(e -> {
                showAlert(Alert.AlertType.INFORMATION, "Supplier Registered", "Supplier '" + supplier.getName() + "' saved successfully.");
                loadSuppliers();
            });
            task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Registration Failed", task.getException().getMessage()));
            AppContext.getInstance().getExecutorService().submit(task);
        });
    }

    private void handleEditSupplier(Supplier existing) {
        SupplierDialog dialog = new SupplierDialog(view.getScene().getWindow(), existing);
        dialog.showAndWait().ifPresent(updated -> {
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    return acquisitionService.updateSupplier(updated);
                }
            };
            task.setOnSucceeded(e -> {
                showAlert(Alert.AlertType.INFORMATION, "Supplier Updated", "Supplier profile updated.");
                loadSuppliers();
            });
            task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Update Failed", task.getException().getMessage()));
            AppContext.getInstance().getExecutorService().submit(task);
        });
    }

    private void handleDeleteSupplier(Supplier supplier) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Deactivate supplier '" + supplier.getName() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Deactivation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return acquisitionService.deactivateSupplier(supplier.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    showAlert(Alert.AlertType.INFORMATION, "Deactivated", "Supplier marked as inactive.");
                    loadSuppliers();
                });
                task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Error", task.getException().getMessage()));
                AppContext.getInstance().getExecutorService().submit(task);
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

