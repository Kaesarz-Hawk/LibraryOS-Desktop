package com.libraryos.view;

import com.libraryos.model.catalog.Author;
import com.libraryos.model.catalog.Book;
import com.libraryos.model.catalog.Category;
import com.libraryos.model.catalog.Publisher;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

public class BookDialog extends Dialog<Book> {

    private final TextField titleEnField = new TextField();
    private final TextField titleBnField = new TextField();
    private final TextField subtitleField = new TextField();
    private final TextField isbn10Field = new TextField();
    private final TextField isbn13Field = new TextField();
    private final ComboBox<Publisher> publisherCombo = new ComboBox<>();
    private final TextField yearField = new TextField();
    private final TextField editionField = new TextField();
    private final ComboBox<String> languageCombo = new ComboBox<>();
    private final TextArea descArea = new TextArea();
    private final ListView<Author> authorListView = new ListView<>();
    private final ListView<Category> categoryListView = new ListView<>();

    private final Label errorLabel = new Label();
    private Book editingBook;

    public BookDialog(Book bookToEdit, List<Author> allAuthors, List<Category> allCategories, List<Publisher> allPublishers) {
        this.editingBook = bookToEdit;

        setTitle(bookToEdit == null ? "Add New Book Title / নতুন বই" : "Edit Book Title / বই সম্পাদনা");
        setHeaderText(bookToEdit == null ? "Enter bibliographic metadata for catalog" : "Update bibliographic information");

        ButtonType saveBtnType = new ButtonType(bookToEdit == null ? "Create Book" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 24, 10, 24));
        grid.setPrefWidth(680);

        errorLabel.getStyleClass().add("banner-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        grid.add(errorLabel, 0, 0, 2, 1);

        // Inputs styling
        titleEnField.getStyleClass().add("form-input");
        titleBnField.getStyleClass().add("form-input");
        subtitleField.getStyleClass().add("form-input");
        isbn10Field.getStyleClass().add("form-input");
        isbn13Field.getStyleClass().add("form-input");
        yearField.getStyleClass().add("form-input");
        editionField.getStyleClass().add("form-input");
        descArea.getStyleClass().add("form-input");
        descArea.setPrefRowCount(3);

        languageCombo.setItems(FXCollections.observableArrayList("Bangla", "English", "Arabic", "Other"));
        languageCombo.setValue("Bangla");
        languageCombo.getStyleClass().add("form-input");

        publisherCombo.setItems(FXCollections.observableArrayList(allPublishers));
        publisherCombo.getStyleClass().add("form-input");
        publisherCombo.setMaxWidth(Double.MAX_VALUE);

        authorListView.setItems(FXCollections.observableArrayList(allAuthors));
        authorListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        authorListView.setPrefHeight(100);

        categoryListView.setItems(FXCollections.observableArrayList(allCategories));
        categoryListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        categoryListView.setPrefHeight(100);

        int row = 1;
        grid.add(new Label("Title (English) *"), 0, row);
        grid.add(titleEnField, 1, row++);

        grid.add(new Label("Title (Bangla / বাংলা)"), 0, row);
        grid.add(titleBnField, 1, row++);

        grid.add(new Label("Subtitle"), 0, row);
        grid.add(subtitleField, 1, row++);

        HBox isbnBox = new HBox(10, new Label("ISBN-13:"), isbn13Field, new Label("ISBN-10:"), isbn10Field);
        isbnBox.getChildren().get(1).setStyle("-fx-pref-width: 180;");
        isbnBox.getChildren().get(3).setStyle("-fx-pref-width: 140;");
        grid.add(new Label("ISBN"), 0, row);
        grid.add(isbnBox, 1, row++);

        grid.add(new Label("Publisher"), 0, row);
        grid.add(publisherCombo, 1, row++);

        HBox detailsBox = new HBox(12,
                new Label("Year:"), yearField,
                new Label("Edition:"), editionField,
                new Label("Lang:"), languageCombo);
        yearField.setPrefWidth(80);
        editionField.setPrefWidth(120);
        grid.add(new Label("Publication Info"), 0, row);
        grid.add(detailsBox, 1, row++);

        grid.add(new Label("Select Author(s)"), 0, row);
        grid.add(authorListView, 1, row++);

        grid.add(new Label("Select Category(ies)"), 0, row);
        grid.add(categoryListView, 1, row++);

        grid.add(new Label("Description"), 0, row);
        grid.add(descArea, 1, row++);

        // Populate existing data if editing
        if (bookToEdit != null) {
            titleEnField.setText(bookToEdit.getTitleEn());
            titleBnField.setText(bookToEdit.getTitleBn());
            subtitleField.setText(bookToEdit.getSubtitle());
            isbn10Field.setText(bookToEdit.getIsbn10());
            isbn13Field.setText(bookToEdit.getIsbn13());
            if (bookToEdit.getPublicationYear() != null) {
                yearField.setText(String.valueOf(bookToEdit.getPublicationYear()));
            }
            editionField.setText(bookToEdit.getEdition());
            if (bookToEdit.getLanguage() != null) {
                languageCombo.setValue(bookToEdit.getLanguage());
            }
            descArea.setText(bookToEdit.getDescription());

            // Select publisher
            if (bookToEdit.getPublisherId() != null) {
                for (Publisher p : allPublishers) {
                    if (p.getId().equals(bookToEdit.getPublisherId())) {
                        publisherCombo.setValue(p);
                        break;
                    }
                }
            }

            // Select authors
            for (Author a : bookToEdit.getAuthors()) {
                for (int i = 0; i < allAuthors.size(); i++) {
                    if (allAuthors.get(i).getId().equals(a.getId())) {
                        authorListView.getSelectionModel().select(i);
                    }
                }
            }

            // Select categories
            for (Category c : bookToEdit.getCategories()) {
                for (int i = 0; i < allCategories.size(); i++) {
                    if (allCategories.get(i).getId().equals(c.getId())) {
                        categoryListView.getSelectionModel().select(i);
                    }
                }
            }
        }

        getDialogPane().setContent(grid);

        // Validation filter on Save button
        Button saveBtn = (Button) getDialogPane().lookupButton(saveBtnType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleEnField.getText() == null || titleEnField.getText().isBlank()) {
                errorLabel.setText("Title (English) is required.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                event.consume();
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                Book b = (editingBook != null) ? editingBook : new Book();
                b.setTitleEn(titleEnField.getText().trim());
                b.setTitleBn(titleBnField.getText() != null ? titleBnField.getText().trim() : null);
                b.setSubtitle(subtitleField.getText() != null ? subtitleField.getText().trim() : null);
                b.setIsbn10(isbn10Field.getText() != null && !isbn10Field.getText().isBlank() ? isbn10Field.getText().trim() : null);
                b.setIsbn13(isbn13Field.getText() != null && !isbn13Field.getText().isBlank() ? isbn13Field.getText().trim() : null);

                Publisher selectedPub = publisherCombo.getValue();
                if (selectedPub != null) {
                    b.setPublisherId(selectedPub.getId());
                    b.setPublisherName(selectedPub.getName());
                }

                if (!yearField.getText().isBlank()) {
                    try {
                        b.setPublicationYear(Integer.parseInt(yearField.getText().trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                b.setEdition(editionField.getText() != null ? editionField.getText().trim() : null);
                b.setLanguage(languageCombo.getValue());
                b.setDescription(descArea.getText() != null ? descArea.getText().trim() : null);

                b.setAuthors(new ArrayList<>(authorListView.getSelectionModel().getSelectedItems()));
                b.setCategories(new ArrayList<>(categoryListView.getSelectionModel().getSelectedItems()));

                return b;
            }
            return null;
        });
    }

    public List<Long> getSelectedAuthorIds() {
        return authorListView.getSelectionModel().getSelectedItems().stream().map(Author::getId).toList();
    }

    public List<Long> getSelectedCategoryIds() {
        return categoryListView.getSelectionModel().getSelectedItems().stream().map(Category::getId).toList();
    }
}

