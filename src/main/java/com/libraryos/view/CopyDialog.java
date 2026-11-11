package com.libraryos.view;

import com.libraryos.model.catalog.Book;
import com.libraryos.model.inventory.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CopyDialog extends Dialog<BookCopy> {

    private final ComboBox<Book> bookCombo = new ComboBox<>();
    private final TextField accessionField = new TextField();
    private final TextField barcodeField = new TextField();
    private final ComboBox<Shelf> shelfCombo = new ComboBox<>();
    private final ComboBox<AcquisitionSource> sourceCombo = new ComboBox<>();
    private final TextField costField = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<ConditionStatus> conditionCombo = new ComboBox<>();
    private final TextArea notesArea = new TextArea();

    private final Label errorLabel = new Label();
    private BookCopy editingCopy;

    public CopyDialog(BookCopy copyToEdit, List<Book> books, List<Shelf> shelves) {
        this.editingCopy = copyToEdit;

        setTitle(copyToEdit == null ? "Add Physical Copy / নতুন কপি" : "Edit Copy Details / কপি সম্পাদনা");
        setHeaderText(copyToEdit == null ? "Register an accession barcode into inventory" : "Update copy attributes");

        ButtonType saveBtnType = new ButtonType(copyToEdit == null ? "Add Copy" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 10, 24));
        grid.setPrefWidth(580);

        errorLabel.getStyleClass().add("banner-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        grid.add(errorLabel, 0, 0, 2, 1);

        bookCombo.setItems(FXCollections.observableArrayList(books));
        bookCombo.getStyleClass().add("form-input");
        bookCombo.setMaxWidth(Double.MAX_VALUE);

        accessionField.getStyleClass().add("form-input");
        barcodeField.getStyleClass().add("form-input");
        costField.getStyleClass().add("form-input");
        costField.setText("0.00");

        shelfCombo.setItems(FXCollections.observableArrayList(shelves));
        shelfCombo.getStyleClass().add("form-input");
        shelfCombo.setMaxWidth(Double.MAX_VALUE);

        sourceCombo.setItems(FXCollections.observableArrayList(AcquisitionSource.values()));
        sourceCombo.setValue(AcquisitionSource.PURCHASE);
        sourceCombo.getStyleClass().add("form-input");

        conditionCombo.setItems(FXCollections.observableArrayList(ConditionStatus.values()));
        conditionCombo.setValue(ConditionStatus.GOOD);
        conditionCombo.getStyleClass().add("form-input");

        datePicker.getStyleClass().add("form-input");
        notesArea.getStyleClass().add("form-input");
        notesArea.setPrefRowCount(3);

        int row = 1;
        grid.add(new Label("Select Book Title *"), 0, row);
        grid.add(bookCombo, 1, row++);

        grid.add(new Label("Accession Number *"), 0, row);
        grid.add(accessionField, 1, row++);

        grid.add(new Label("Barcode (Code-128) *"), 0, row);
        grid.add(barcodeField, 1, row++);

        grid.add(new Label("Shelf Location"), 0, row);
        grid.add(shelfCombo, 1, row++);

        grid.add(new Label("Acquisition Source"), 0, row);
        grid.add(sourceCombo, 1, row++);

        HBox costAndDate = new HBox(12, new Label("Cost (BDT):"), costField, new Label("Date:"), datePicker);
        costField.setPrefWidth(90);
        grid.add(new Label("Cost & Acquisition"), 0, row);
        grid.add(costAndDate, 1, row++);

        grid.add(new Label("Condition"), 0, row);
        grid.add(conditionCombo, 1, row++);

        grid.add(new Label("Notes"), 0, row);
        grid.add(notesArea, 1, row++);

        // If editing existing
        if (copyToEdit != null) {
            for (Book b : books) {
                if (b.getId().equals(copyToEdit.getBookId())) {
                    bookCombo.setValue(b);
                    break;
                }
            }
            bookCombo.setDisable(true); // Don't change book reference
            accessionField.setText(copyToEdit.getAccessionNumber());
            accessionField.setDisable(true); // Accession number is permanent identifier
            barcodeField.setText(copyToEdit.getBarcode());
            if (copyToEdit.getShelfId() != null) {
                for (Shelf s : shelves) {
                    if (s.getId().equals(copyToEdit.getShelfId())) {
                        shelfCombo.setValue(s);
                        break;
                    }
                }
            }
            sourceCombo.setValue(copyToEdit.getAcquisitionSource());
            costField.setText(copyToEdit.getPurchaseCost() != null ? copyToEdit.getPurchaseCost().toString() : "0.00");
            if (copyToEdit.getAcquiredDate() != null) {
                datePicker.setValue(copyToEdit.getAcquiredDate());
            }
            conditionCombo.setValue(copyToEdit.getConditionStatus());
            notesArea.setText(copyToEdit.getNotes());
        } else {
            // Auto-generate suggestion
            long randomNum = System.currentTimeMillis() % 100000;
            accessionField.setText("ACC-2026-" + String.format("%04d", randomNum));
            barcodeField.setText("BC" + (100000 + randomNum));
        }

        getDialogPane().setContent(grid);

        Button saveBtn = (Button) getDialogPane().lookupButton(saveBtnType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (bookCombo.getValue() == null) {
                showError("Please select a book title.");
                event.consume();
            } else if (accessionField.getText().isBlank()) {
                showError("Accession Number is required.");
                event.consume();
            } else if (barcodeField.getText().isBlank()) {
                showError("Barcode is required.");
                event.consume();
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                BookCopy c = (editingCopy != null) ? editingCopy : new BookCopy();
                c.setBookId(bookCombo.getValue().getId());
                c.setBookTitle(bookCombo.getValue().getTitleEn());
                c.setAccessionNumber(accessionField.getText().trim());
                c.setBarcode(barcodeField.getText().trim());

                Shelf s = shelfCombo.getValue();
                if (s != null) {
                    c.setShelfId(s.getId());
                    c.setShelfCode(s.getCode());
                } else {
                    c.setShelfId(null);
                    c.setShelfCode(null);
                }

                c.setAcquisitionSource(sourceCombo.getValue());
                try {
                    c.setPurchaseCost(new BigDecimal(costField.getText().trim()));
                } catch (Exception e) {
                    c.setPurchaseCost(BigDecimal.ZERO);
                }
                c.setAcquiredDate(datePicker.getValue());
                c.setConditionStatus(conditionCombo.getValue());
                c.setNotes(notesArea.getText() != null ? notesArea.getText().trim() : null);

                return c;
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

