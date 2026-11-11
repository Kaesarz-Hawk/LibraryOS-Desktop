package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.catalog.Book;
import com.libraryos.model.digital.Bookmark;
import com.libraryos.model.digital.DigitalBook;
import com.libraryos.model.digital.ReadingProgress;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.service.CatalogService;
import com.libraryos.service.DigitalLibraryService;
import com.libraryos.view.DigitalLibraryView;
import com.libraryos.view.UploadDigitalBookDialog;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

public class DigitalLibraryController {

    private static final Logger log = LoggerFactory.getLogger(DigitalLibraryController.class);

    private final DigitalLibraryView view;
    private final DigitalLibraryService digitalLibraryService;
    private final CatalogService catalogService;

    private DigitalBook currentReadingBook;
    private int currentPageNumber = 1;
    private float currentZoomLevel = 1.0f;

    public DigitalLibraryController(DigitalLibraryView view) {
        this.view = view;
        this.digitalLibraryService = AppContext.getInstance().getDigitalLibraryService();
        this.catalogService = AppContext.getInstance().getCatalogService();
        initialize();
    }

    private void initialize() {
        // Enforce permissions on upload / delete
        boolean canManage = SessionManager.hasPermission(PermissionCode.DIGITAL_MANAGE);
        view.getUploadBtn().setVisible(canManage);
        view.getUploadBtn().setManaged(canManage);
        view.getDeleteBtn().setVisible(canManage);
        view.getDeleteBtn().setManaged(canManage);

        // Catalog tab wiring
        view.getSearchBtn().setOnAction(e -> loadDigitalBooks());
        view.getSearchField().setOnAction(e -> loadDigitalBooks());
        view.getRefreshBtn().setOnAction(e -> {
            view.getSearchField().clear();
            loadDigitalBooks();
        });

        view.getUploadBtn().setOnAction(e -> handleUploadDialog());
        view.getReadBtn().setOnAction(e -> {
            DigitalBook selected = view.getBooksTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                openBookInReader(selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an e-book from the catalog to read.");
            }
        });

        view.getDownloadBtn().setOnAction(e -> {
            DigitalBook selected = view.getBooksTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleDownload(selected);
            }
        });

        view.getDeleteBtn().setOnAction(e -> {
            DigitalBook selected = view.getBooksTable().getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleDelete(selected);
            }
        });

        // Double click row to open reader
        view.getBooksTable().setRowFactory(tv -> {
            var row = new javafx.scene.control.TableRow<DigitalBook>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openBookInReader(row.getItem());
                }
            });
            return row;
        });

        // Reader toolbar wiring
        view.getBackToCatalogBtn().setOnAction(e -> view.getTabPane().getSelectionModel().select(view.getCatalogTab()));

        view.getPrevPageBtn().setOnAction(e -> {
            if (currentReadingBook != null && currentPageNumber > 1) {
                renderPage(currentPageNumber - 1);
            }
        });

        view.getNextPageBtn().setOnAction(e -> {
            if (currentReadingBook != null && currentPageNumber < currentReadingBook.getTotalPages()) {
                renderPage(currentPageNumber + 1);
            }
        });

        view.getPageNumberField().setOnAction(e -> {
            try {
                int target = Integer.parseInt(view.getPageNumberField().getText().trim());
                if (currentReadingBook != null && target >= 1 && target <= currentReadingBook.getTotalPages()) {
                    renderPage(target);
                } else {
                    view.getPageNumberField().setText(String.valueOf(currentPageNumber));
                }
            } catch (NumberFormatException ignored) {
                view.getPageNumberField().setText(String.valueOf(currentPageNumber));
            }
        });

        view.getZoomInBtn().setOnAction(e -> {
            currentZoomLevel = Math.min(2.5f, currentZoomLevel * 1.25f);
            updateZoomDisplay();
            if (currentReadingBook != null) {
                renderPage(currentPageNumber);
            }
        });

        view.getZoomOutBtn().setOnAction(e -> {
            currentZoomLevel = Math.max(0.6f, currentZoomLevel / 1.25f);
            updateZoomDisplay();
            if (currentReadingBook != null) {
                renderPage(currentPageNumber);
            }
        });

        view.getAddBookmarkBtn().setOnAction(e -> handleAddBookmark());

        view.getBookmarksListView().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && currentReadingBook != null) {
                renderPage(newVal.getPageNumber());
            }
        });

        view.getRemoveBookmarkBtn().setOnAction(e -> {
            Bookmark bm = view.getBookmarksListView().getSelectionModel().getSelectedItem();
            if (bm != null) {
                Long memberId = resolveCurrentMemberId();
                if (memberId != null) {
                    digitalLibraryService.removeBookmark(bm.getId(), memberId);
                    refreshBookmarks();
                }
            }
        });

        loadDigitalBooks();
    }

    public void loadDigitalBooks() {
        String query = view.getSearchField().getText();
        view.getCatalogStatusLabel().setText("Loading e-books...");

        Task<List<DigitalBook>> task = new Task<>() {
            @Override
            protected List<DigitalBook> call() {
                if (query == null || query.isBlank()) {
                    return digitalLibraryService.getAllDigitalBooks();
                } else {
                    return digitalLibraryService.searchDigitalBooks(query);
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<DigitalBook> list = task.getValue();
            view.getBooksTable().getItems().setAll(list);
            view.getCatalogStatusLabel().setText("Total: " + list.size() + " digital resource(s) available.");
        });

        task.setOnFailed(e -> {
            log.error("Failed to load digital books", task.getException());
            view.getCatalogStatusLabel().setText("Failed to load digital resources.");
        });

        AppContext.getInstance().getExecutorService().submit(task);
    }

    public void openBookInReader(DigitalBook book) {
        this.currentReadingBook = book;
        view.getTotalPagesLabel().setText("of " + book.getTotalPages());

        // Check if there is saved reading progress for this member
        Long memberId = resolveCurrentMemberId();
        int initialPage = 1;
        if (memberId != null) {
            Optional<ReadingProgress> rp = digitalLibraryService.getReadingProgress(memberId, book.getId());
            if (rp.isPresent()) {
                initialPage = rp.get().getLastPageRead();
            }
        }

        view.getTabPane().getSelectionModel().select(view.getReaderTab());
        refreshBookmarks();
        renderPage(initialPage);
    }

    private void renderPage(int pageNumber) {
        if (currentReadingBook == null) return;
        this.currentPageNumber = pageNumber;
        view.getPageNumberField().setText(String.valueOf(pageNumber));

        // Calculate progress percentage
        int total = Math.max(1, currentReadingBook.getTotalPages());
        int pct = (int) Math.round(((double) pageNumber / total) * 100.0);
        view.getReaderProgressLabel().setText("Page " + pageNumber + " / " + total + " (" + pct + "%)");

        float dpi = 130f * currentZoomLevel;

        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                // 0-based page index for PDFBox renderer
                byte[] pngBytes = digitalLibraryService.renderPageToPng(currentReadingBook.getId(), pageNumber - 1, dpi);
                return new Image(new ByteArrayInputStream(pngBytes));
            }
        };

        task.setOnSucceeded(e -> {
            Image img = task.getValue();
            view.getPdfPageImageView().setImage(img);

            // Record reading progress asynchronously
            Long memberId = resolveCurrentMemberId();
            if (memberId != null) {
                AppContext.getInstance().getExecutorService().submit(() -> {
                    try {
                        digitalLibraryService.recordReadingProgress(memberId, currentReadingBook.getId(), pageNumber, total);
                    } catch (Exception ex) {
                        log.warn("Could not save reading progress: {}", ex.getMessage());
                    }
                });
            }
        });

        task.setOnFailed(e -> {
            log.error("Failed to render page {} for digital book {}", pageNumber, currentReadingBook.getId(), task.getException());
            showAlert(Alert.AlertType.ERROR, "Render Error", "Could not render page " + pageNumber + ": " + task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().submit(task);
    }

    private void refreshBookmarks() {
        if (currentReadingBook == null) return;
        Long memberId = resolveCurrentMemberId();
        if (memberId == null) return;

        Task<List<Bookmark>> task = new Task<>() {
            @Override
            protected List<Bookmark> call() {
                return digitalLibraryService.getBookmarks(memberId, currentReadingBook.getId());
            }
        };

        task.setOnSucceeded(e -> view.getBookmarksListView().getItems().setAll(task.getValue()));
        AppContext.getInstance().getExecutorService().submit(task);
    }

    private void handleAddBookmark() {
        if (currentReadingBook == null) return;
        Long memberId = resolveCurrentMemberId();
        if (memberId == null) {
            showAlert(Alert.AlertType.WARNING, "Bookmark Notice", "Bookmarks require an active library member profile.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("Page " + currentPageNumber + " Note");
        dialog.setTitle("Add Bookmark / বুকমার্ক যোগ করুন");
        dialog.setHeaderText("Add bookmark for page " + currentPageNumber + " of " + currentReadingBook.getBookTitle());
        dialog.setContentText("Bookmark Title / শিরোনাম:");

        dialog.showAndWait().ifPresent(title -> {
            if (!title.trim().isBlank()) {
                digitalLibraryService.addBookmark(memberId, currentReadingBook.getId(), currentPageNumber, title.trim(), null);
                refreshBookmarks();
            }
        });
    }

    private void handleUploadDialog() {
        Task<List<Book>> fetchBooksTask = new Task<>() {
            @Override
            protected List<Book> call() {
                return catalogService.getAllBooks();
            }
        };

        fetchBooksTask.setOnSucceeded(e -> {
            List<Book> books = fetchBooksTask.getValue();
            UploadDigitalBookDialog dialog = new UploadDigitalBookDialog(view.getScene().getWindow(), books);
            dialog.showAndWait().ifPresent(result -> {
                view.getCatalogStatusLabel().setText("Uploading and processing PDF...");

                Task<DigitalBook> uploadTask = new Task<>() {
                    @Override
                    protected DigitalBook call() throws Exception {
                        return digitalLibraryService.uploadDigitalBook(result.bookId(), result.file(), result.downloadAllowed());
                    }
                };

                uploadTask.setOnSucceeded(ev -> {
                    showAlert(Alert.AlertType.INFORMATION, "Upload Successful",
                            "E-book successfully uploaded: " + uploadTask.getValue().getFileName() +
                                    " (" + uploadTask.getValue().getTotalPages() + " pages)");
                    loadDigitalBooks();
                });

                uploadTask.setOnFailed(ev -> {
                    log.error("Failed to upload e-book", uploadTask.getException());
                    showAlert(Alert.AlertType.ERROR, "Upload Failed", uploadTask.getException().getMessage());
                    view.getCatalogStatusLabel().setText("Upload failed.");
                });

                AppContext.getInstance().getExecutorService().submit(uploadTask);
            });
        });

        AppContext.getInstance().getExecutorService().submit(fetchBooksTask);
    }

    private void handleDownload(DigitalBook book) {
        if (!book.isDownloadAllowed()) {
            showAlert(Alert.AlertType.WARNING, "Download Restricted",
                    "This digital publication is licensed for streaming/in-app reading only. Offline file export is disabled by library policy.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save E-Book PDF");
        chooser.setInitialFileName(book.getFileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));

        File target = chooser.showSaveDialog(view.getScene().getWindow());
        if (target != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    File src = new File(book.getFilePath());
                    if (!src.exists()) {
                        throw new IOException("Source physical file not found on server.");
                    }
                    Files.copy(src.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return null;
                }
            };

            task.setOnSucceeded(e -> showAlert(Alert.AlertType.INFORMATION, "Download Complete", "Saved e-book to: " + target.getAbsolutePath()));
            task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Download Failed", task.getException().getMessage()));
            AppContext.getInstance().getExecutorService().submit(task);
        }
    }

    private void handleDelete(DigitalBook book) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete digital book '" + book.getFileName() + "'? This will remove physical files and reading records.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Deletion");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return digitalLibraryService.deleteDigitalBook(book.getId());
                    }
                };
                task.setOnSucceeded(e -> {
                    showAlert(Alert.AlertType.INFORMATION, "Deleted", "Digital book removed from library.");
                    loadDigitalBooks();
                });
                task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Error", task.getException().getMessage()));
                AppContext.getInstance().getExecutorService().submit(task);
            }
        });
    }

    private void updateZoomDisplay() {
        int pct = Math.round(currentZoomLevel * 100f);
        view.getZoomLabel().setText(pct + "%");
    }

    private Long resolveCurrentMemberId() {
        UserSession session = SessionManager.getCurrentSession().orElse(null);
        if (session != null && session.getMemberId() != null) {
            return session.getMemberId();
        }
        return 1L; // Fallback to institutional default member
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

