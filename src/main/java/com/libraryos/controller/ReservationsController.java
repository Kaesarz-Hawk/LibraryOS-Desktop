package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.security.SessionManager;
import com.libraryos.model.catalog.Book;
import com.libraryos.model.member.Member;
import com.libraryos.model.reservation.Reservation;
import com.libraryos.model.reservation.ReservationStatus;
import com.libraryos.service.CatalogService;
import com.libraryos.service.MemberService;
import com.libraryos.service.ReservationService;
import com.libraryos.view.PlaceReservationDialog;
import com.libraryos.view.ReservationsView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ReservationsController {

    private static final Logger log = LoggerFactory.getLogger(ReservationsController.class);

    private final ReservationsView view;
    private final ReservationService reservationService;
    private final CatalogService catalogService;
    private final MemberService memberService;

    public ReservationsController(ReservationsView view) {
        this.view = view;
        this.reservationService = AppContext.getInstance().getReservationService();
        this.catalogService = AppContext.getInstance().getCatalogService();
        this.memberService = AppContext.getInstance().getMemberService();

        initialize();
    }

    private void initialize() {
        boolean canManage = SessionManager.hasPermission("RESERVATION_MANAGE");
        view.getProcessExpiredBtn().setDisable(!canManage);

        view.getPlaceReservationBtn().setOnAction(e -> handlePlaceReservation());
        view.getProcessExpiredBtn().setOnAction(e -> handleProcessExpiredHolds());
        view.getRefreshBtn().setOnAction(e -> loadReservations());

        view.getSearchBtn().setOnAction(e -> loadReservations());
        view.getSearchField().setOnAction(e -> loadReservations());
        view.getStatusFilter().valueProperty().addListener((obs, o, n) -> loadReservations());
        view.getResetBtn().setOnAction(e -> {
            view.getSearchField().clear();
            view.getStatusFilter().setValue("ALL");
            loadReservations();
        });

        setupTableContextMenu();
        loadReservations();
    }

    private void setupTableContextMenu() {
        view.getReservationsTable().setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();

            MenuItem cancelItem = new MenuItem("Cancel Hold / বাতিল করুন");
            cancelItem.setOnAction(e -> {
                Reservation res = row.getItem();
                if (res != null) {
                    handleCancelReservation(res);
                }
            });

            MenuItem copyBarcodeItem = new MenuItem("Copy Barcode / বারকোড কপি");
            copyBarcodeItem.setOnAction(e -> {
                Reservation res = row.getItem();
                if (res != null && res.getAllocatedBarcode() != null) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(res.getAllocatedBarcode());
                    Clipboard.getSystemClipboard().setContent(content);
                }
            });

            cm.getItems().addAll(cancelItem, copyBarcodeItem);

            row.itemProperty().addListener((obs, oldVal, res) -> {
                if (res != null) {
                    boolean cancellable = res.getStatus() == ReservationStatus.WAITING || res.getStatus() == ReservationStatus.READY_FOR_PICKUP;
                    cancelItem.setVisible(cancellable);
                    copyBarcodeItem.setVisible(res.getAllocatedBarcode() != null);
                }
            });

            row.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) {
                    row.setContextMenu(null);
                } else {
                    row.setContextMenu(cm);
                }
            });

            return row;
        });
    }

    public void loadReservations() {
        String query = view.getSearchField().getText() != null ? view.getSearchField().getText().trim() : "";
        String statusStr = view.getStatusFilter().getValue();
        ReservationStatus status = (statusStr == null || "ALL".equals(statusStr)) ? null : ReservationStatus.valueOf(statusStr);

        Task<List<Reservation>> task = new Task<>() {
            @Override
            protected List<Reservation> call() {
                return reservationService.searchReservations(query, status);
            }
        };

        task.setOnSucceeded(e -> {
            List<Reservation> results = task.getValue();
            view.getReservationsTable().setItems(FXCollections.observableArrayList(results));
            view.getCountLabel().setText("Showing " + results.size() + " reservation(s)");
        });

        task.setOnFailed(e -> {
            log.error("Failed to load reservations", task.getException());
            showError("Load Error", "Unable to load reservation queue: " + task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void handlePlaceReservation() {
        // Load active books and active members in background, then open dialog
        Task<DialogData> loadTask = new Task<>() {
            @Override
            protected DialogData call() {
                List<Book> books = catalogService.searchBooks("");
                List<Member> members = memberService.searchMembers("");
                return new DialogData(books, members);
            }
        };

        loadTask.setOnSucceeded(e -> {
            DialogData data = loadTask.getValue();
            PlaceReservationDialog dialog = new PlaceReservationDialog(data.books, data.members);
            Optional<PlaceReservationDialog.ReservationFormData> result = dialog.showAndWait();

            result.ifPresent(formData -> {
                Task<Reservation> saveTask = new Task<>() {
                    @Override
                    protected Reservation call() {
                        return reservationService.placeReservation(
                                formData.getBookId(),
                                formData.getMemberId(),
                                formData.getNotes()
                        );
                    }
                };

                saveTask.setOnSucceeded(se -> {
                    Reservation created = saveTask.getValue();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Reservation Placed");
                    alert.setHeaderText("Reservation #" + created.getId() + " enqueued successfully");
                    alert.setContentText("Status: " + created.getStatus() +
                            (created.getAllocatedBarcode() != null ? "\nAllocated Copy: " + created.getAllocatedBarcode() : "\nPosition in queue: Waiting"));
                    alert.showAndWait();
                    loadReservations();
                });

                saveTask.setOnFailed(fe -> {
                    log.error("Failed to place reservation", saveTask.getException());
                    showError("Reservation Error", saveTask.getException().getMessage());
                });

                AppContext.getInstance().getExecutorService().execute(saveTask);
            });
        });

        loadTask.setOnFailed(e -> {
            log.error("Failed to prepare reservation dialog", loadTask.getException());
            showError("Load Error", "Could not load books or members: " + loadTask.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(loadTask);
    }

    private void handleCancelReservation(Reservation reservation) {
        TextInputDialog dialog = new TextInputDialog("Cancelled by library staff");
        dialog.setTitle("Cancel Reservation");
        dialog.setHeaderText("Cancel Hold #" + reservation.getId() + " for " + reservation.getBookTitle());
        dialog.setContentText("Cancellation reason:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            Task<Void> cancelTask = new Task<>() {
                @Override
                protected Void call() {
                    reservationService.cancelReservation(reservation.getId(), reason);
                    return null;
                }
            };

            cancelTask.setOnSucceeded(e -> {
                loadReservations();
            });

            cancelTask.setOnFailed(e -> {
                log.error("Failed to cancel reservation", cancelTask.getException());
                showError("Cancel Error", cancelTask.getException().getMessage());
            });

            AppContext.getInstance().getExecutorService().execute(cancelTask);
        });
    }

    private void handleProcessExpiredHolds() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Process Expired Holds");
        confirm.setHeaderText("Reallocate or release expired ready-for-pickup holds?");
        confirm.setContentText("Any reservation past its pickup expiration date will be marked EXPIRED, and its allocated copy will be automatically assigned to the next waiting member or returned to AVAILABLE inventory.");

        Optional<ButtonType> resp = confirm.showAndWait();
        if (resp.isPresent() && resp.get() == ButtonType.OK) {
            Task<Integer> task = new Task<>() {
                @Override
                protected Integer call() {
                    return reservationService.processExpiredHolds();
                }
            };

            task.setOnSucceeded(e -> {
                int count = task.getValue();
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Holds Processed");
                info.setHeaderText("Hold Expiration Processing Complete");
                info.setContentText(count > 0
                        ? "Processed " + count + " expired hold(s) and reallocated inventory."
                        : "No expired holds currently pending.");
                info.showAndWait();
                loadReservations();
            });

            task.setOnFailed(e -> {
                log.error("Failed to process expired holds", task.getException());
                showError("Processing Error", task.getException().getMessage());
            });

            AppContext.getInstance().getExecutorService().execute(task);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class DialogData {
        final List<Book> books;
        final List<Member> members;

        DialogData(List<Book> books, List<Member> members) {
            this.books = books;
            this.members = members;
        }
    }
}
