package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.circulation.Loan;
import com.libraryos.model.circulation.LoanStatus;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.ConditionStatus;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.model.member.BorrowingPolicy;
import com.libraryos.model.member.Member;
import com.libraryos.model.member.MemberStatus;
import com.libraryos.service.CirculationService;
import com.libraryos.service.FineCalculationService;
import com.libraryos.service.InventoryService;
import com.libraryos.service.MemberService;
import com.libraryos.view.BorrowingPolicyDialog;
import com.libraryos.view.CirculationDeskView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class CirculationDeskController {

    private static final Logger log = LoggerFactory.getLogger(CirculationDeskController.class);

    private final CirculationDeskView view;
    private final CirculationService circulationService;
    private final InventoryService inventoryService;
    private final MemberService memberService;

    // Issue Tab state
    private BookCopy currentIssueCopy;
    private Member currentIssueMember;

    // Return Tab state
    private Loan currentReturnLoan;

    public CirculationDeskController(CirculationDeskView view) {
        this.view = view;
        this.circulationService = AppContext.getInstance().getCirculationService();
        this.inventoryService = AppContext.getInstance().getInventoryService();
        this.memberService = AppContext.getInstance().getMemberService();

        initialize();
    }

    private void initialize() {
        // Permissions
        boolean canIssue = SessionManager.hasPermission("LOAN_ISSUE");
        boolean canReturn = SessionManager.hasPermission("LOAN_RETURN");
        boolean canManagePolicy = SessionManager.hasPermission("SETTINGS_MANAGE");

        view.getIssueConfirmButton().setDisable(!canIssue);
        view.getReturnConfirmButton().setDisable(!canReturn);
        view.getPolicyButton().setDisable(!canManagePolicy);

        // Header Actions
        view.getPolicyButton().setOnAction(e -> handleOpenPolicies());
        view.getRefreshButton().setOnAction(e -> loadActiveLoans());

        // Issue Tab Wiring
        view.getIssueLookupCopyBtn().setOnAction(e -> handleLookupIssueCopy());
        view.getIssueBarcodeField().setOnAction(e -> handleLookupIssueCopy());

        view.getIssueLookupMemberBtn().setOnAction(e -> handleLookupIssueMember());
        view.getIssueMemberField().setOnAction(e -> handleLookupIssueMember());

        view.getIssueConfirmButton().setOnAction(e -> handleConfirmIssue());

        // Return Tab Wiring
        view.getReturnLookupBtn().setOnAction(e -> handleLookupReturnLoan());
        view.getReturnBarcodeField().setOnAction(e -> handleLookupReturnLoan());

        view.getReturnConfirmButton().setOnAction(e -> handleConfirmReturn());

        // Active Loans Tab Wiring
        view.getLoansSearchBtn().setOnAction(e -> loadActiveLoans());
        view.getLoansSearchField().setOnAction(e -> loadActiveLoans());
        view.getLoansStatusFilter().valueProperty().addListener((obs, o, n) -> loadActiveLoans());
        view.getLoansOverdueOnlyCheck().selectedProperty().addListener((obs, o, n) -> loadActiveLoans());
        view.getLoansResetBtn().setOnAction(e -> {
            view.getLoansSearchField().clear();
            view.getLoansStatusFilter().setValue("ACTIVE");
            view.getLoansOverdueOnlyCheck().setSelected(false);
            loadActiveLoans();
        });

        // Table Context Menu
        setupTableContextMenu();

        // Initial Data Load
        loadActiveLoans();
    }

    // =========================================================================
    // Issue Tab Workflows
    // =========================================================================

    private void handleLookupIssueCopy() {
        String query = view.getIssueBarcodeField().getText().trim();
        if (query.isBlank()) {
            showIssueFeedback("Please enter or scan a book copy barcode", true);
            return;
        }

        Task<Optional<BookCopy>> task = new Task<>() {
            @Override
            protected Optional<BookCopy> call() {
                Optional<BookCopy> copy = inventoryService.getCopyByBarcode(query);
                if (copy.isEmpty()) {
                    copy = inventoryService.getCopyByAccessionNumber(query);
                }
                return copy;
            }
        };

        task.setOnSucceeded(e -> {
            Optional<BookCopy> opt = task.getValue();
            if (opt.isPresent()) {
                currentIssueCopy = opt.get();
                view.getIssueCopyTitleLabel().setText(currentIssueCopy.getBookTitle());
                view.getIssueCopyBarcodeLabel().setText(currentIssueCopy.getBarcode());
                view.getIssueCopyAccessionLabel().setText(currentIssueCopy.getAccessionNumber());
                view.getIssueCopyShelfLabel().setText(currentIssueCopy.getShelfCode() != null ? currentIssueCopy.getShelfCode() : "—");
                view.getIssueCopyConditionLabel().setText(currentIssueCopy.getConditionStatus().getDisplayName());

                view.getIssueCopyStatusBadge().setText(currentIssueCopy.getStatus().name());
                view.getIssueCopyStatusBadge().getStyleClass().removeAll("badge-success", "badge-error", "badge-warning", "badge-info");
                if (currentIssueCopy.getStatus() == CopyStatus.AVAILABLE) {
                    view.getIssueCopyStatusBadge().getStyleClass().add("badge-success");
                    showIssueFeedback("Book copy loaded: Ready to issue", false);
                } else if (currentIssueCopy.getStatus() == CopyStatus.RESERVED) {
                    view.getIssueCopyStatusBadge().getStyleClass().add("badge-warning");
                    showIssueFeedback("Copy is RESERVED for hold pickup", false);
                } else {
                    view.getIssueCopyStatusBadge().getStyleClass().add("badge-error");
                    showIssueFeedback("Warning: Copy status is " + currentIssueCopy.getStatus() + " (Cannot be issued)", true);
                }
                checkIssueReadiness();
            } else {
                currentIssueCopy = null;
                showIssueFeedback("No book copy found with barcode/accession: " + query, true);
                resetIssueCopyLabels();
                checkIssueReadiness();
            }
        });

        task.setOnFailed(e -> showIssueFeedback("Lookup error: " + task.getException().getMessage(), true));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void handleLookupIssueMember() {
        String query = view.getIssueMemberField().getText().trim();
        if (query.isBlank()) {
            showIssueFeedback("Please enter member card number, roll, or phone number", true);
            return;
        }

        Task<Optional<Member>> task = new Task<>() {
            @Override
            protected Optional<Member> call() {
                Optional<Member> member = memberService.getMemberByNumber(query);
                if (member.isEmpty()) {
                    member = memberService.getMemberByInstitutionalId(query);
                }
                if (member.isEmpty()) {
                    member = memberService.getMemberByPhone(query);
                }
                return member;
            }
        };

        task.setOnSucceeded(e -> {
            Optional<Member> opt = task.getValue();
            if (opt.isPresent()) {
                currentIssueMember = opt.get();
                view.getIssueMemberNameLabel().setText(currentIssueMember.getDisplayName());
                view.getIssueMemberCardLabel().setText(currentIssueMember.getMembershipNumber());
                view.getIssueMemberTypeLabel().setText(currentIssueMember.getMemberTypeName() != null ? currentIssueMember.getMemberTypeName() : "Tier #" + currentIssueMember.getMemberTypeId());

                int loans = currentIssueMember.getActiveLoansCount();
                view.getIssueMemberLoansLabel().setText(loans + " Active Loan(s)");
                view.getIssueMemberFinesLabel().setText("৳ " + (currentIssueMember.getUnpaidFinesBdt() != null ? currentIssueMember.getUnpaidFinesBdt().toPlainString() : "0.00"));

                view.getIssueMemberStatusBadge().setText(currentIssueMember.getStatus().name());
                view.getIssueMemberStatusBadge().getStyleClass().removeAll("badge-success", "badge-error", "badge-info");
                if (currentIssueMember.getStatus() == MemberStatus.ACTIVE) {
                    view.getIssueMemberStatusBadge().getStyleClass().add("badge-success");
                    showIssueFeedback("Member account verified: Active", false);
                } else {
                    view.getIssueMemberStatusBadge().getStyleClass().add("badge-error");
                    showIssueFeedback("Warning: Member account is " + currentIssueMember.getStatus(), true);
                }
                checkIssueReadiness();
            } else {
                currentIssueMember = null;
                showIssueFeedback("No member found matching: " + query, true);
                resetIssueMemberLabels();
                checkIssueReadiness();
            }
        });

        task.setOnFailed(e -> showIssueFeedback("Member search error: " + task.getException().getMessage(), true));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void checkIssueReadiness() {
        if (currentIssueCopy != null && currentIssueMember != null &&
                (currentIssueCopy.getStatus() == CopyStatus.AVAILABLE || currentIssueCopy.getStatus() == CopyStatus.RESERVED) &&
                currentIssueMember.getStatus() == MemberStatus.ACTIVE) {

            // Calculate estimated due date
            LocalDate estimatedDue = DateTimeUtil.adjustForWeeklyHoliday(LocalDate.now().plusDays(14));
            view.getIssueDueDatePreviewLabel().setText(DateTimeUtil.formatDate(estimatedDue) + " (14-day institutional loan)");
            view.getIssueConfirmButton().setDisable(false);
        } else {
            view.getIssueDueDatePreviewLabel().setText("Calculated upon copy & member lookup");
            view.getIssueConfirmButton().setDisable(true);
        }
    }

    private void handleConfirmIssue() {
        if (currentIssueCopy == null || currentIssueMember == null) return;

        view.getIssueConfirmButton().setDisable(true);

        Task<Loan> task = new Task<>() {
            @Override
            protected Loan call() {
                return circulationService.issueBook(currentIssueCopy.getId(), currentIssueMember.getId());
            }
        };

        task.setOnSucceeded(e -> {
            Loan loan = task.getValue();
            showIssueFeedback("SUCCESS: Book copy [" + loan.getCopyBarcode() + "] issued to " +
                    loan.getMemberName() + ". Due date: " + DateTimeUtil.formatDate(loan.getDueDate()), false);

            // Clear inputs for next checkout
            view.getIssueBarcodeField().clear();
            currentIssueCopy = null;
            resetIssueCopyLabels();
            checkIssueReadiness();

            // Refresh table and member counts
            loadActiveLoans();
            handleLookupIssueMember();
        });

        task.setOnFailed(e -> {
            view.getIssueConfirmButton().setDisable(false);
            Throwable ex = task.getException();
            showIssueFeedback("Issue failed: " + ex.getMessage(), true);
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void resetIssueCopyLabels() {
        view.getIssueCopyTitleLabel().setText("No copy scanned");
        view.getIssueCopyBarcodeLabel().setText("—");
        view.getIssueCopyAccessionLabel().setText("—");
        view.getIssueCopyShelfLabel().setText("—");
        view.getIssueCopyConditionLabel().setText("—");
        view.getIssueCopyStatusBadge().setText("IDLE");
        view.getIssueCopyStatusBadge().getStyleClass().removeAll("badge-success", "badge-error", "badge-warning", "badge-info");
        view.getIssueCopyStatusBadge().getStyleClass().add("badge-info");
    }

    private void resetIssueMemberLabels() {
        view.getIssueMemberNameLabel().setText("No member selected");
        view.getIssueMemberCardLabel().setText("—");
        view.getIssueMemberTypeLabel().setText("—");
        view.getIssueMemberLoansLabel().setText("0 / 3");
        view.getIssueMemberFinesLabel().setText("৳ 0.00");
        view.getIssueMemberStatusBadge().setText("IDLE");
        view.getIssueMemberStatusBadge().getStyleClass().removeAll("badge-success", "badge-error", "badge-info");
        view.getIssueMemberStatusBadge().getStyleClass().add("badge-info");
    }

    private void showIssueFeedback(String message, boolean isError) {
        view.getIssueFeedbackLabel().setText(message);
        view.getIssueFeedbackLabel().getStyleClass().removeAll("banner-error", "banner-info");
        view.getIssueFeedbackLabel().getStyleClass().add(isError ? "banner-error" : "banner-info");
        view.getIssueFeedbackLabel().setVisible(true);
        view.getIssueFeedbackLabel().setManaged(true);
    }

    // =========================================================================
    // Return Tab Workflows
    // =========================================================================

    private void handleLookupReturnLoan() {
        String barcode = view.getReturnBarcodeField().getText().trim();
        if (barcode.isBlank()) {
            showReturnFeedback("Please enter or scan a returned book barcode", true);
            return;
        }

        Task<Optional<Loan>> task = new Task<>() {
            @Override
            protected Optional<Loan> call() {
                Optional<BookCopy> copy = inventoryService.getCopyByBarcode(barcode);
                if (copy.isEmpty()) return Optional.empty();
                return Optional.ofNullable(circulationService.searchLoans(barcode, LoanStatus.ACTIVE, null).stream()
                        .findFirst().orElse(null));
            }
        };

        task.setOnSucceeded(e -> {
            Optional<Loan> opt = task.getValue();
            if (opt.isPresent()) {
                currentReturnLoan = opt.get();
                view.getReturnBookTitleLabel().setText(currentReturnLoan.getBookTitle());
                view.getReturnBarcodeLabel().setText(currentReturnLoan.getCopyBarcode());
                view.getReturnMemberLabel().setText(currentReturnLoan.getMemberName() + " (" + currentReturnLoan.getMembershipNumber() + ")");
                view.getReturnIssuedDateLabel().setText(DateTimeUtil.formatDate(currentReturnLoan.getIssuedAt() != null ? currentReturnLoan.getIssuedAt().toLocalDate() : null));
                view.getReturnDueDateLabel().setText(DateTimeUtil.formatDate(currentReturnLoan.getDueDate()));

                // Calculate live fine preview
                FineCalculationService.OverdueResult preview = circulationService.calculatePreviewFine(currentReturnLoan.getId(), LocalDate.now());
                view.getReturnOverdueDaysLabel().setText(preview.getTotalCalendarDaysPastDue() + " Days (" + preview.getChargeableDays() + " chargeable)");
                view.getDailyRateLabel().setText("৳ " + preview.getDailyRate() + " / day");
                view.getReturnCalculatedFineLabel().setText("৳ " + preview.getFineAmount().toPlainString());

                view.getReturnFineStatusBadge().getStyleClass().removeAll("badge-success", "badge-error");
                if (preview.isOverdue()) {
                    view.getReturnFineStatusBadge().setText("OVERDUE (" + preview.getChargeableDays() + "d chargeable)");
                    view.getReturnFineStatusBadge().getStyleClass().add("badge-error");
                    showReturnFeedback("Loan is overdue! Fine of ৳" + preview.getFineAmount() + " will be registered upon return.", true);
                } else {
                    view.getReturnFineStatusBadge().setText("ON TIME");
                    view.getReturnFineStatusBadge().getStyleClass().add("badge-success");
                    showReturnFeedback("Active loan loaded. Returned on time.", false);
                }

                view.getReturnConfirmButton().setDisable(false);
            } else {
                currentReturnLoan = null;
                showReturnFeedback("No ACTIVE loan found for barcode: " + barcode, true);
                resetReturnLabels();
                view.getReturnConfirmButton().setDisable(true);
            }
        });

        task.setOnFailed(e -> showReturnFeedback("Lookup error: " + task.getException().getMessage(), true));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void handleConfirmReturn() {
        if (currentReturnLoan == null) return;

        view.getReturnConfirmButton().setDisable(true);
        ConditionStatus condition = view.getReturnConditionCombo().getValue();
        String notes = view.getReturnNotesArea().getText();

        Task<CirculationService.ReturnResult> task = new Task<>() {
            @Override
            protected CirculationService.ReturnResult call() {
                return circulationService.returnBook(currentReturnLoan.getCopyId(), condition != null ? condition.name() : "GOOD", notes);
            }
        };

        task.setOnSucceeded(e -> {
            CirculationService.ReturnResult result = task.getValue();
            StringBuilder msg = new StringBuilder();
            msg.append("✓ Successfully returned [").append(currentReturnLoan.getCopyBarcode()).append("] from ")
                    .append(currentReturnLoan.getMemberName()).append(". ");

            if (result.getGeneratedFine() != null && result.getGeneratedFine().getOriginalAmountBdt().compareTo(BigDecimal.ZERO) > 0) {
                msg.append("Overdue fine of ৳").append(result.getGeneratedFine().getOriginalAmountBdt()).append(" recorded. ");
            }

            if (result.isCopyReservedForNextMember()) {
                msg.append("★ Copy has been placed on HOLD for the next queued reservation!");
            }

            showReturnFeedback(msg.toString(), false);

            view.getReturnBarcodeField().clear();
            view.getReturnNotesArea().clear();
            currentReturnLoan = null;
            resetReturnLabels();
            view.getReturnConfirmButton().setDisable(true);

            loadActiveLoans();
        });

        task.setOnFailed(e -> {
            view.getReturnConfirmButton().setDisable(false);
            showReturnFeedback("Return processing failed: " + task.getException().getMessage(), true);
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void resetReturnLabels() {
        view.getReturnBookTitleLabel().setText("No loan loaded");
        view.getReturnBarcodeLabel().setText("—");
        view.getReturnMemberLabel().setText("—");
        view.getReturnIssuedDateLabel().setText("—");
        view.getReturnDueDateLabel().setText("—");
        view.getReturnOverdueDaysLabel().setText("0 Days");
        view.getDailyRateLabel().setText("৳ 0.00 / day");
        view.getReturnCalculatedFineLabel().setText("৳ 0.00");
        view.getReturnFineStatusBadge().setText("IDLE");
        view.getReturnFineStatusBadge().getStyleClass().removeAll("badge-success", "badge-error");
        view.getReturnFineStatusBadge().getStyleClass().add("badge-info");
    }

    private void showReturnFeedback(String message, boolean isError) {
        view.getReturnFeedbackLabel().setText(message);
        view.getReturnFeedbackLabel().getStyleClass().removeAll("banner-error", "banner-info");
        view.getReturnFeedbackLabel().getStyleClass().add(isError ? "banner-error" : "banner-info");
        view.getReturnFeedbackLabel().setVisible(true);
        view.getReturnFeedbackLabel().setManaged(true);
    }

    // =========================================================================
    // Active Loans Tab Workflows
    // =========================================================================

    private void loadActiveLoans() {
        String query = view.getLoansSearchField().getText().trim();
        String statusStr = view.getLoansStatusFilter().getValue();
        LoanStatus status = "ALL".equalsIgnoreCase(statusStr) ? null : LoanStatus.valueOf(statusStr);
        boolean overdueOnly = view.getLoansOverdueOnlyCheck().isSelected();

        Task<List<Loan>> task = new Task<>() {
            @Override
            protected List<Loan> call() {
                return circulationService.searchLoans(query, status, overdueOnly);
            }
        };

        task.setOnSucceeded(e -> {
            List<Loan> loans = task.getValue();
            view.getLoansTable().setItems(FXCollections.observableArrayList(loans));
            view.getLoansCountLabel().setText("Showing " + loans.size() + " loan record(s)");
        });

        task.setOnFailed(e -> log.error("Failed to load loans: {}", task.getException().getMessage()));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void setupTableContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem renewItem = new MenuItem("Renew Loan / ঋণ নবায়ন");
        renewItem.setOnAction(e -> {
            Loan selected = view.getLoansTable().getSelectionModel().getSelectedItem();
            if (selected != null) handleRenewLoan(selected);
        });

        MenuItem returnItem = new MenuItem("Process Return / ফেরত নিন");
        returnItem.setOnAction(e -> {
            Loan selected = view.getLoansTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                view.getTabPane().getSelectionModel().select(view.getReturnTab());
                view.getReturnBarcodeField().setText(selected.getCopyBarcode());
                handleLookupReturnLoan();
            }
        });

        menu.getItems().addAll(renewItem, returnItem);
        view.getLoansTable().setContextMenu(menu);
    }

    private void handleRenewLoan(Loan loan) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Renewal / নবায়ন নিশ্চিতকরণ");
        confirm.setHeaderText("Renew loan for: " + loan.getBookTitle());
        confirm.setContentText("Borrower: " + loan.getMemberName() + "\nCurrent Due Date: " +
                DateTimeUtil.formatDate(loan.getDueDate()) + "\nRenewals Used: " + loan.getRenewalCount());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Loan> task = new Task<>() {
                @Override
                protected Loan call() {
                    return circulationService.renewLoan(loan.getId());
                }
            };

            task.setOnSucceeded(e -> {
                Loan renewed = task.getValue();
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Renewal Successful");
                success.setHeaderText("Loan renewed successfully");
                success.setContentText("New Due Date: " + DateTimeUtil.formatDate(renewed.getDueDate()) +
                        "\nRenewal Count: " + renewed.getRenewalCount());
                success.showAndWait();
                loadActiveLoans();
            });

            task.setOnFailed(e -> {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Renewal Rejected");
                error.setHeaderText("Cannot renew loan");
                error.setContentText(task.getException().getMessage());
                error.showAndWait();
            });

            AppContext.getInstance().getExecutorService().execute(task);
        }
    }

    private void handleOpenPolicies() {
        Task<List<BorrowingPolicy>> task = new Task<>() {
            @Override
            protected List<BorrowingPolicy> call() {
                return circulationService.getAllBorrowingPolicies();
            }
        };

        task.setOnSucceeded(e -> {
            List<BorrowingPolicy> policies = task.getValue();
            BorrowingPolicyDialog dialog = new BorrowingPolicyDialog(policies);
            dialog.showAndWait();
        });

        task.setOnFailed(e -> log.error("Failed to load policies: {}", task.getException().getMessage()));
        AppContext.getInstance().getExecutorService().execute(task);
    }
}
