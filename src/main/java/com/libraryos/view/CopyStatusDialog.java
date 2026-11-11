package com.libraryos.view;

import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.CopyStatus;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class CopyStatusDialog extends Dialog<CopyStatusDialog.StatusChangeResult> {

    public record StatusChangeResult(CopyStatus newStatus, String reason) {}

    private final ComboBox<CopyStatus> statusCombo = new ComboBox<>();
    private final TextField reasonField = new TextField();
    private final Label errorLabel = new Label();

    public CopyStatusDialog(BookCopy copy) {
        setTitle("Transition Copy Status / অবস্থা পরিবর্তন");
        setHeaderText("Change status for Accession: " + copy.getAccessionNumber() + " (" + copy.getStatus().name() + ")");

        ButtonType updateBtn = new ButtonType("Update Status", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(updateBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 24, 10, 24));
        grid.setPrefWidth(480);

        errorLabel.getStyleClass().add("banner-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        grid.add(errorLabel, 0, 0, 2, 1);

        statusCombo.setItems(FXCollections.observableArrayList(CopyStatus.values()));
        statusCombo.setValue(copy.getStatus());
        statusCombo.getStyleClass().add("form-input");
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        reasonField.getStyleClass().add("form-input");
        reasonField.setPromptText("Enter justification for status change (required)");

        grid.add(new Label("Current Status:"), 0, 1);
        grid.add(new Label(copy.getStatus().getDisplayName()), 1, 1);

        grid.add(new Label("Target Status: *"), 0, 2);
        grid.add(statusCombo, 1, 2);

        grid.add(new Label("Reason / কারণ: *"), 0, 3);
        grid.add(reasonField, 1, 3);

        getDialogPane().setContent(grid);

        Button save = (Button) getDialogPane().lookupButton(updateBtn);
        save.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (reasonField.getText() == null || reasonField.getText().isBlank()) {
                errorLabel.setText("A valid reason is required for status transitions.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                event.consume();
            }
        });

        setResultConverter(btn -> {
            if (btn == updateBtn) {
                return new StatusChangeResult(statusCombo.getValue(), reasonField.getText().trim());
            }
            return null;
        });
    }
}

