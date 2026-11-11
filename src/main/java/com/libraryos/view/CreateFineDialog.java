package com.libraryos.view;

import com.libraryos.model.finance.FineType;
import com.libraryos.model.member.Member;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

public class CreateFineDialog extends Dialog<CreateFineDialog.CreateFineFormData> {

    public static class CreateFineFormData {
        private final Long memberId;
        private final FineType fineType;
        private final BigDecimal amount;
        private final String reason;

        public CreateFineFormData(Long memberId, FineType fineType, BigDecimal amount, String reason) {
            this.memberId = memberId;
            this.fineType = fineType;
            this.amount = amount;
            this.reason = reason;
        }

        public Long getMemberId() { return memberId; }
        public FineType getFineType() { return fineType; }
        public BigDecimal getAmount() { return amount; }
        public String getReason() { return reason; }
    }

    private final ComboBox<Member> memberCombo = new ComboBox<>();
    private final ComboBox<FineType> fineTypeCombo = new ComboBox<>();
    private final TextField amountField = new TextField();
    private final TextArea reasonArea = new TextArea();
    private final Label errorLabel = new Label();

    public CreateFineDialog(List<Member> members, Member preselectedMember) {
        setTitle("Manual Fine Assessment / নতুন জরিমানা আরোপ");
        setHeaderText("Record an assessed liability for lost book, property damage, or penalty");

        ButtonType saveBtnType = new ButtonType("Assess Fine", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16, 20, 10, 20));
        layout.setPrefWidth(480);

        errorLabel.getStyleClass().add("banner-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        memberCombo.setItems(FXCollections.observableArrayList(members));
        memberCombo.setMaxWidth(Double.MAX_VALUE);
        memberCombo.getStyleClass().add("form-input");
        memberCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getMembershipNumber() + " — " + item.getDisplayName());
            }
        });
        memberCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getMembershipNumber() + " — " + item.getDisplayName());
            }
        });

        if (preselectedMember != null) {
            for (Member m : members) {
                if (m.getId().equals(preselectedMember.getId())) {
                    memberCombo.setValue(m);
                    break;
                }
            }
        }

        fineTypeCombo.setItems(FXCollections.observableArrayList(
                FineType.DAMAGED_BOOK, FineType.LOST_BOOK, FineType.OTHER
        ));
        fineTypeCombo.setValue(FineType.DAMAGED_BOOK);
        fineTypeCombo.setMaxWidth(Double.MAX_VALUE);
        fineTypeCombo.getStyleClass().add("form-input");

        amountField.setPromptText("e.g. 150.00");
        amountField.getStyleClass().add("form-input");

        reasonArea.setPromptText("Enter assessment reason or damaged item details...");
        reasonArea.setPrefRowCount(3);
        reasonArea.getStyleClass().add("form-input");

        grid.add(new Label("Member / সদস্য *:"), 0, 0);
        grid.add(memberCombo, 1, 0);

        grid.add(new Label("Fine Type / ধরন *:"), 0, 1);
        grid.add(fineTypeCombo, 1, 1);

        grid.add(new Label("Fine Amount (৳) *:"), 0, 2);
        grid.add(amountField, 1, 2);

        grid.add(new Label("Assessment Reason *:"), 0, 3);
        grid.add(reasonArea, 1, 3);

        layout.getChildren().addAll(errorLabel, grid);
        getDialogPane().setContent(layout);

        Button saveBtn = (Button) getDialogPane().lookupButton(saveBtnType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (memberCombo.getValue() == null) {
                showError("Please select a library member.");
                event.consume();
                return;
            }
            if (fineTypeCombo.getValue() == null) {
                showError("Please select a fine type.");
                event.consume();
                return;
            }
            try {
                BigDecimal entered = new BigDecimal(amountField.getText().trim());
                if (entered.compareTo(BigDecimal.ZERO) <= 0) {
                    showError("Fine amount must be greater than zero.");
                    event.consume();
                    return;
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid monetary amount.");
                event.consume();
                return;
            }

            String reasonText = reasonArea.getText() != null ? reasonArea.getText().trim() : "";
            if (reasonText.length() < 3) {
                showError("Please enter a brief assessment reason.");
                event.consume();
            }
        });

        setResultConverter(btn -> {
            if (btn == saveBtnType) {
                Long memberId = memberCombo.getValue().getId();
                FineType ft = fineTypeCombo.getValue();
                BigDecimal amt = new BigDecimal(amountField.getText().trim());
                String rsn = reasonArea.getText().trim();
                return new CreateFineFormData(memberId, ft, amt, rsn);
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

