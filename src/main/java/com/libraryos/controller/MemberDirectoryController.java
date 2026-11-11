package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.security.SessionManager;
import com.libraryos.model.member.*;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.service.MemberService;
import com.libraryos.view.MemberDialog;
import com.libraryos.view.MemberDirectoryView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class MemberDirectoryController {

    private static final Logger log = LoggerFactory.getLogger(MemberDirectoryController.class);

    private final MemberDirectoryView view;
    private final MemberService memberService;

    public MemberDirectoryController(MemberDirectoryView view) {
        this.view = view;
        this.memberService = AppContext.getInstance().getMemberService();
        initialize();
    }

    private void initialize() {
        view.getAddMemberButton().setDisable(!SessionManager.hasPermission(PermissionCode.MEMBER_CREATE));
        view.getAddMemberButton().setOnAction(e -> handleAddMember());

        view.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> loadMembers());
        view.getTypeFilter().valueProperty().addListener((obs, oldVal, newVal) -> loadMembers());
        view.getStatusFilter().valueProperty().addListener((obs, oldVal, newVal) -> loadMembers());

        view.getResetButton().setOnAction(e -> {
            view.getSearchField().clear();
            view.getTypeFilter().setValue(null);
            view.getStatusFilter().setValue("ALL");
            loadMembers();
        });

        // Context Menu
        ContextMenu cm = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Member Profile / সম্পাদনা");
        editItem.setOnAction(e -> {
            Member selected = view.getMemberTable().getSelectionModel().getSelectedItem();
            if (selected != null) handleEditMember(selected);
        });

        MenuItem suspendItem = new MenuItem("Suspend / Reactivate Membership");
        suspendItem.setOnAction(e -> {
            Member selected = view.getMemberTable().getSelectionModel().getSelectedItem();
            if (selected != null) handleToggleStatus(selected);
        });

        MenuItem policyItem = new MenuItem("View Borrowing Policy Limits / নিয়মাবলী");
        policyItem.setOnAction(e -> {
            Member selected = view.getMemberTable().getSelectionModel().getSelectedItem();
            if (selected != null) handleViewPolicy(selected);
        });

        MenuItem printCardItem = new MenuItem("Print Member ID Card (PDF) / পরিচয়পত্র প্রিন্ট");
        printCardItem.setOnAction(e -> {
            Member selected = view.getMemberTable().getSelectionModel().getSelectedItem();
            if (selected != null) handlePrintMemberCard(selected);
        });

        cm.getItems().addAll(editItem, suspendItem, policyItem, printCardItem);
        view.getMemberTable().setContextMenu(cm);

        loadMemberTypes();
        loadMembers();
    }

    private void loadMemberTypes() {
        Task<List<MemberType>> task = new Task<>() {
            @Override
            protected List<MemberType> call() {
                return memberService.getAllMemberTypes();
            }
        };
        task.setOnSucceeded(e -> view.getTypeFilter().setItems(FXCollections.observableArrayList(task.getValue())));
        AppContext.getInstance().getExecutorService().submit(task);
    }

    public void loadMembers() {
        String query = view.getSearchField().getText();
        MemberType type = view.getTypeFilter().getValue();
        Integer typeId = type != null ? type.getId() : null;

        String statusStr = view.getStatusFilter().getValue();
        MemberStatus status = (statusStr != null && !"ALL".equalsIgnoreCase(statusStr)) ? MemberStatus.valueOf(statusStr) : null;

        view.getLoadingSpinner().setVisible(true);

        Task<List<Member>> task = new Task<>() {
            @Override
            protected List<Member> call() {
                return memberService.searchMembers(query, typeId, status);
            }
        };

        task.setOnSucceeded(e -> {
            view.getLoadingSpinner().setVisible(false);
            List<Member> list = task.getValue();
            view.getMemberTable().setItems(FXCollections.observableArrayList(list));
            view.getCountLabel().setText(list.size() + " member(s) registered");
        });

        task.setOnFailed(e -> {
            view.getLoadingSpinner().setVisible(false);
            log.error("Failed to load members", task.getException());
        });

        AppContext.getInstance().getExecutorService().submit(task);
    }

    private void handleAddMember() {
        List<MemberType> types = memberService.getAllMemberTypes();
        MemberDialog dialog = new MemberDialog(null, types);
        Optional<Member> result = dialog.showAndWait();

        result.ifPresent(newMember -> {
            try {
                memberService.registerMember(newMember);
                loadMembers();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Registration Error", ex.getMessage());
            }
        });
    }

    private void handleEditMember(Member member) {
        if (!SessionManager.hasPermission(PermissionCode.MEMBER_UPDATE)) {
            showAlert(Alert.AlertType.WARNING, "Permission Denied", "You do not have permission to update member details.");
            return;
        }

        List<MemberType> types = memberService.getAllMemberTypes();
        MemberDialog dialog = new MemberDialog(member, types);
        Optional<Member> result = dialog.showAndWait();

        result.ifPresent(updatedMember -> {
            try {
                memberService.updateMember(updatedMember);
                loadMembers();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Update Error", ex.getMessage());
            }
        });
    }

    private void handleToggleStatus(Member member) {
        if (!SessionManager.hasPermission(PermissionCode.MEMBER_SUSPEND)) {
            showAlert(Alert.AlertType.WARNING, "Permission Denied", "You do not have permission to suspend or activate members.");
            return;
        }

        MemberStatus targetStatus = (member.getStatus() == MemberStatus.ACTIVE) ? MemberStatus.SUSPENDED : MemberStatus.ACTIVE;
        String actionName = (targetStatus == MemberStatus.SUSPENDED) ? "Suspend" : "Reactivate";

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Confirm " + actionName);
        dialog.setHeaderText(actionName + " membership for: " + member.getFullNameEn() + " (" + member.getMembershipNumber() + ")");
        dialog.setContentText("Enter reason for " + actionName.toLowerCase() + ":");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            if (reason.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Reason Required", "Please provide a valid reason.");
                return;
            }
            try {
                memberService.setMemberStatus(member.getId(), targetStatus, reason);
                loadMembers();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Status Change Error", ex.getMessage());
            }
        });
    }

    private void handleViewPolicy(Member member) {
        try {
            BorrowingPolicy policy = memberService.getPolicyForMemberType(member.getMemberTypeId());
            String text = "Borrowing Policy Limits for " + member.getMemberTypeName() + ":\n\n" +
                    "• Max Active Loans: " + policy.getMaxActiveLoans() + " books\n" +
                    "• Loan Duration: " + policy.getLoanDurationDays() + " days\n" +
                    "• Max Renewals Allowed: " + policy.getMaxRenewals() + " (" + policy.getRenewalDurationDays() + " days each)\n" +
                    "• Grace Period: " + policy.getGracePeriodDays() + " days\n" +
                    "• Daily Overdue Fine: ৳ " + policy.getFinePerDayBdt() + " per day\n" +
                    "• Max Fine per Loan: " + (policy.getMaxFinePerLoanBdt() != null ? "৳ " + policy.getMaxFinePerLoanBdt() : "Unlimited") + "\n" +
                    "• Max Unpaid Fine Limit: ৳ " + policy.getMaxUnpaidFineLimitBdt() + "\n" +
                    "• Reservation Hold Duration: " + policy.getReservationHoldDays() + " days\n" +
                    "• Count Holidays in Overdue: " + (policy.isCountHolidaysInOverdue() ? "Yes" : "No (Fridays/holidays excluded)");

            showAlert(Alert.AlertType.INFORMATION, "Policy Limits: " + member.getMemberTypeName(), text);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Policy Error", ex.getMessage());
        }
    }

    private void handlePrintMemberCard(Member member) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Member ID Card PDF");
        fc.setInitialFileName("Member_Card_" + member.getMembershipNumber() + ".pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));

        java.io.File file = fc.showSaveDialog(view.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    AppContext.getInstance().getPdfExportService().generateMemberIdCardPdf(member, fos);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> showAlert(Alert.AlertType.INFORMATION, "Card Exported", "Member ID card PDF saved to:\n" + file.getAbsolutePath()));
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

