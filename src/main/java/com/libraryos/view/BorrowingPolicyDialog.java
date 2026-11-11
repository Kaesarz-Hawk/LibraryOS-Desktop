package com.libraryos.view;

import com.libraryos.app.AppContext;
import com.libraryos.core.exception.ValidationException;
import com.libraryos.model.member.BorrowingPolicy;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;

public class BorrowingPolicyDialog extends Dialog<Void> {

    private final ComboBox<BorrowingPolicy> policyCombo = new ComboBox<>();
    private final Spinner<Integer> maxLoansSpinner = new Spinner<>(1, 50, 3);
    private final Spinner<Integer> loanDurationSpinner = new Spinner<>(1, 365, 14);
    private final Spinner<Integer> maxRenewalsSpinner = new Spinner<>(0, 10, 1);
    private final Spinner<Integer> renewalDurationSpinner = new Spinner<>(1, 60, 7);
    private final Spinner<Integer> gracePeriodSpinner = new Spinner<>(0, 30, 0);
    private final TextField finePerDayField = new TextField("5.00");
    private final TextField maxFineField = new TextField("100.00");
    private final TextField maxUnpaidLimitField = new TextField("100.00");
    private final Spinner<Integer> holdDaysSpinner = new Spinner<>(1, 30, 3);
    private final CheckBox countHolidaysCheck = new CheckBox("Count Fridays / Library Holidays in Overdue Calculation");

    private final Label bannerError = new Label();
    private final Label bannerSuccess = new Label();

    public BorrowingPolicyDialog(List<BorrowingPolicy> policies) {
        setTitle("Borrowing Policies / ঋণ নীতিমালা সেটিংস");
        setHeaderText("Configure borrowing quotas, loan periods, and overdue fine rates per member tier");

        ButtonType saveBtnType = new ButtonType("Save Policy", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CLOSE);

        VBox content = new VBox(14);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setPrefWidth(540);

        bannerError.getStyleClass().add("banner-error");
        bannerError.setVisible(false);
        bannerError.setManaged(false);

        bannerSuccess.getStyleClass().add("banner-info");
        bannerSuccess.setVisible(false);
        bannerSuccess.setManaged(false);

        content.getChildren().addAll(bannerError, bannerSuccess);

        // Tier Selector
        VBox selectorBox = new VBox(4);
        Label tierLabel = new Label("Select Member Tier / মেম্বার ধরন:");
        tierLabel.getStyleClass().add("form-label");
        policyCombo.setItems(FXCollections.observableArrayList(policies));
        policyCombo.setMaxWidth(Double.MAX_VALUE);
        policyCombo.getStyleClass().add("form-input");
        policyCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BorrowingPolicy item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getMemberTypeCode() + " (Tier #" + item.getMemberTypeId() + ")");
            }
        });
        policyCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(BorrowingPolicy item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getMemberTypeCode() + " (Tier #" + item.getMemberTypeId() + ")");
            }
        });
        selectorBox.getChildren().addAll(tierLabel, policyCombo);
        content.getChildren().add(selectorBox);

        // Parameters Grid
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);

        configureSpinner(maxLoansSpinner);
        configureSpinner(loanDurationSpinner);
        configureSpinner(maxRenewalsSpinner);
        configureSpinner(renewalDurationSpinner);
        configureSpinner(gracePeriodSpinner);
        configureSpinner(holdDaysSpinner);

        finePerDayField.getStyleClass().add("form-input");
        maxFineField.getStyleClass().add("form-input");
        maxUnpaidLimitField.getStyleClass().add("form-input");

        int row = 0;
        grid.add(new Label("Max Active Loans / সর্বোচ্চ বই:"), 0, row);
        grid.add(maxLoansSpinner, 1, row++);

        grid.add(new Label("Loan Duration (Days) / ঋণ মেয়াদ (দিন):"), 0, row);
        grid.add(loanDurationSpinner, 1, row++);

        grid.add(new Label("Max Renewals / সর্বোচ্চ নবায়ন:"), 0, row);
        grid.add(maxRenewalsSpinner, 1, row++);

        grid.add(new Label("Renewal Duration (Days) / নবায়ন মেয়াদ:"), 0, row);
        grid.add(renewalDurationSpinner, 1, row++);

        grid.add(new Label("Grace Period (Days) / গ্রেস পিরিয়ড:"), 0, row);
        grid.add(gracePeriodSpinner, 1, row++);

        grid.add(new Label("Daily Fine Rate (৳) / দৈনিক জরিমানা:"), 0, row);
        grid.add(finePerDayField, 1, row++);

        grid.add(new Label("Max Fine Per Loan (৳) / সর্বোচ্চ জরিমানা:"), 0, row);
        grid.add(maxFineField, 1, row++);

        grid.add(new Label("Max Unpaid Fine Limit (৳) / বকেয়া সীমা:"), 0, row);
        grid.add(maxUnpaidLimitField, 1, row++);

        grid.add(new Label("Hold Pickup Window (Days) / হোল্ড সময়:"), 0, row);
        grid.add(holdDaysSpinner, 1, row++);

        grid.add(countHolidaysCheck, 0, row++, 2, 1);

        content.getChildren().add(grid);
        getDialogPane().setContent(content);

        policyCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateFields(newVal);
            }
        });

        if (!policies.isEmpty()) {
            policyCombo.getSelectionModel().selectFirst();
        }

        Button saveBtn = (Button) getDialogPane().lookupButton(saveBtnType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            BorrowingPolicy selected = policyCombo.getValue();
            if (selected == null) {
                showError("Please select a borrowing policy tier");
                event.consume();
                return;
            }

            try {
                selected.setMaxActiveLoans(maxLoansSpinner.getValue());
                selected.setLoanDurationDays(loanDurationSpinner.getValue());
                selected.setMaxRenewals(maxRenewalsSpinner.getValue());
                selected.setRenewalDurationDays(renewalDurationSpinner.getValue());
                selected.setGracePeriodDays(gracePeriodSpinner.getValue());
                selected.setFinePerDayBdt(new BigDecimal(finePerDayField.getText().trim()));
                if (!maxFineField.getText().isBlank()) {
                    selected.setMaxFinePerLoanBdt(new BigDecimal(maxFineField.getText().trim()));
                } else {
                    selected.setMaxFinePerLoanBdt(null);
                }
                selected.setMaxUnpaidFineLimitBdt(new BigDecimal(maxUnpaidLimitField.getText().trim()));
                selected.setReservationHoldDays(holdDaysSpinner.getValue());
                selected.setCountHolidaysInOverdue(countHolidaysCheck.isSelected());

                AppContext.getInstance().getCirculationService().updateBorrowingPolicy(selected);
                showSuccess("Borrowing policy for " + selected.getMemberTypeCode() + " successfully updated!");
                event.consume();
            } catch (NumberFormatException e) {
                showError("Please enter valid numeric amounts for BDT currency fields");
                event.consume();
            } catch (ValidationException e) {
                showError(e.getMessage());
                event.consume();
            } catch (Exception e) {
                showError("Failed to save policy: " + e.getMessage());
                event.consume();
            }
        });
    }

    private void configureSpinner(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.getStyleClass().add("form-input");
        spinner.setMaxWidth(Double.MAX_VALUE);
    }

    private void populateFields(BorrowingPolicy policy) {
        maxLoansSpinner.getValueFactory().setValue(policy.getMaxActiveLoans());
        loanDurationSpinner.getValueFactory().setValue(policy.getLoanDurationDays());
        maxRenewalsSpinner.getValueFactory().setValue(policy.getMaxRenewals());
        renewalDurationSpinner.getValueFactory().setValue(policy.getRenewalDurationDays());
        gracePeriodSpinner.getValueFactory().setValue(policy.getGracePeriodDays());
        finePerDayField.setText(policy.getFinePerDayBdt() != null ? policy.getFinePerDayBdt().toPlainString() : "5.00");
        maxFineField.setText(policy.getMaxFinePerLoanBdt() != null ? policy.getMaxFinePerLoanBdt().toPlainString() : "");
        maxUnpaidLimitField.setText(policy.getMaxUnpaidFineLimitBdt() != null ? policy.getMaxUnpaidFineLimitBdt().toPlainString() : "100.00");
        holdDaysSpinner.getValueFactory().setValue(policy.getReservationHoldDays());
        countHolidaysCheck.setSelected(policy.isCountHolidaysInOverdue());

        bannerError.setVisible(false);
        bannerError.setManaged(false);
        bannerSuccess.setVisible(false);
        bannerSuccess.setManaged(false);
    }

    private void showError(String message) {
        bannerError.setText(message);
        bannerError.setVisible(true);
        bannerError.setManaged(true);
        bannerSuccess.setVisible(false);
        bannerSuccess.setManaged(false);
    }

    private void showSuccess(String message) {
        bannerSuccess.setText(message);
        bannerSuccess.setVisible(true);
        bannerSuccess.setManaged(true);
        bannerError.setVisible(false);
        bannerError.setManaged(false);
    }
}

