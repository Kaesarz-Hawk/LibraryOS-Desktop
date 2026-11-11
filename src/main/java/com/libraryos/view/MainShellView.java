package com.libraryos.view;

import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.view.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsive Application Shell with 3D Depth, Live Theme Switcher,
 * Left Sidebar, Top Bar, and dynamic Content Workspace.
 */
public class MainShellView extends BorderPane {

    private final VBox sidebar;
    private final VBox navItemsBox;
    private final HBox topHeader;
    private final StackPane contentContainer;

    // Header controls
    private final TextField searchField;
    private final ContextMenu searchSuggestionsPopup;
    private final Button aiAssistantBtn;
    private final Label dateLabel;
    private final Label userNameLabel;
    private final Label userRoleLabel;
    private final Button logoutButton;

    // Live Theme Switcher Buttons (2-Option Toggle: Light & Dark)
    private final Button lightThemeBtn;
    private final Button darkThemeBtn;

    // Nav buttons map
    private final Map<String, Button> navButtons = new HashMap<>();
    private String activeNavKey = "DASHBOARD";

    public MainShellView() {
        getStyleClass().add("app-shell");

        // 1. Sidebar (Left, 285px) with 3D real depth
        sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setMinWidth(285);
        sidebar.setPrefWidth(285);
        sidebar.setMaxWidth(285);

        VBox brandBox = new VBox(4);
        brandBox.setPadding(new Insets(6, 10, 20, 10));

        Label brandTitle = new Label("LibraryOS");
        brandTitle.getStyleClass().add("sidebar-brand-title");

        Label brandSub = new Label("BUBT Central Library • CSE Edition");
        brandSub.getStyleClass().add("sidebar-brand-subtitle");

        brandBox.getChildren().addAll(brandTitle, brandSub);

        Separator brandSep = new Separator();
        brandSep.setStyle("-fx-background-color: #334155;");

        // Scrollable nav list
        ScrollPane navScroll = new ScrollPane();
        navScroll.setFitToWidth(true);
        navScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        navItemsBox = new VBox(6);
        navItemsBox.setPadding(new Insets(14, 0, 14, 0));
        navScroll.setContent(navItemsBox);
        VBox.setVgrow(navScroll, Priority.ALWAYS);

        sidebar.getChildren().addAll(brandBox, brandSep, navScroll);
        setLeft(sidebar);

        // 2. Top Header Bar (64px)
        topHeader = new HBox(14);
        topHeader.getStyleClass().add("top-header");

        searchField = new TextField();
        searchField.setPromptText("Quick search books, BUBT courses, barcodes... (Ctrl+K)");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(340);

        searchSuggestionsPopup = new ContextMenu();
        searchSuggestionsPopup.setAutoHide(true);

        aiAssistantBtn = new Button("🤖 AI Assistant / এআই সহায়ক");
        aiAssistantBtn.getStyleClass().add("btn-ai-sparkle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 2-Option Theme Switcher Widget (Light / Dark)
        HBox themeSwitchBox = new HBox(4);
        themeSwitchBox.getStyleClass().add("theme-switch-box");

        lightThemeBtn = new Button("☀️ Light");
        lightThemeBtn.getStyleClass().add("theme-pill-btn");

        darkThemeBtn = new Button("🌙 Dark");
        darkThemeBtn.getStyleClass().add("theme-pill-btn");

        themeSwitchBox.getChildren().addAll(lightThemeBtn, darkThemeBtn);

        // Wire Theme Switching
        lightThemeBtn.setOnAction(e -> {
            ThemeManager.getInstance().setTheme(ThemeManager.Theme.LIGHT);
            updateThemeButtons();
        });
        darkThemeBtn.setOnAction(e -> {
            ThemeManager.getInstance().setTheme(ThemeManager.Theme.DARK);
            updateThemeButtons();
        });

        ThemeManager.getInstance().addListener(this::updateThemeButtons);
        updateThemeButtons();

        // Date & Holiday indicator
        LocalDate today = LocalDate.now();
        String dateText = DateTimeUtil.formatDate(today);
        if (DateTimeUtil.isBangladeshWeeklyHoliday(today)) {
            dateText += " (Weekly Holiday / ছুটির দিন)";
        }
        dateLabel = new Label(dateText);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-weight: 500;");

        // User Pill with high contrast
        HBox userPill = new HBox(8);
        userPill.getStyleClass().add("user-pill");
        userPill.setAlignment(Pos.CENTER);

        userNameLabel = new Label("Kawsar");
        userNameLabel.getStyleClass().add("user-name-label");

        userRoleLabel = new Label("Super Admin");
        userRoleLabel.getStyleClass().add("user-role-badge");

        userPill.getChildren().addAll(userNameLabel, userRoleLabel);

        // Logout Button
        logoutButton = new Button("Sign Out");
        logoutButton.getStyleClass().add("btn-secondary");

        topHeader.getChildren().addAll(
                searchField,
                aiAssistantBtn,
                spacer,
                themeSwitchBox,
                dateLabel,
                userPill,
                logoutButton
        );
        setTop(topHeader);

        // 3. Main Dynamic Content Container
        contentContainer = new StackPane();
        contentContainer.getStyleClass().add("content-container");
        setCenter(contentContainer);

        // Build navigation items based on current session
        buildNavigation();
    }

    public void updateThemeButtons() {
        ThemeManager.Theme current = ThemeManager.getInstance().getCurrentTheme();
        lightThemeBtn.getStyleClass().remove("active");
        darkThemeBtn.getStyleClass().remove("active");

        if (current == ThemeManager.Theme.DARK) {
            darkThemeBtn.getStyleClass().add("active");
        } else {
            lightThemeBtn.getStyleClass().add("active");
        }
    }

    public void updateUserInfo(UserSession session) {
        if (session != null) {
            String name = session.getFullName();
            if (name == null || name.isBlank() || name.contains("Super Administrator") || name.contains("admin")) {
                name = "Kawsar";
            }
            if (session.getFullNameBn() != null && !session.getFullNameBn().isBlank()) {
                name += " (" + session.getFullNameBn() + ")";
            }
            userNameLabel.setText(name);

            String role = session.getPrimaryRoleName() != null ? session.getPrimaryRoleName() : "Super Admin";
            if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
                role = "Super Admin";
            }
            userRoleLabel.setText(role);
        }
        buildNavigation();
    }

    private void buildNavigation() {
        navItemsBox.getChildren().clear();
        navButtons.clear();

        addNavHeader("OPERATIONS / কার্যক্রম");

        // Dashboard
        if (SessionManager.hasPermission(PermissionCode.DASHBOARD_STAFF_VIEW) ||
            SessionManager.hasPermission(PermissionCode.DASHBOARD_MEMBER_VIEW)) {
            addNavItem("DASHBOARD", "⊞  Dashboard / ড্যাশবোর্ড", true);
        }

        // Circulation Desk
        if (SessionManager.hasPermission(PermissionCode.LOAN_ISSUE) ||
            SessionManager.hasPermission(PermissionCode.LOAN_RETURN)) {
            addNavItem("CIRCULATION", "⇄  Circulation Desk / লেনদেন", false);
        }

        // Reservations
        if (SessionManager.hasPermission(PermissionCode.RESERVATION_MANAGE) ||
            SessionManager.hasPermission(PermissionCode.RESERVATION_SELF)) {
            addNavItem("RESERVATIONS", "⏳  Reservations / সংরক্ষণ", false);
        }

        // Fines & Payments
        if (SessionManager.hasPermission(PermissionCode.FINE_VIEW) ||
            SessionManager.hasPermission(PermissionCode.PAYMENT_RECORD)) {
            addNavItem("FINANCE", "৳  Fines & Payments / জরিমানা", false);
        }

        addNavHeader("RESOURCES / সম্পদ ও ক্যাটালগ");

        // Books Catalog
        if (SessionManager.hasPermission(PermissionCode.BOOK_VIEW)) {
            addNavItem("CATALOG", "📚  Books Catalog / বই ক্যাটালগ", false);
        }

        // Physical Copies & Inventory
        if (SessionManager.hasPermission(PermissionCode.COPY_MANAGE)) {
            addNavItem("INVENTORY", "🏷  Physical Copies / বই কপি", false);
        }

        // Members
        if (SessionManager.hasPermission(PermissionCode.MEMBER_VIEW)) {
            addNavItem("MEMBERS", "👥  Members / সদস্যবৃন্দ", false);
        }

        // Digital Library
        if (SessionManager.hasPermission(PermissionCode.DIGITAL_READ)) {
            addNavItem("DIGITAL", "💻  Digital Library / ডিজিটাল লাইব্রেরি", false);
        }

        // Suppliers & Acquisitions
        if (SessionManager.hasPermission(PermissionCode.SUPPLIER_MANAGE)) {
            addNavItem("ACQUISITIONS", "📦  Acquisitions & Suppliers / সংগ্রহ", false);
        }

        addNavHeader("SYSTEM & REPORTS / প্রশাসন");

        // Reports
        if (SessionManager.hasPermission(PermissionCode.REPORT_VIEW)) {
            addNavItem("REPORTS", "📊  Reports & Analytics / প্রতিবেদন", false);
        }

        // Administration
        if (SessionManager.hasPermission(PermissionCode.USER_MANAGE) ||
            SessionManager.hasPermission(PermissionCode.SETTINGS_MANAGE) ||
            SessionManager.hasPermission(PermissionCode.AUDIT_VIEW)) {
            addNavItem("ADMIN", "⚙  System Administration / প্রশাসন", false);
        }
    }

    private void addNavHeader(String title) {
        Label header = new Label(title);
        header.getStyleClass().add("sidebar-nav-header");
        navItemsBox.getChildren().add(header);
    }

    private void addNavItem(String key, String title, boolean active) {
        Button btn = new Button(title);
        btn.getStyleClass().add("sidebar-nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setWrapText(true);
        btn.setTextOverrun(OverrunStyle.CLIP);

        if (active) {
            btn.getStyleClass().add("active");
            activeNavKey = key;
        }

        navButtons.put(key, btn);
        navItemsBox.getChildren().add(btn);
    }

    public void setActiveNav(String key) {
        this.activeNavKey = key;
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            if (entry.getKey().equals(key)) {
                if (!entry.getValue().getStyleClass().contains("active")) {
                    entry.getValue().getStyleClass().add("active");
                }
            } else {
                entry.getValue().getStyleClass().remove("active");
            }
        }
    }

    public void setMainContent(Node content) {
        contentContainer.getChildren().setAll(content);
    }

    public Map<String, Button> getNavButtons() {
        return navButtons;
    }

    public Button getLogoutButton() {
        return logoutButton;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public ContextMenu getSearchSuggestionsPopup() {
        return searchSuggestionsPopup;
    }

    public Button getAiAssistantBtn() {
        return aiAssistantBtn;
    }
}
