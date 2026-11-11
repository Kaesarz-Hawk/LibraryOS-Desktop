package com.libraryos.view;

import com.libraryos.core.validation.ValidationUtil;
import com.libraryos.model.member.Member;
import com.libraryos.model.member.MemberType;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;

public class MemberDialog extends Dialog<Member> {

    private final TextField cardNumField = new TextField();
    private final ComboBox<MemberType> typeCombo = new ComboBox<>();
    private final TextField nameEnField = new TextField();
    private final TextField nameBnField = new TextField();
    private final TextField rollField = new TextField();
    private final TextField deptField = new TextField();
    private final TextField programField = new TextField();
    private final TextField sessionField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final TextArea addressArea = new TextArea();
    private final DatePicker startDatePicker = new DatePicker(LocalDate.now());
    private final DatePicker expiryDatePicker = new DatePicker(LocalDate.now().plusYears(1));
    private final TextArea notesArea = new TextArea();

    private final Label errorLabel = new Label();
    private Member editingMember;

    public MemberDialog(Member memberToEdit, List<MemberType> types) {
        this.editingMember = memberToEdit;

        setTitle(memberToEdit == null ? "Register New Member / নতুন সদস্য" : "Edit Member Profile / সদস্য সম্পাদনা");
        setHeaderText(memberToEdit == null ? "Register patron membership into the institutional system" : "Update member profile");

        ButtonType saveBtnType = new ButtonType(memberToEdit == null ? "Register Member" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 10, 24));
        grid.setPrefWidth(640);

        errorLabel.getStyleClass().add("banner-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        grid.add(errorLabel, 0, 0, 2, 1);

        cardNumField.getStyleClass().add("form-input");
        typeCombo.setItems(FXCollections.observableArrayList(types));
        typeCombo.getStyleClass().add("form-input");
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        if (!types.isEmpty()) typeCombo.setValue(types.get(0));

        nameEnField.getStyleClass().add("form-input");
        nameBnField.getStyleClass().add("form-input");
        rollField.getStyleClass().add("form-input");
        deptField.getStyleClass().add("form-input");
        programField.getStyleClass().add("form-input");
        sessionField.getStyleClass().add("form-input");
        phoneField.getStyleClass().add("form-input");
        phoneField.setPromptText("e.g. 01712345678");
        emailField.getStyleClass().add("form-input");

        addressArea.getStyleClass().add("form-input");
        addressArea.setPrefRowCount(2);

        startDatePicker.getStyleClass().add("form-input");
        expiryDatePicker.getStyleClass().add("form-input");

        notesArea.getStyleClass().add("form-input");
        notesArea.setPrefRowCount(2);

        int row = 1;
        grid.add(new Label("Membership Number *"), 0, row);
        grid.add(cardNumField, 1, row++);

        grid.add(new Label("Member Type *"), 0, row);
        grid.add(typeCombo, 1, row++);

        grid.add(new Label("Full Name (English) *"), 0, row);
        grid.add(nameEnField, 1, row++);

        grid.add(new Label("Full Name (Bangla / বাংলা)"), 0, row);
        grid.add(nameBnField, 1, row++);

        HBox idAndDept = new HBox(12,
                new Label("Roll / Staff ID:"), rollField,
                new Label("Dept:"), deptField);
        rollField.setPrefWidth(140);
        grid.add(new Label("Institutional Details"), 0, row);
        grid.add(idAndDept, 1, row++);

        HBox progAndSession = new HBox(12,
                new Label("Program:"), programField,
                new Label("Session:"), sessionField);
        programField.setPrefWidth(160);
        grid.add(new Label("Program & Session"), 0, row);
        grid.add(progAndSession, 1, row++);

        HBox contactBox = new HBox(12,
                new Label("Phone (01...): *"), phoneField,
                new Label("Email:"), emailField);
        phoneField.setPrefWidth(140);
        grid.add(new Label("Contact Information"), 0, row);
        grid.add(contactBox, 1, row++);

        HBox datesBox = new HBox(12,
                new Label("Start:"), startDatePicker,
                new Label("Expiry:"), expiryDatePicker);
        grid.add(new Label("Validity Period"), 0, row);
        grid.add(datesBox, 1, row++);

        grid.add(new Label("Address"), 0, row);
        grid.add(addressArea, 1, row++);

        if (memberToEdit != null) {
            cardNumField.setText(memberToEdit.getMembershipNumber());
            cardNumField.setDisable(true);
            for (MemberType mt : types) {
                if (mt.getId() == memberToEdit.getMemberTypeId()) {
                    typeCombo.setValue(mt);
                    break;
                }
            }
            nameEnField.setText(memberToEdit.getFullNameEn());
            nameBnField.setText(memberToEdit.getFullNameBn());
            rollField.setText(memberToEdit.getInstitutionalId());
            deptField.setText(memberToEdit.getDepartment());
            programField.setText(memberToEdit.getProgram());
            sessionField.setText(memberToEdit.getSemesterOrSession());
            phoneField.setText(memberToEdit.getPhone());
            emailField.setText(memberToEdit.getEmail());
            addressArea.setText(memberToEdit.getAddress());
            if (memberToEdit.getMembershipStartDate() != null) startDatePicker.setValue(memberToEdit.getMembershipStartDate());
            if (memberToEdit.getMembershipExpiryDate() != null) expiryDatePicker.setValue(memberToEdit.getMembershipExpiryDate());
        } else {
            long rand = System.currentTimeMillis() % 10000;
            cardNumField.setText("LIB-2026-" + String.format("%04d", rand));
        }

        getDialogPane().setContent(grid);

        Button saveBtn = (Button) getDialogPane().lookupButton(saveBtnType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (cardNumField.getText().isBlank()) {
                showError("Membership Number is required.");
                event.consume();
            } else if (nameEnField.getText().isBlank()) {
                showError("Full Name (English) is required.");
                event.consume();
            } else if (!ValidationUtil.isValidBdPhone(phoneField.getText())) {
                showError("Invalid Bangladesh mobile number. Must match 013-019 (11 digits).");
                event.consume();
            }
        });

        setResultConverter(btn -> {
            if (btn == saveBtnType) {
                Member m = (editingMember != null) ? editingMember : new Member();
                m.setMembershipNumber(cardNumField.getText().trim());
                if (typeCombo.getValue() != null) {
                    m.setMemberTypeId(typeCombo.getValue().getId());
                    m.setMemberTypeName(typeCombo.getValue().getName());
                }
                m.setFullNameEn(nameEnField.getText().trim());
                m.setFullNameBn(nameBnField.getText() != null ? nameBnField.getText().trim() : null);
                m.setInstitutionalId(rollField.getText() != null ? rollField.getText().trim() : null);
                m.setDepartment(deptField.getText() != null ? deptField.getText().trim() : null);
                m.setProgram(programField.getText() != null ? programField.getText().trim() : null);
                m.setSemesterOrSession(sessionField.getText() != null ? sessionField.getText().trim() : null);
                m.setPhone(phoneField.getText().trim());
                m.setEmail(emailField.getText() != null && !emailField.getText().isBlank() ? emailField.getText().trim() : null);
                m.setAddress(addressArea.getText() != null ? addressArea.getText().trim() : null);
                m.setMembershipStartDate(startDatePicker.getValue());
                m.setMembershipExpiryDate(expiryDatePicker.getValue());

                return m;
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

