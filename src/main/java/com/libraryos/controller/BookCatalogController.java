package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.security.SessionManager;
import com.libraryos.model.catalog.*;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.service.CatalogService;
import com.libraryos.view.BookCatalogView;
import com.libraryos.view.BookDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class BookCatalogController {

    private static final Logger log = LoggerFactory.getLogger(BookCatalogController.class);

    private final BookCatalogView view;
    private final CatalogService catalogService;

    public BookCatalogController(BookCatalogView view) {
        this.view = view;
        this.catalogService = AppContext.getInstance().getCatalogService();
        initialize();
    }

    private void initialize() {
        // Enforce permission on Add button
        view.getAddBookButton().setDisable(!SessionManager.hasPermission(PermissionCode.BOOK_CREATE));
        view.getAddBookButton().setOnAction(e -> handleAddBook());

        // Wire search and filter controls
        view.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> loadBooks());

        view.getCategoryFilter().valueProperty().addListener((obs, oldVal, newVal) -> loadBooks());
        view.getPublisherFilter().valueProperty().addListener((obs, oldVal, newVal) -> loadBooks());

        view.getResetButton().setOnAction(e -> {
            view.getSearchField().clear();
            view.getCategoryFilter().setValue(null);
            view.getPublisherFilter().setValue(null);
            loadBooks();
        });

        // Context Menu for Edit / Archive
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Book Metadata / সম্পাদনা");
        editItem.setOnAction(e -> {
            Book selected = view.getBookTable().getSelectionModel().getSelectedItem();
            if (selected != null) handleEditBook(selected);
        });

        MenuItem archiveItem = new MenuItem("Archive Book / সংরক্ষণাগারে পাঠান");
        archiveItem.setOnAction(e -> {
            Book selected = view.getBookTable().getSelectionModel().getSelectedItem();
            if (selected != null) handleArchiveBook(selected);
        });

        contextMenu.getItems().addAll(editItem, archiveItem);
        view.getBookTable().setContextMenu(contextMenu);

        // Load categories and publishers for dropdowns
        loadFilterDropdowns();

        // Initial book load
        loadBooks();
    }

    private void loadFilterDropdowns() {
        Task<Void> task = new Task<>() {
            List<Category> categories;
            List<Publisher> publishers;

            @Override
            protected Void call() {
                categories = catalogService.getAllCategories();
                publishers = catalogService.getAllPublishers();
                return null;
            }

            @Override
            protected void succeeded() {
                view.getCategoryFilter().setItems(FXCollections.observableArrayList(categories));
                view.getPublisherFilter().setItems(FXCollections.observableArrayList(publishers));
            }
        };
        AppContext.getInstance().getExecutorService().submit(task);
    }

    public void loadBooks() {
        String query = view.getSearchField().getText();
        Category selectedCat = view.getCategoryFilter().getValue();
        Publisher selectedPub = view.getPublisherFilter().getValue();

        Long catId = selectedCat != null ? selectedCat.getId() : null;
        Long pubId = selectedPub != null ? selectedPub.getId() : null;

        view.getLoadingSpinner().setVisible(true);

        Task<List<Book>> task = new Task<>() {
            @Override
            protected List<Book> call() {
                return catalogService.searchBooks(query, catId, pubId);
            }
        };

        task.setOnSucceeded(e -> {
            view.getLoadingSpinner().setVisible(false);
            List<Book> books = task.getValue();
            view.getBookTable().setItems(FXCollections.observableArrayList(books));
            view.getCountLabel().setText(books.size() + " book title(s) registered");
        });

        task.setOnFailed(e -> {
            view.getLoadingSpinner().setVisible(false);
            log.error("Failed to load catalog books", task.getException());
        });

        AppContext.getInstance().getExecutorService().submit(task);
    }

    private void handleAddBook() {
        List<Author> authors = catalogService.getAllAuthors();
        List<Category> categories = catalogService.getAllCategories();
        List<Publisher> publishers = catalogService.getAllPublishers();

        BookDialog dialog = new BookDialog(null, authors, categories, publishers);
        Optional<Book> result = dialog.showAndWait();

        result.ifPresent(newBook -> {
            try {
                catalogService.createBook(newBook, dialog.getSelectedAuthorIds(), dialog.getSelectedCategoryIds());
                loadBooks();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", ex.getMessage());
            }
        });
    }

    private void handleEditBook(Book book) {
        if (!SessionManager.hasPermission(PermissionCode.BOOK_UPDATE)) {
            showAlert(Alert.AlertType.WARNING, "Permission Denied", "You do not have permission to edit book titles.");
            return;
        }

        List<Author> authors = catalogService.getAllAuthors();
        List<Category> categories = catalogService.getAllCategories();
        List<Publisher> publishers = catalogService.getAllPublishers();

        BookDialog dialog = new BookDialog(book, authors, categories, publishers);
        Optional<Book> result = dialog.showAndWait();

        result.ifPresent(updatedBook -> {
            try {
                catalogService.updateBook(updatedBook, dialog.getSelectedAuthorIds(), dialog.getSelectedCategoryIds());
                loadBooks();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Update Error", ex.getMessage());
            }
        });
    }

    private void handleArchiveBook(Book book) {
        if (!SessionManager.hasPermission(PermissionCode.BOOK_ARCHIVE)) {
            showAlert(Alert.AlertType.WARNING, "Permission Denied", "You do not have permission to archive book titles.");
            return;
        }

        try {
            catalogService.archiveBook(book.getId());
            loadBooks();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Archive Error", ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("LibraryOS — " + title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

