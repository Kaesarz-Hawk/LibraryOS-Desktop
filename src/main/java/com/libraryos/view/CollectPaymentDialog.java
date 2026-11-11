package com.libraryos.view;

import com.libraryos.model.finance.Fine;
import com.libraryos.model.finance.PaymentMethod;
import com.libraryos.model.member.Member;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

public class CollectPaymentDialog extends Dialog<CollectPaymentDialog.PaymentFormData> {

    public static class PaymentFormData {
        private final BigDecimal amount;
        private final PaymentMethod paymentMethod;
        private final String transactionReference;
        private final String notes;

        public PaymentFormData(BigDecimal amount, PaymentMethod paymentMethod, String transactionReference, String notes) {
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.transactionReference = transactionReference;
            this.notes = notes;
        }

        public BigDecimal getAmount() { return amount; }
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public String getTransactionReference() { return transactionReference; }
        public String getNotes() { return notes; }
    }

    private final TextField amountField = new TextField();
    private final ComboBox<PaymentMethod> methodCombo = new ComboBox<>();
    private final TextField trxRefField = new TextField();
    private final TextArea notesArea = new TextArea();
    private final Label errorLabel = new Label();

    public CollectPaymentDialog(Member member, BigDecimal totalUnpaidBalance, List<Fine> unpaidFines) {
        setTitle("Collect Fine Payment / জরিমানা আদায়");
        setHeaderText("Record payment for " + member.getDisplayName() + " (" + member.getMembershipNumber() + ")");

        ButtonType collectBtnType = new ButtonType("Confirm Payment", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(collectBtnType, ButtonType.CANCEL);

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16, 20, 10, 20));
        layout.setPrefWidth(480);

        errorLabel.getStyleClass().add("banner-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Summary Info Banner
        VBox summaryCard = new VBox(4);
        summaryCard.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 12px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        Label memberLbl = new Label("Member: " + member.getDisplayName() + " | Card: " + member.getMembershipNumber());
        memberLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label balanceLbl = new Label("Total Outstanding Balance: ৳ " + (totalUnpaidBalance != null ? totalUnpaidBalance.toPlainString() : "0.00")
                + " across " + (unpaidFines != null ? unpaidFines.size() : 0) + " unpaid fine(s)");
        balanceLbl.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        summaryCard.getChildren().addAll(memberLbl, balanceLbl);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        // Pre-fill amount with total balance
        amountField.setText(totalUnpaidBalance != null ? totalUnpaidBalance.toPlainString() : "0.00");
        amountField.getStyleClass().add("form-input");

        methodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        methodCombo.setValue(PaymentMethod.CASH);
        methodCombo.setMaxWidth(Double.MAX_VALUE);
        methodCombo.getStyleClass().add("form-input");

        trxRefField.setPromptText("e.g. bKash TrxID, Bank Ref (optional for cash)");
        trxRefField.getStyleClass().add("form-input");

        notesArea.setPromptText("Optional payment notes");
        notesArea.setPrefRowCount(2);
        notesArea.getStyleClass().add("form-input");

        grid.add(new Label("Payment Amount (৳) *:"), 0, 0);
        grid.add(amountField, 1, 0);

        grid.add(new Label("Payment Method *:"), 0, 1);
        grid.add(methodCombo, 1, 1);

        grid.add(new Label("Transaction / Reference ID:"), 0, 2);
        grid.add(trxRefField, 1, 2);

        grid.add(new Label("Notes / মন্তব্য:"), 0, 3);
        grid.add(notesArea, 1, 3);

        Label fifoNotice = new Label("ℹ Payments are automatically allocated to unpaid fines on a FIFO (oldest first) basis.");
        fifoNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        layout.getChildren().addAll(summaryCard, errorLabel, grid, fifoNotice);
        getDialogPane().setContent(layout);

        Button collectBtn = (Button) getDialogPane().lookupButton(collectBtnType);
        collectBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                BigDecimal entered = new BigDecimal(amountField.getText().trim());
                if (entered.compareTo(BigDecimal.ZERO) <= 0) {
                    showError("Payment amount must be greater than zero.");
                    event.consume();
                    return;
                }
                if (totalUnpaidBalance != null && entered.compareTo(totalUnpaidBalance) > 0) {
                    showError("Payment amount (৳ " + entered + ") cannot exceed total unpaid balance (৳ " + totalUnpaidBalance + ").");
                    event.consume();
                    return;
                }
                if (methodCombo.getValue() == null) {
                    showError("Please select a valid payment method.");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid monetary amount.");
                event.consume();
            }
        });

        setResultConverter(btn -> {
            if (btn == collectBtnType) {
                BigDecimal amt = new BigDecimal(amountField.getText().trim());
                PaymentMethod method = methodCombo.getValue();
                String ref = trxRefField.getText() != null && !trxRefField.getText().isBlank() ? trxRefField.getText().trim() : null;
                String notes = notesArea.getText() != null && !notesArea.getText().isBlank() ? notesArea.getText().trim() : null;
                return new PaymentFormData(amt, method, ref, notes);
            }
            return null;
        });
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}

