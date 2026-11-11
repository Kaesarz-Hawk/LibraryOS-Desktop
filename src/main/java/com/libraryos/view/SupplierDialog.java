package com.libraryos.view;

import com.libraryos.model.acquisition.Supplier;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Window;

public class SupplierDialog extends Dialog<Supplier> {

    private final TextField nameField;
    private final TextField contactPersonField;
    private final TextField phoneField;
    private final TextField emailField;
    private final TextArea addressArea;
    private final CheckBox activeCheck;

    public SupplierDialog(Window owner, Supplier existingSupplier) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle(existingSupplier == null ? "Add Supplier / নতুন সরবরাহকারী" : "Edit Supplier / সরবরাহকারী সম্পাদনা");
        setHeaderText(existingSupplier == null ? "Register a new book publisher or commercial procurement supplier." : "Update supplier contact information.");

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("card-panel");
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setText(existingSupplier == null ? "Save / সংরক্ষণ" : "Update / আপডেট");
        okBtn.getStyleClass().add("btn-primary");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        nameField = new TextField();
        nameField.setPromptText("e.g. Batighar Books / বাতিঘর প্রকাশনী");
        nameField.setPrefWidth(300);

        contactPersonField = new TextField();
        contactPersonField.setPromptText("Contact Person Name");

        phoneField = new TextField();
        phoneField.setPromptText("e.g. +8801711223344");

        emailField = new TextField();
        emailField.setPromptText("e.g. sales@batighar.com");

        addressArea = new TextArea();
        addressArea.setPromptText("Supplier address in Bangladesh...");
        addressArea.setPrefRowCount(3);

        activeCheck = new CheckBox("Active Supplier / সক্রিয় সরবরাহকারী");
        activeCheck.setSelected(true);

        if (existingSupplier != null) {
            nameField.setText(existingSupplier.getName());
            contactPersonField.setText(existingSupplier.getContactPerson());
            phoneField.setText(existingSupplier.getPhone());
            emailField.setText(existingSupplier.getEmail());
            addressArea.setText(existingSupplier.getAddress());
            activeCheck.setSelected(existingSupplier.isActive());
        }

        grid.add(new Label("Supplier Name / নাম: *"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Contact Person:"), 0, 1);
        grid.add(contactPersonField, 1, 1);

        grid.add(new Label("Phone / ফোন: *"), 0, 2);
        grid.add(phoneField, 1, 2);

        grid.add(new Label("Email / ইমেইল:"), 0, 3);
        grid.add(emailField, 1, 3);

        grid.add(new Label("Address / ঠিকানা:"), 0, 4);
        grid.add(addressArea, 1, 4);

        grid.add(new Label("Status:"), 0, 5);
        grid.add(activeCheck, 1, 5);

        pane.setContent(grid);

        setResultConverter(btnType -> {
            if (btnType == ButtonType.OK) {
                Supplier s = existingSupplier != null ? existingSupplier : new Supplier();
                s.setName(nameField.getText().trim());
                s.setContactPerson(contactPersonField.getText().trim());
                s.setPhone(phoneField.getText().trim());
                s.setEmail(emailField.getText().trim());
                s.setAddress(addressArea.getText().trim());
                s.setActive(activeCheck.isSelected());
                return s;
            }
            return null;
        });
    }
}

