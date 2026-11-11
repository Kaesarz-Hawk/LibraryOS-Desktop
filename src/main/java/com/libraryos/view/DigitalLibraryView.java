package com.libraryos.view;

import com.libraryos.model.digital.Bookmark;
import com.libraryos.model.digital.DigitalBook;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class DigitalLibraryView extends BorderPane {

    private final TabPane tabPane;
    private final Tab catalogTab;
    private final Tab readerTab;

    // Catalog controls
    private final TextField searchField;
    private final Button searchBtn;
    private final Button refreshBtn;
    private final Button uploadBtn;
    private final TableView<DigitalBook> booksTable;
    private final Button readBtn;
    private final Button downloadBtn;
    private final Button deleteBtn;
    private final Label catalogStatusLabel;

    // Reader controls
    private final Button backToCatalogBtn;
    private final Button prevPageBtn;
    private final Button nextPageBtn;
    private final TextField pageNumberField;
    private final Label totalPagesLabel;
    private final Button zoomOutBtn;
    private final Button zoomInBtn;
    private final Label zoomLabel;
    private final Button addBookmarkBtn;
    private final Label readerProgressLabel;
    private final ImageView pdfPageImageView;
    private final ScrollPane pdfScrollPane;
    private final ListView<Bookmark> bookmarksListView;
    private final Button removeBookmarkBtn;

    public DigitalLibraryView() {
        setPadding(new Insets(24));
        setStyle("-fx-background-color: #F8FAFC;");

        // 1. Top Header Area
        VBox headerBox = new VBox(6);
        headerBox.setPadding(new Insets(0, 0, 16, 0));

        Label titleLabel = new Label("Digital Library & E-Books / ডিজিটাল লাইব্রেরি");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        Label subtitleLabel = new Label("Stream authorized digital PDFs, track reading progress, bookmark key pages, and manage e-book assets.");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        headerBox.getChildren().addAll(titleLabel, subtitleLabel);
        setTop(headerBox);

        // 2. Main TabPane
        tabPane = new TabPane();
        tabPane.getStyleClass().add("card-panel");

        // ==========================================
        // TAB 1: E-Books Catalog
        // ==========================================
        catalogTab = new Tab("E-Books Catalog / ডিজিটাল গ্রন্থতালিকা");
        catalogTab.setClosable(false);

        VBox catalogContent = new VBox(14);
        catalogContent.setPadding(new Insets(16));

        // Toolbar
        HBox catalogToolbar = new HBox(10);
        catalogToolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Search digital books by title, Bangla title, or file name...");
        searchField.setPrefWidth(380);
        searchField.getStyleClass().add("search-field");

        searchBtn = new Button("Search / খুঁজুন");
        searchBtn.getStyleClass().add("btn-secondary");

        refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        uploadBtn = new Button("+ Upload E-Book / নতুন ই-বুক");
        uploadBtn.getStyleClass().add("btn-primary");

        catalogToolbar.getChildren().addAll(searchField, searchBtn, refreshBtn, spacer1, uploadBtn);

        // Table
        booksTable = new TableView<>();
        booksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(booksTable, Priority.ALWAYS);

        TableColumn<DigitalBook, String> colTitle = new TableColumn<>("Book Title / শিরোনাম");
        colTitle.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getBookTitle() + (d.getValue().getBookTitleBn() != null && !d.getValue().getBookTitleBn().isBlank() ?
                        " (" + d.getValue().getBookTitleBn() + ")" : "")
        ));
        colTitle.setPrefWidth(300);

        TableColumn<DigitalBook, String> colFile = new TableColumn<>("File Name / ফাইল নাম");
        colFile.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getFileName()));
        colFile.setPrefWidth(200);

        TableColumn<DigitalBook, Integer> colPages = new TableColumn<>("Pages / পৃষ্ঠা");
        colPages.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getTotalPages()));
        colPages.setPrefWidth(90);

        TableColumn<DigitalBook, String> colSize = new TableColumn<>("Size / আকার");
        colSize.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getFormattedFileSize()));
        colSize.setPrefWidth(100);

        TableColumn<DigitalBook, String> colDownload = new TableColumn<>("Offline Access");
        colDownload.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().isDownloadAllowed() ? "Allowed / অনুমোদিত" : "Streaming Only / কেবল স্ট্রিমিং"
        ));
        colDownload.setPrefWidth(140);

        TableColumn<DigitalBook, String> colDate = new TableColumn<>("Uploaded Date / আপলোড");
        colDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getUploadedAt() != null ? d.getValue().getUploadedAt().toLocalDate().toString() : ""
        ));
        colDate.setPrefWidth(120);

        booksTable.getColumns().addAll(colTitle, colFile, colPages, colSize, colDownload, colDate);

        // Bottom Action Bar
        HBox catalogBottomBar = new HBox(12);
        catalogBottomBar.setAlignment(Pos.CENTER_LEFT);

        readBtn = new Button("📖 Open Reader / পড়ুন");
        readBtn.getStyleClass().add("btn-primary");

        downloadBtn = new Button("⬇ Download PDF / ডাউনলোড");
        downloadBtn.getStyleClass().add("btn-secondary");

        deleteBtn = new Button("🗑 Delete / মুছুন");
        deleteBtn.getStyleClass().add("btn-danger");

        catalogStatusLabel = new Label("Ready.");
        catalogStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        catalogBottomBar.getChildren().addAll(readBtn, downloadBtn, deleteBtn, spacer2, catalogStatusLabel);

        catalogContent.getChildren().addAll(catalogToolbar, booksTable, catalogBottomBar);
        catalogTab.setContent(catalogContent);

        // ==========================================
        // TAB 2: Interactive PDF Reader
        // ==========================================
        readerTab = new Tab("Interactive Reader / পিডিএফ রিডার");
        readerTab.setClosable(false);

        BorderPane readerLayout = new BorderPane();

        // Top Reader Toolbar
        HBox readerToolbar = new HBox(10);
        readerToolbar.setAlignment(Pos.CENTER_LEFT);
        readerToolbar.setPadding(new Insets(10, 14, 10, 14));
        readerToolbar.getStyleClass().add("toolbar-panel");

        backToCatalogBtn = new Button("← Back / তালিকায় ফিরুন");
        backToCatalogBtn.getStyleClass().add("btn-secondary");

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);

        prevPageBtn = new Button("◄ Prev");
        prevPageBtn.getStyleClass().add("btn-secondary");

        pageNumberField = new TextField("1");
        pageNumberField.setPrefWidth(55);
        pageNumberField.setAlignment(Pos.CENTER);

        totalPagesLabel = new Label("of 1");
        totalPagesLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        nextPageBtn = new Button("Next ►");
        nextPageBtn.getStyleClass().add("btn-secondary");

        Separator sep2 = new Separator(javafx.geometry.Orientation.VERTICAL);

        zoomOutBtn = new Button(" - ");
        zoomOutBtn.getStyleClass().add("btn-secondary");

        zoomLabel = new Label("100%");
        zoomLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        zoomInBtn = new Button(" + ");
        zoomInBtn.getStyleClass().add("btn-secondary");

        Separator sep3 = new Separator(javafx.geometry.Orientation.VERTICAL);

        addBookmarkBtn = new Button("🔖 Add Bookmark");
        addBookmarkBtn.getStyleClass().add("btn-secondary");

        Region spacer3 = new Region();
        HBox.setHgrow(spacer3, Priority.ALWAYS);

        readerProgressLabel = new Label("Progress: 0%");
        readerProgressLabel.getStyleClass().add("badge-issued");

        readerToolbar.getChildren().addAll(
                backToCatalogBtn, sep1, prevPageBtn, pageNumberField, totalPagesLabel, nextPageBtn,
                sep2, zoomOutBtn, zoomLabel, zoomInBtn, sep3, addBookmarkBtn, spacer3, readerProgressLabel
        );
        readerLayout.setTop(readerToolbar);

        // Center: PDF ScrollPane & ImageView
        pdfPageImageView = new ImageView();
        pdfPageImageView.setPreserveRatio(true);
        pdfPageImageView.setSmooth(true);

        StackPane imageHolder = new StackPane(pdfPageImageView);
        imageHolder.setAlignment(Pos.CENTER);
        imageHolder.setPadding(new Insets(20));
        imageHolder.setStyle("-fx-background-color: #475569;");

        pdfScrollPane = new ScrollPane(imageHolder);
        pdfScrollPane.setFitToWidth(true);
        pdfScrollPane.setFitToHeight(true);
        readerLayout.setCenter(pdfScrollPane);

        // Right: Bookmarks Sidebar
        VBox bookmarkSidebar = new VBox(10);
        bookmarkSidebar.setPrefWidth(260);
        bookmarkSidebar.setPadding(new Insets(14));
        bookmarkSidebar.getStyleClass().add("card-panel");

        Label bmHeader = new Label("Bookmarks / বুকমার্ক");
        bmHeader.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #0F172A;");

        bookmarksListView = new ListView<>();
        bookmarksListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Bookmark item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Page " + item.getPageNumber() + ": " + item.getTitle());
                }
            }
        });
        VBox.setVgrow(bookmarksListView, Priority.ALWAYS);

        removeBookmarkBtn = new Button("Remove Bookmark / মুছুন");
        removeBookmarkBtn.getStyleClass().add("btn-danger");
        removeBookmarkBtn.setMaxWidth(Double.MAX_VALUE);

        bookmarkSidebar.getChildren().addAll(bmHeader, bookmarksListView, removeBookmarkBtn);
        readerLayout.setRight(bookmarkSidebar);

        readerTab.setContent(readerLayout);

        tabPane.getTabs().addAll(catalogTab, readerTab);
        setCenter(tabPane);
    }

    // Getters for Controller
    public TabPane getTabPane() { return tabPane; }
    public Tab getCatalogTab() { return catalogTab; }
    public Tab getReaderTab() { return readerTab; }

    public TextField getSearchField() { return searchField; }
    public Button getSearchBtn() { return searchBtn; }
    public Button getRefreshBtn() { return refreshBtn; }
    public Button getUploadBtn() { return uploadBtn; }
    public TableView<DigitalBook> getBooksTable() { return booksTable; }
    public Button getReadBtn() { return readBtn; }
    public Button getDownloadBtn() { return downloadBtn; }
    public Button getDeleteBtn() { return deleteBtn; }
    public Label getCatalogStatusLabel() { return catalogStatusLabel; }

    public Button getBackToCatalogBtn() { return backToCatalogBtn; }
    public Button getPrevPageBtn() { return prevPageBtn; }
    public Button getNextPageBtn() { return nextPageBtn; }
    public TextField getPageNumberField() { return pageNumberField; }
    public Label getTotalPagesLabel() { return totalPagesLabel; }
    public Button getZoomOutBtn() { return zoomOutBtn; }
    public Button getZoomInBtn() { return zoomInBtn; }
    public Label getZoomLabel() { return zoomLabel; }
    public Button getAddBookmarkBtn() { return addBookmarkBtn; }
    public Label getReaderProgressLabel() { return readerProgressLabel; }
    public ImageView getPdfPageImageView() { return pdfPageImageView; }
    public ListView<Bookmark> getBookmarksListView() { return bookmarksListView; }
    public Button getRemoveBookmarkBtn() { return removeBookmarkBtn; }
}

