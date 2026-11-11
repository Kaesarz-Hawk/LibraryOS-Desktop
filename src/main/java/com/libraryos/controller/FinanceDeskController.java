package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.model.finance.Fine;
import com.libraryos.model.finance.FineStatus;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.member.Member;
import com.libraryos.service.FineService;
import com.libraryos.service.MemberService;
import com.libraryos.service.PaymentService;
import com.libraryos.view.CollectPaymentDialog;
import com.libraryos.view.CreateFineDialog;
import com.libraryos.view.FinanceDeskView;
import com.libraryos.view.WaiveFineDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class FinanceDeskController {

    private static final Logger log = LoggerFactory.getLogger(FinanceDeskController.class);

    private final FinanceDeskView view;
    private final FineService fineService;
    private final PaymentService paymentService;
    private final MemberService memberService;

    // Current state
    private Member currentMember;
    private BigDecimal currentUnpaidBalance = BigDecimal.ZERO;
    private List<Fine> currentUnpaidFines = List.of();

    public FinanceDeskController(FinanceDeskView view) {
        this.view = view;
        this.fineService = AppContext.getInstance().getFineService();
        this.paymentService = AppContext.getInstance().getPaymentService();
        this.memberService = AppContext.getInstance().getMemberService();

        initialize();
    }

    private void initialize() {
        // Permissions
        boolean canCollect = SessionManager.hasPermission("FINE_COLLECT");
        boolean canWaive = SessionManager.hasPermission("FINE_WAIVE");
        boolean canManage = SessionManager.hasPermission("FINE_MANAGE");

        view.getCollectPaymentBtn().setDisable(true);
        view.getWaiveFineBtn().setDisable(true);
        view.getAssessFineBtn().setDisable(!canManage && !canCollect);

        // Header
        view.getRefreshBtn().setOnAction(e -> handleRefreshCurrentTab());

        // Tab 1: Member Collection Wiring
        view.getMemberLookupBtn().setOnAction(e -> handleMemberLookup());
        view.getMemberSearchField().setOnAction(e -> handleMemberLookup());

        view.getCollectPaymentBtn().setOnAction(e -> handleCollectPayment());
        view.getWaiveFineBtn().setOnAction(e -> handleWaiveFine());
        view.getAssessFineBtn().setOnAction(e -> handleAssessFine());

        view.getMemberFinesTable().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            boolean hasSelection = selected != null && selected.getUnpaidBalance().compareTo(BigDecimal.ZERO) > 0;
            view.getWaiveFineBtn().setDisable(!hasSelection || !canWaive);
        });

        // Tab 2: Payments History Wiring
        view.getPaymentsSearchBtn().setOnAction(e -> loadPayments());
        view.getPaymentsSearchField().setOnAction(e -> loadPayments());
        view.getPaymentsResetBtn().setOnAction(e -> {
            view.getPaymentsSearchField().clear();
            loadPayments();
        });
        setupPaymentsContextMenu();

        // Tab 3: All Fines Ledger Wiring
        view.getAllFinesSearchBtn().setOnAction(e -> loadAllFines());
        view.getAllFinesSearchField().setOnAction(e -> loadAllFines());
        view.getAllFinesStatusFilter().valueProperty().addListener((obs, o, n) -> loadAllFines());
        view.getAllFinesResetBtn().setOnAction(e -> {
            view.getAllFinesSearchField().clear();
            view.getAllFinesStatusFilter().setValue("ALL");
            loadAllFines();
        });

        // Tab change listener to auto-load tab content if empty
        view.getTabPane().getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == view.getPaymentsTab() && view.getPaymentsTable().getItems().isEmpty()) {
                loadPayments();
            } else if (newTab == view.getAllFinesTab() && view.getAllFinesTable().getItems().isEmpty()) {
                loadAllFines();
            }
        });
    }

    private void handleRefreshCurrentTab() {
        Tab current = view.getTabPane().getSelectionModel().getSelectedItem();
        if (current == view.getMemberTab()) {
            if (currentMember != null) {
                loadMemberFines(currentMember);
            }
        } else if (current == view.getPaymentsTab()) {
            loadPayments();
        } else if (current == view.getAllFinesTab()) {
            loadAllFines();
        }
    }

    private void handleMemberLookup() {
        String query = view.getMemberSearchField().getText() != null ? view.getMemberSearchField().getText().trim() : "";
        if (query.isBlank()) {
            showError("Lookup Error", "Please enter a membership number or member name.");
            return;
        }

        Task<List<Member>> task = new Task<>() {
            @Override
            protected List<Member> call() {
                return memberService.searchMembers(query);
            }
        };

        task.setOnSucceeded(e -> {
            List<Member> results = task.getValue();
            if (results.isEmpty()) {
                clearMemberSelection();
                showError("Member Not Found", "No library member matches query: '" + query + "'");
            } else {
                Member selected = results.get(0);
                loadMemberFines(selected);
            }
        });

        task.setOnFailed(e -> {
            log.error("Member lookup failed", task.getException());
            showError("Lookup Error", task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    public void loadMemberFines(Member member) {
        this.currentMember = member;
        view.getMemberNameLabel().setText(member.getDisplayName());
        view.getMemberCardLabel().setText(member.getMembershipNumber());
        view.getMemberContactLabel().setText((member.getPhone() != null ? member.getPhone() : "") +
                (member.getEmail() != null ? " / " + member.getEmail() : ""));
        view.getMemberStatusBadge().setText(member.getStatus().name());

        Task<MemberFinanceData> task = new Task<>() {
            @Override
            protected MemberFinanceData call() {
                BigDecimal balance = fineService.getTotalUnpaidFines(member.getId());
                List<Fine> unpaid = fineService.getUnpaidFinesByMember(member.getId());
                return new MemberFinanceData(balance, unpaid);
            }
        };

        task.setOnSucceeded(e -> {
            MemberFinanceData data = task.getValue();
            currentUnpaidBalance = data.balance;
            currentUnpaidFines = data.unpaidFines;

            view.getTotalUnpaidBalanceLabel().setText("৳ " + data.balance.toPlainString());
            view.getUnpaidCountLabel().setText(data.unpaidFines.size() + " unpaid fine(s)");
            view.getMemberFinesTable().setItems(FXCollections.observableArrayList(data.unpaidFines));
            view.getMemberFinesCountLabel().setText("Showing " + data.unpaidFines.size() + " unpaid fine(s) for member");

            boolean canCollect = SessionManager.hasPermission("FINE_COLLECT");
            view.getCollectPaymentBtn().setDisable(data.balance.compareTo(BigDecimal.ZERO) <= 0 || !canCollect);
            view.getWaiveFineBtn().setDisable(true);
        });

        task.setOnFailed(e -> {
            log.error("Failed to load member fines", task.getException());
            showError("Finance Load Error", task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void clearMemberSelection() {
        this.currentMember = null;
        this.currentUnpaidBalance = BigDecimal.ZERO;
        this.currentUnpaidFines = List.of();

        view.getMemberNameLabel().setText("No member loaded");
        view.getMemberCardLabel().setText("—");
        view.getMemberContactLabel().setText("—");
        view.getMemberStatusBadge().setText("IDLE");
        view.getTotalUnpaidBalanceLabel().setText("৳ 0.00");
        view.getUnpaidCountLabel().setText("0 Unpaid fines");
        view.getMemberFinesTable().getItems().clear();
        view.getMemberFinesCountLabel().setText("No fines for member");
        view.getCollectPaymentBtn().setDisable(true);
        view.getWaiveFineBtn().setDisable(true);
    }

    private void handleCollectPayment() {
        if (currentMember == null || currentUnpaidBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        CollectPaymentDialog dialog = new CollectPaymentDialog(currentMember, currentUnpaidBalance, currentUnpaidFines);
        Optional<CollectPaymentDialog.PaymentFormData> result = dialog.showAndWait();

        result.ifPresent(formData -> {
            Task<Payment> task = new Task<>() {
                @Override
                protected Payment call() {
                    return paymentService.recordPayment(
                            currentMember.getId(),
                            formData.getAmount(),
                            formData.getPaymentMethod(),
                            formData.getTransactionReference(),
                            formData.getNotes()
                    );
                }
            };

            task.setOnSucceeded(e -> {
                Payment payment = task.getValue();
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Payment Recorded");
                successAlert.setHeaderText("Receipt #" + payment.getReceiptNumber() + " Generated");
                successAlert.setContentText("Successfully collected ৳ " + payment.getAmount().toPlainString() +
                        " via " + payment.getPaymentMethod() +
                        (payment.getTransactionReference() != null ? "\nTrxID: " + payment.getTransactionReference() : "") +
                        "\nAmount was allocated to outstanding fines in FIFO order.");
                successAlert.showAndWait();

                loadMemberFines(currentMember);
                if (!view.getPaymentsTable().getItems().isEmpty()) {
                    loadPayments();
                }
            });

            task.setOnFailed(e -> {
                log.error("Failed to collect payment", task.getException());
                showError("Payment Failed", task.getException().getMessage());
            });

            AppContext.getInstance().getExecutorService().execute(task);
        });
    }

    private void handleWaiveFine() {
        Fine selectedFine = view.getMemberFinesTable().getSelectionModel().getSelectedItem();
        if (selectedFine == null) {
            showError("Selection Required", "Please select an unpaid fine row to waive.");
            return;
        }

        WaiveFineDialog dialog = new WaiveFineDialog(selectedFine);
        Optional<WaiveFineDialog.WaiverFormData> result = dialog.showAndWait();

        result.ifPresent(formData -> {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    fineService.waiveFine(formData.getFineId(), formData.getWaiverAmount(), formData.getReason());
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Fine Waived");
                info.setHeaderText("Waiver of ৳ " + formData.getWaiverAmount().toPlainString() + " Approved");
                info.setContentText("The waiver was permanently logged with your user credentials in the institutional audit log.");
                info.showAndWait();

                if (currentMember != null) {
                    loadMemberFines(currentMember);
                }
                if (!view.getAllFinesTable().getItems().isEmpty()) {
                    loadAllFines();
                }
            });

            task.setOnFailed(e -> {
                log.error("Failed to waive fine", task.getException());
                showError("Waiver Failed", task.getException().getMessage());
            });

            AppContext.getInstance().getExecutorService().execute(task);
        });
    }

    private void handleAssessFine() {
        Task<List<Member>> loadMembersTask = new Task<>() {
            @Override
            protected List<Member> call() {
                return memberService.searchMembers("");
            }
        };

        loadMembersTask.setOnSucceeded(e -> {
            List<Member> members = loadMembersTask.getValue();
            CreateFineDialog dialog = new CreateFineDialog(members, currentMember);
            Optional<CreateFineDialog.CreateFineFormData> result = dialog.showAndWait();

            result.ifPresent(formData -> {
                Task<Fine> task = new Task<>() {
                    @Override
                    protected Fine call() {
                        return fineService.createFine(
                                formData.getMemberId(),
                                null,
                                formData.getFineType(),
                                formData.getAmount(),
                                formData.getReason()
                        );
                    }
                };

                task.setOnSucceeded(se -> {
                    Fine created = task.getValue();
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Fine Assessed");
                    info.setHeaderText("Fine #" + created.getId() + " Created");
                    info.setContentText("Assessed ৳ " + created.getAmount().toPlainString() + " (" + created.getFineType() + ")");
                    info.showAndWait();

                    if (currentMember != null && currentMember.getId().equals(formData.getMemberId())) {
                        loadMemberFines(currentMember);
                    }
                    if (!view.getAllFinesTable().getItems().isEmpty()) {
                        loadAllFines();
                    }
                });

                task.setOnFailed(fe -> {
                    log.error("Failed to assess fine", task.getException());
                    showError("Assessment Failed", task.getException().getMessage());
                });

                AppContext.getInstance().getExecutorService().execute(task);
            });
        });

        loadMembersTask.setOnFailed(e -> {
            log.error("Failed to load members for assessment dialog", loadMembersTask.getException());
            showError("Load Error", "Unable to load members list: " + loadMembersTask.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(loadMembersTask);
    }

    public void loadPayments() {
        String query = view.getPaymentsSearchField().getText() != null ? view.getPaymentsSearchField().getText().trim() : "";

        Task<List<Payment>> task = new Task<>() {
            @Override
            protected List<Payment> call() {
                return paymentService.searchPayments(query, null, null);
            }
        };

        task.setOnSucceeded(e -> {
            List<Payment> results = task.getValue();
            view.getPaymentsTable().setItems(FXCollections.observableArrayList(results));
            view.getPaymentsCountLabel().setText("Showing " + results.size() + " payment record(s)");
        });

        task.setOnFailed(e -> {
            log.error("Failed to search payments", task.getException());
            showError("Load Error", "Unable to load payments: " + task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void setupPaymentsContextMenu() {
        view.getPaymentsTable().setRowFactory(tv -> {
            TableRow<Payment> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();

            MenuItem copyReceiptItem = new MenuItem("Copy Receipt # / রসিদ কপি");
            copyReceiptItem.setOnAction(e -> {
                Payment p = row.getItem();
                if (p != null) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(p.getReceiptNumber());
                    Clipboard.getSystemClipboard().setContent(content);
                }
            });

            MenuItem printReceiptItem = new MenuItem("Print Receipt Voucher (PDF) / রসিদ প্রিন্ট");
            printReceiptItem.setOnAction(e -> {
                Payment p = row.getItem();
                if (p != null) {
                    handlePrintReceipt(p);
                }
            });

            MenuItem reverseItem = new MenuItem("Reverse Payment / রসিদ বাতিল");
            reverseItem.setOnAction(e -> {
                Payment p = row.getItem();
                if (p != null) {
                    handleReversePayment(p);
                }
            });

            cm.getItems().addAll(copyReceiptItem, printReceiptItem, reverseItem);

            row.itemProperty().addListener((obs, oldVal, p) -> {
                if (p != null) {
                    boolean canReverse = SessionManager.hasPermission("FINE_WAIVE") && !p.isReversed();
                    reverseItem.setVisible(canReverse);
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

    private void handleReversePayment(Payment payment) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reverse Payment");
        dialog.setHeaderText("Reverse Receipt #" + payment.getReceiptNumber() + " (৳ " + payment.getAmount() + ")?");
        dialog.setContentText("Mandatory reversal justification / কারণ:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            if (reason.trim().length() < 5) {
                showError("Validation Error", "A valid justification of at least 5 characters is required.");
                return;
            }

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    paymentService.reversePayment(payment.getId(), reason.trim());
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Payment Reversed");
                info.setHeaderText("Receipt #" + payment.getReceiptNumber() + " Reversed");
                info.setContentText("The allocated fines have had their unpaid balances restored, and the reversal was permanently logged in the audit trail.");
                info.showAndWait();

                loadPayments();
                if (currentMember != null && currentMember.getId().equals(payment.getMemberId())) {
                    loadMemberFines(currentMember);
                }
                if (!view.getAllFinesTable().getItems().isEmpty()) {
                    loadAllFines();
                }
            });

            task.setOnFailed(e -> {
                log.error("Failed to reverse payment", task.getException());
                showError("Reversal Failed", task.getException().getMessage());
            });

            AppContext.getInstance().getExecutorService().execute(task);
        });
    }

    private void handlePrintReceipt(Payment payment) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Receipt Voucher PDF");
        fc.setInitialFileName("Receipt_" + payment.getReceiptNumber() + ".pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));

        java.io.File file = fc.showSaveDialog(view.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    AppContext.getInstance().getPdfExportService().generatePaymentReceiptPdf(payment, payment.getAllocations(), fos);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Receipt Saved");
            alert.setHeaderText("Receipt Voucher Generated");
            alert.setContentText("Payment receipt PDF saved to:\n" + file.getAbsolutePath());
            alert.showAndWait();
        });
        task.setOnFailed(e -> showError("Receipt Export Failed", task.getException().getMessage()));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    public void loadAllFines() {
        String query = view.getAllFinesSearchField().getText() != null ? view.getAllFinesSearchField().getText().trim() : "";
        String statusStr = view.getAllFinesStatusFilter().getValue();
        FineStatus status = (statusStr == null || "ALL".equals(statusStr)) ? null : FineStatus.valueOf(statusStr);

        Task<List<Fine>> task = new Task<>() {
            @Override
            protected List<Fine> call() {
                return fineService.searchFines(query, status);
            }
        };

        task.setOnSucceeded(e -> {
            List<Fine> results = task.getValue();
            view.getAllFinesTable().setItems(FXCollections.observableArrayList(results));
            view.getAllFinesCountLabel().setText("Showing " + results.size() + " fine record(s)");
        });

        task.setOnFailed(e -> {
            log.error("Failed to search all fines", task.getException());
            showError("Load Error", "Unable to load fines: " + task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class MemberFinanceData {
        final BigDecimal balance;
        final List<Fine> unpaidFines;

        MemberFinanceData(BigDecimal balance, List<Fine> unpaidFines) {
            this.balance = balance;
            this.unpaidFines = unpaidFines;
        }
    }
}

