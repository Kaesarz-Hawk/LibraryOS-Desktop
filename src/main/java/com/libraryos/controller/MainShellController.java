package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.catalog.Book;
import com.libraryos.service.AiAssistantService;
import com.libraryos.view.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MainShellController {

    private static final Logger log = LoggerFactory.getLogger(MainShellController.class);

    private final MainShellView view;
    private Runnable onLogoutRequested;

    private DashboardView dashboardView;
    private DashboardController dashboardController;

    private BookCatalogView catalogView;
    private BookCatalogController catalogController;

    private InventoryView inventoryView;
    private InventoryController inventoryController;

    private MemberDirectoryView memberView;
    private MemberDirectoryController memberController;

    private CirculationDeskView circulationView;
    private CirculationDeskController circulationController;

    private ReservationsView reservationsView;
    private ReservationsController reservationsController;

    private FinanceDeskView financeDeskView;
    private FinanceDeskController financeDeskController;

    private ReportsView reportsView;
    private ReportsController reportsController;

    private AdminDeskView adminDeskView;
    private AdminDeskController adminDeskController;

    public MainShellController(MainShellView view) {
        this.view = view;
        initialize();
    }

    private void initialize() {
        // Wire logout
        view.getLogoutButton().setOnAction(e -> {
            AppContext.getInstance().getAuthService().logout();
            if (onLogoutRequested != null) {
                onLogoutRequested.run();
            }
        });

        // Wire AI Assistant dialog
        view.getAiAssistantBtn().setOnAction(e -> openAiDialog(null));

        // Wire Header Search Autocomplete with Real-Time Stock & Shelf Location
        setupHeaderLiveSearch();

        // Wire Global Keyboard Shortcuts (Ctrl+K focus, Escape close)
        setupGlobalKeyShortcuts();

        // Initialize dashboard view as primary home
        dashboardView = new DashboardView();
        dashboardController = new DashboardController(dashboardView);
        view.setMainContent(dashboardView);

        // Connect quick action buttons on dashboard to real screens
        dashboardView.getIssueBookBtn().setOnAction(e -> handleNavigation("CIRCULATION"));
        dashboardView.getReturnBookBtn().setOnAction(e -> {
            handleNavigation("CIRCULATION");
            circulationView.getTabPane().getSelectionModel().select(circulationView.getReturnTab());
        });
        dashboardView.getAddBookBtn().setOnAction(e -> handleNavigation("CATALOG"));
        dashboardView.getAddMemberBtn().setOnAction(e -> handleNavigation("MEMBERS"));

        // Initialize Phase 2 & 3 views & controllers lazily or directly
        catalogView = new BookCatalogView();
        catalogController = new BookCatalogController(catalogView);

        inventoryView = new InventoryView();
        inventoryController = new InventoryController(inventoryView);

        memberView = new MemberDirectoryView();
        memberController = new MemberDirectoryController(memberView);

        circulationView = new CirculationDeskView();
        circulationController = new CirculationDeskController(circulationView);

        reservationsView = new ReservationsView();
        reservationsController = new ReservationsController(reservationsView);

        financeDeskView = new FinanceDeskView();
        financeDeskController = new FinanceDeskController(financeDeskView);

        reportsView = new ReportsView();
        reportsController = new ReportsController(reportsView);

        adminDeskView = new AdminDeskView();
        adminDeskController = new AdminDeskController(adminDeskView);

        // Wire navigation button clicks
        view.getNavButtons().forEach((key, btn) -> {
            btn.setOnAction(e -> handleNavigation(key));
        });
    }

    public void setOnLogoutRequested(Runnable callback) {
        this.onLogoutRequested = callback;
    }

    public void refreshSession(UserSession session) {
        view.updateUserInfo(session);
        view.getNavButtons().forEach((key, btn) -> {
            btn.setOnAction(e -> handleNavigation(key));
        });
        view.setActiveNav("DASHBOARD");
        view.setMainContent(dashboardView);
        dashboardController.loadDashboardData();
    }

    private void handleNavigation(String key) {
        view.setActiveNav(key);
        switch (key) {
            case "DASHBOARD" -> {
                view.setMainContent(dashboardView);
                dashboardController.loadDashboardData();
            }
            case "CATALOG" -> {
                view.setMainContent(catalogView);
                catalogController.loadBooks();
            }
            case "INVENTORY" -> {
                view.setMainContent(inventoryView);
                inventoryController.loadCopies();
            }
            case "MEMBERS" -> {
                view.setMainContent(memberView);
                memberController.loadMembers();
            }
            case "CIRCULATION" -> {
                view.setMainContent(circulationView);
            }
            case "RESERVATIONS" -> {
                view.setMainContent(reservationsView);
                reservationsController.loadReservations();
            }
            case "FINANCE" -> {
                view.setMainContent(financeDeskView);
            }
            case "REPORTS" -> {
                view.setMainContent(reportsView);
                reportsController.loadOverdueReport();
            }
            case "DIGITAL" -> view.setMainContent(createPhasePlaceholder(
                    "Digital Library / ডিজিটাল লাইব্রেরি",
                    "Phase 6 — Digital Library & Acquisitions",
                    "Managed e-books, local PDF storage, reading progress bookmarks, and built-in PDF viewer."));
            case "ADMIN" -> {
                view.setMainContent(adminDeskView);
                adminDeskController.loadAuditLogs();
            }
            default -> view.setMainContent(dashboardView);
        }
    }

    private Node createPhasePlaceholder(String title, String phaseTag, String description) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 40, 60, 40));
        box.getStyleClass().add("card-panel");

        Label tag = new Label(phaseTag);
        tag.getStyleClass().add("badge-issued");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800;");
        titleLabel.getStyleClass().add("page-title");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");
        descLabel.getStyleClass().add("page-subtitle");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(600);

        Label notice = new Label("Phase 1 & Phase 2 (Catalog, Copies & Members) are verified and running.");
        notice.setStyle("-fx-font-size: 11px; -fx-text-fill: #16A34A; -fx-font-weight: bold;");

        Button backBtn = new Button("← Return to Dashboard");
        backBtn.getStyleClass().add("btn-primary");
        backBtn.setOnAction(e -> handleNavigation("DASHBOARD"));

        box.getChildren().addAll(tag, titleLabel, descLabel, notice, backBtn);

        VBox wrapper = new VBox(box);
        wrapper.setPadding(new Insets(28));
        wrapper.getStyleClass().add("content-area");
        VBox.setVgrow(box, Priority.ALWAYS);
        return wrapper;
    }

    private void openAiDialog(String initialQuery) {
        AiAssistantDialog dialog = new AiAssistantDialog(view.getScene().getWindow());
        dialog.setOnIssueRequested(book -> {
            dialog.close();
            handleNavigation("CIRCULATION");
            circulationView.getTabPane().getSelectionModel().select(circulationView.getIssueTab());
        });
        dialog.setOnViewDetailsRequested(book -> {
            dialog.close();
            handleNavigation("CATALOG");
            catalogView.getSearchField().setText(book.getTitle());
            catalogController.loadBooks();
        });
        dialog.setOnReserveRequested(book -> {
            dialog.close();
            handleNavigation("RESERVATIONS");
        });
        dialog.show();
        if (initialQuery != null && !initialQuery.isBlank()) {
            dialog.askQuestion(initialQuery);
        }
    }

    private void setupHeaderLiveSearch() {
        view.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().length() < 2) {
                view.getSearchSuggestionsPopup().hide();
                return;
            }

            String query = newVal.trim();
            List<AiAssistantService.BookStockInfo> suggestions =
                    AppContext.getInstance().getAiAssistantService().searchLiveSuggestions(query);

            if (suggestions.isEmpty()) {
                view.getSearchSuggestionsPopup().hide();
                return;
            }

            view.getSearchSuggestionsPopup().getItems().clear();
            boolean isDark = com.libraryos.view.theme.ThemeManager.getInstance().isDarkMode();
            String titleColor = isDark ? "#FFFFFF" : "#0F172A";
            String subColor = isDark ? "#DDD6FE" : "#64748B";
            String shelfBg = isDark ? "#2A1B0E" : "#FEF3C7";
            String shelfText = isDark ? "#FDE047" : "#92400E";
            String askAiColor = isDark ? "#A78BFA" : "#2563EB";

            for (AiAssistantService.BookStockInfo stock : suggestions) {
                Book b = stock.book();
                VBox itemBox = new VBox(3);
                itemBox.setPrefWidth(420);
                itemBox.setPadding(new Insets(6, 10, 6, 10));

                HBox topRow = new HBox(8);
                topRow.setAlignment(Pos.CENTER_LEFT);

                Label titleLbl = new Label(b.getTitle());
                titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13.5px; -fx-text-fill: " + titleColor + ";");
                HBox.setHgrow(titleLbl, Priority.ALWAYS);

                Label stockPill = new Label(stock.inStock() ?
                        "🟢 In Stock (" + stock.availableCopies() + ")" : "🔴 Out of Stock");
                stockPill.setStyle(stock.inStock() ?
                        (isDark ? "-fx-background-color: #064E3B; -fx-text-fill: #6EE7B7; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 8px;" :
                                  "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 8px;") :
                        (isDark ? "-fx-background-color: #451A25; -fx-text-fill: #FDA4AF; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 8px;" :
                                  "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 8px;"));

                topRow.getChildren().addAll(titleLbl, stockPill);

                HBox bottomRow = new HBox(8);
                bottomRow.setAlignment(Pos.CENTER_LEFT);

                String author = (b.getAuthors() != null && !b.getAuthors().isEmpty()) ?
                        b.getAuthors().get(0).getNameEn() : "Academic Reference";
                Label authorLbl = new Label("✍ " + author);
                authorLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + subColor + ";");

                Label shelfLbl = new Label("📍 " + stock.shelfLocation());
                shelfLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + shelfText + "; -fx-background-color: " + shelfBg + "; -fx-padding: 2 6 2 6; -fx-background-radius: 6px;");

                bottomRow.getChildren().addAll(authorLbl, shelfLbl);

                if (stock.courseCodeTag() != null) {
                    Label courseBadge = new Label("🎓 " + stock.courseCodeTag());
                    courseBadge.setStyle(isDark ?
                            "-fx-font-size: 10px; -fx-text-fill: #DDD6FE; -fx-background-color: #3B1D6E; -fx-padding: 2 6 2 6; -fx-background-radius: 6px;" :
                            "-fx-font-size: 10px; -fx-text-fill: #3730A3; -fx-background-color: #E0E7FF; -fx-padding: 2 6 2 6; -fx-background-radius: 6px;");
                    bottomRow.getChildren().add(courseBadge);
                }

                itemBox.getChildren().addAll(topRow, bottomRow);

                CustomMenuItem menuItem = new CustomMenuItem(itemBox);
                menuItem.setOnAction(e -> {
                    handleNavigation("CATALOG");
                    catalogView.getSearchField().setText(b.getTitle());
                    catalogController.loadBooks();
                });
                view.getSearchSuggestionsPopup().getItems().add(menuItem);
            }

            SeparatorMenuItem sep = new SeparatorMenuItem();
            Label askAiLbl = new Label("🤖 Ask AI Assistant about \"" + query + "\" (Press Enter)");
            askAiLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + askAiColor + "; -fx-padding: 6 10 6 10; -fx-cursor: hand;");
            CustomMenuItem askAiItem = new CustomMenuItem(askAiLbl);
            askAiItem.setOnAction(e -> openAiDialog(query));

            view.getSearchSuggestionsPopup().getItems().addAll(sep, askAiItem);

            if (!view.getSearchSuggestionsPopup().isShowing() && view.getSearchField().getScene() != null) {
                view.getSearchSuggestionsPopup().show(view.getSearchField(), Side.BOTTOM, 0, 4);
            }
        });

        view.getSearchField().setOnAction(e -> {
            String query = view.getSearchField().getText().trim();
            view.getSearchSuggestionsPopup().hide();
            if (!query.isBlank()) {
                handleNavigation("CATALOG");
                catalogView.getSearchField().setText(query);
                catalogController.loadBooks();
            }
        });
    }

    private void setupGlobalKeyShortcuts() {
        // Handle when scene is attached or changed
        view.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                registerKeyFilter(newScene);
            }
        });
        if (view.getScene() != null) {
            registerKeyFilter(view.getScene());
        }
    }

    private void registerKeyFilter(javafx.scene.Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if ((event.isControlDown() || event.isMetaDown()) && event.getCode() == KeyCode.K) {
                view.getSearchField().requestFocus();
                view.getSearchField().selectAll();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                if (view.getSearchSuggestionsPopup().isShowing()) {
                    view.getSearchSuggestionsPopup().hide();
                    event.consume();
                }
            }
        });
    }
}
