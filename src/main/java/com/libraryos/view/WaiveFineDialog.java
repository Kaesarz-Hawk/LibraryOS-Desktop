package com.libraryos.view;

import com.libraryos.model.finance.Fine;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;

public class WaiveFineDialog extends Dialog<WaiveFineDialog.WaiverFormData> {

    public static class WaiverFormData {
        private final Long fineId;
        private final BigDecimal waiverAmount;
        private final String reason;

        public WaiverFormData(Long fineId, BigDecimal waiverAmount, String reason) {
            this.fineId = fineId;
            this.waiverAmount = waiverAmount;
            this.reason = reason;
        }

        public Long getFineId() { return fineId; }
        public BigDecimal getWaiverAmount() { return waiverAmount; }
        public String getReason() { return reason; }
    }

    private final TextField amountField = new TextField();
    private final TextArea reasonArea = new TextArea();
    private final Label errorLabel = new Label();

    public WaiveFineDialog(Fine fine) {
        setTitle("Waive Fine / জরিমানা মওকুফ");
        setHeaderText("Authorize fine waiver for Fine #" + fine.getId() + " (" + fine.getFineType() + ")");

        ButtonType waiveBtnType = new ButtonType("Authorize Waiver", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(waiveBtnType, ButtonType.CANCEL);

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16, 20, 10, 20));
        layout.setPrefWidth(460);

        errorLabel.getStyleClass().add("banner-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Warning banner
        VBox warningCard = new VBox(4);
        warningCard.setStyle("-fx-background-color: #FEF2F2; -fx-padding: 12px; -fx-border-color: #F87171; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        Label warnTitle = new Label("⚠️ Audit Notice: Waivers are logged permanently");
        warnTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #991B1B;");
        Label warnDesc = new Label("Fine: ৳ " + fine.getAmount().toPlainString() + " | Unpaid: ৳ " + fine.getUnpaidBalance().toPlainString()
                + "\nA detailed justification is required for institutional audit compliance.");
        warnDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F1D1D;");
        warningCard.getChildren().addAll(warnTitle, warnDesc);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        amountField.setText(fine.getUnpaidBalance().toPlainString());
        amountField.getStyleClass().add("form-input");

        reasonArea.setPromptText("Enter official justification (e.g., Medical emergency approved by Librarian)...");
        reasonArea.setPrefRowCount(3);
        reasonArea.getStyleClass().add("form-input");

        grid.add(new Label("Waiver Amount (৳) *:"), 0, 0);
        grid.add(amountField, 1, 0);

        grid.add(new Label("Mandatory Reason *:"), 0, 1);
        grid.add(reasonArea, 1, 1);

        layout.getChildren().addAll(warningCard, errorLabel, grid);
        getDialogPane().setContent(layout);

        Button waiveBtn = (Button) getDialogPane().lookupButton(waiveBtnType);
        waiveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                BigDecimal entered = new BigDecimal(amountField.getText().trim());
                if (entered.compareTo(BigDecimal.ZERO) <= 0) {
                    showError("Waiver amount must be greater than zero.");
                    event.consume();
                    return;
                }
                if (entered.compareTo(fine.getUnpaidBalance()) > 0) {
                    showError("Waiver amount (৳ " + entered + ") cannot exceed remaining unpaid balance (৳ " + fine.getUnpaidBalance() + ").");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid monetary amount.");
                event.consume();
                return;
            }

            String reasonText = reasonArea.getText() != null ? reasonArea.getText().trim() : "";
            if (reasonText.length() < 5) {
                showError("A valid justification of at least 5 characters is required.");
                event.consume();
            }
        });

        setResultConverter(btn -> {
            if (btn == waiveBtnType) {
                BigDecimal amt = new BigDecimal(amountField.getText().trim());
                String rsn = reasonArea.getText().trim();
                return new WaiverFormData(fine.getId(), amt, rsn);
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

