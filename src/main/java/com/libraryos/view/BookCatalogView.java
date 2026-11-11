package com.libraryos.view;

import com.libraryos.model.catalog.Author;
import com.libraryos.model.catalog.Book;
import com.libraryos.model.catalog.Category;
import com.libraryos.model.catalog.Publisher;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Modern JavaFX Books Catalog screen supporting live bilingual search, multi-category filtering,
 * and book addition/editing dialogs.
 */
public class BookCatalogView extends BorderPane {

    private final TextField searchField;
    private final ComboBox<Category> categoryFilter;
    private final ComboBox<Publisher> publisherFilter;
    private final Button resetButton;
    private final Button addBookButton;
    private final TableView<Book> bookTable;
    private final Label countLabel;
    private final ProgressIndicator loadingSpinner;

    public BookCatalogView() {
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        // Top Section: Header & Action Bar
        VBox topBox = new VBox(16);

        // Header Row
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Books Catalog / বই ক্যাটালগ");
        title.getStyleClass().add("page-title");

        countLabel = new Label("Loading catalog...");
        countLabel.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, countLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(20, 20);
        loadingSpinner.setVisible(false);

        addBookButton = new Button("+ Add Book Title / নতুন বই");
        addBookButton.getStyleClass().add("btn-primary");

        headerRow.getChildren().addAll(titleBox, spacer, loadingSpinner, addBookButton);

        // Filter Toolbar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().addAll("card-panel", "filter-container");

        searchField = new TextField();
        searchField.setPromptText("Search by English/Bangla title, ISBN, author...");
        searchField.getStyleClass().add("form-input");
        searchField.setPrefWidth(320);

        categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("All Categories");
        categoryFilter.getStyleClass().add("form-input");
        categoryFilter.setPrefWidth(200);

        publisherFilter = new ComboBox<>();
        publisherFilter.setPromptText("All Publishers");
        publisherFilter.getStyleClass().add("form-input");
        publisherFilter.setPrefWidth(180);

        resetButton = new Button("Reset");
        resetButton.getStyleClass().add("btn-secondary");

        filterBar.getChildren().addAll(searchField, categoryFilter, publisherFilter, resetButton);
        topBox.getChildren().addAll(headerRow, filterBar);
        setTop(topBox);

        // Center Table View
        bookTable = new TableView<>();
        bookTable.getStyleClass().add("table-view");
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Book, Number> idCol = new TableColumn<>("#");
        idCol.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        idCol.setMaxWidth(60);
        idCol.setMinWidth(50);

        TableColumn<Book, String> titleCol = new TableColumn<>("Book Title / শিরোনাম");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayTitle()));
        titleCol.setMinWidth(260);

        TableColumn<Book, String> authorsCol = new TableColumn<>("Author(s) / লেখক");
        authorsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAuthorsDisplay()));
        authorsCol.setMinWidth(180);

        TableColumn<Book, String> categoriesCol = new TableColumn<>("Category / বিভাগ");
        categoriesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoriesDisplay()));
        categoriesCol.setMinWidth(160);

        TableColumn<Book, String> publisherCol = new TableColumn<>("Publisher / প্রকাশক");
        publisherCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPublisherName() != null ? data.getValue().getPublisherName() : "—"));
        publisherCol.setMinWidth(150);

        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN-13 / ISBN-10");
        isbnCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getIsbn13() != null ? data.getValue().getIsbn13() :
                (data.getValue().getIsbn10() != null ? data.getValue().getIsbn10() : "—")));
        isbnCol.setMinWidth(160);

        TableColumn<Book, Number> totalCopiesCol = new TableColumn<>("Total Copies");
        totalCopiesCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTotalCopies()));
        totalCopiesCol.setMaxWidth(110);
        totalCopiesCol.setMinWidth(95);

        TableColumn<Book, String> availCol = new TableColumn<>("Available");
        availCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getAvailableCopies())));
        availCol.setMaxWidth(110);
        availCol.setMinWidth(95);
        availCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    int count = Integer.parseInt(item);
                    Label badge = new Label(item + " Avail");
                    if (count > 0) {
                        badge.getStyleClass().add("badge-available");
                    } else {
                        badge.getStyleClass().add("badge-overdue");
                    }
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        bookTable.getColumns().addAll(idCol, titleCol, authorsCol, categoriesCol, publisherCol, isbnCol, totalCopiesCol, availCol);

        // Elegant Bilingual Empty State
        VBox emptyState = new VBox(10);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(32));

        Label emptyIcon = new Label("📚 🔍");
        emptyIcon.setStyle("-fx-font-size: 38px;");

        Label emptyTitle = new Label("No books found matching your query / কোনো বই পাওয়া যায়নি");
        emptyTitle.getStyleClass().add("card-title");
        emptyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label emptySub = new Label("Try adjusting your search terms or clearing active category and publisher filters.");
        emptySub.getStyleClass().add("page-subtitle");
        emptySub.setStyle("-fx-font-size: 13px;");

        Button emptyResetBtn = new Button("↺ Reset Filters / ফিল্টার মুছুন");
        emptyResetBtn.getStyleClass().add("btn-primary");
        emptyResetBtn.setOnAction(e -> resetButton.fire());

        emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptySub, emptyResetBtn);
        bookTable.setPlaceholder(emptyState);

        VBox centerBox = new VBox(bookTable);
        centerBox.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(bookTable, Priority.ALWAYS);
        setCenter(centerBox);
    }

    public TextField getSearchField() {
        return searchField;
    }

    public ComboBox<Category> getCategoryFilter() {
        return categoryFilter;
    }

    public ComboBox<Publisher> getPublisherFilter() {
        return publisherFilter;
    }

    public Button getResetButton() {
        return resetButton;
    }

    public Button getAddBookButton() {
        return addBookButton;
    }

    public TableView<Book> getBookTable() {
        return bookTable;
    }

    public Label getCountLabel() {
        return countLabel;
    }

    public ProgressIndicator getLoadingSpinner() {
        return loadingSpinner;
    }
}

