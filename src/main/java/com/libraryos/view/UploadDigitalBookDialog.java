package com.libraryos.view;

import com.libraryos.model.catalog.Book;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

public class UploadDigitalBookDialog extends Dialog<UploadDigitalBookDialog.Result> {

    public record Result(Long bookId, File file, boolean downloadAllowed) {}

    private final ComboBox<Book> bookComboBox;
    private final Label selectedFileLabel;
    private final CheckBox downloadAllowedCheck;
    private File selectedFile;

    public UploadDigitalBookDialog(Window owner, List<Book> availableBooks) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Upload E-Book / নতুন ডিজিটাল বই আপলোড");
        setHeaderText("Upload and register an authorized PDF document for the digital library.");

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("card-panel");
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setText("Upload / আপলোড");
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setDisable(true);

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));
        content.setPrefWidth(480);

        // 1. Select Book
        Label bookLabel = new Label("Select Bibliographic Title / বই নির্বাচন করুন: *");
        bookLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #0F172A;");

        bookComboBox = new ComboBox<>();
        bookComboBox.getItems().addAll(availableBooks);
        bookComboBox.setMaxWidth(Double.MAX_VALUE);
        bookComboBox.setPromptText("Choose catalog book...");
        bookComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + (item.getTitleBn() != null && !item.getTitleBn().isBlank() ? " (" + item.getTitleBn() + ")" : ""));
                }
            }
        });
        bookComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + (item.getTitleBn() != null && !item.getTitleBn().isBlank() ? " (" + item.getTitleBn() + ")" : ""));
                }
            }
        });

        // 2. File Chooser
        Label fileLabel = new Label("PDF File / পিডিএফ ফাইল: *");
        fileLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #0F172A;");

        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);

        Button browseBtn = new Button("Choose File... / ফাইল নির্বাচন");
        browseBtn.getStyleClass().add("btn-secondary");

        selectedFileLabel = new Label("No PDF file chosen.");
        selectedFileLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        browseBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select PDF Document");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
            File file = chooser.showOpenDialog(getOwner());
            if (file != null) {
                selectedFile = file;
                selectedFileLabel.setText(file.getName() + " (" + String.format("%.2f MB", file.length() / (1024.0 * 1024.0)) + ")");
                selectedFileLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: 600;");
                checkValidity(okBtn);
            }
        });

        fileBox.getChildren().addAll(browseBtn, selectedFileLabel);

        // 3. Download permission
        downloadAllowedCheck = new CheckBox("Allow Members to Download Offline / অফলাইন ডাউনলোডের অনুমতি দিন");
        downloadAllowedCheck.setSelected(true);
        downloadAllowedCheck.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

        bookComboBox.valueProperty().addListener((obs, oldVal, newVal) -> checkValidity(okBtn));

        content.getChildren().addAll(bookLabel, bookComboBox, fileLabel, fileBox, downloadAllowedCheck);
        pane.setContent(content);

        setResultConverter(btnType -> {
            if (btnType == ButtonType.OK && bookComboBox.getValue() != null && selectedFile != null) {
                return new Result(bookComboBox.getValue().getId(), selectedFile, downloadAllowedCheck.isSelected());
            }
            return null;
        });
    }

    private void checkValidity(Button okBtn) {
        boolean valid = bookComboBox.getValue() != null && selectedFile != null && selectedFile.exists();
        okBtn.setDisable(!valid);
    }
}

