package com.libraryos.view.theme;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Modern 2-Theme Switcher:
 * 1. Light Mode (Crisp Slate & Royal Blue)
 * 2. Dark Mode (Rich Cyberpunk Obsidian-Violet #0F0B1E with glowing purple accents)
 */
public class ThemeManager {

    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_KEY = "libraryos_active_theme";

    public enum Theme {
        LIGHT("☀️ Light", "light", "/css/theme.css", null),
        DARK("🌙 Dark", "dark", "/css/theme.css", "/css/theme-dark.css");

        private final String displayName;
        private final String id;
        private final String baseCss;
        private final String overrideCss;

        Theme(String displayName, String id, String baseCss, String overrideCss) {
            this.displayName = displayName;
            this.id = id;
            this.baseCss = baseCss;
            this.overrideCss = overrideCss;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getId() {
            return id;
        }

        public String getBaseCss() {
            return baseCss;
        }

        public String getOverrideCss() {
            return overrideCss;
        }
    }

    private static final ThemeManager instance = new ThemeManager();
    private Theme currentTheme = Theme.LIGHT;
    private final List<Scene> scenes = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private ThemeManager() {
        String saved = prefs.get(PREF_KEY, "LIGHT");
        try {
            if ("PURPLE".equalsIgnoreCase(saved)) {
                this.currentTheme = Theme.DARK;
            } else {
                this.currentTheme = Theme.valueOf(saved.toUpperCase());
            }
        } catch (Exception e) {
            this.currentTheme = Theme.LIGHT;
        }
    }

    public static ThemeManager getInstance() {
        return instance;
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    public void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void registerScene(Scene scene) {
        if (scene != null && !scenes.contains(scene)) {
            scenes.add(scene);
            applyThemeToScene(scene, currentTheme);
        }
    }

    public void setTheme(Theme theme) {
        if (theme == null) return;
        this.currentTheme = theme;
        try {
            prefs.put(PREF_KEY, theme.name());
        } catch (Exception ignored) {}

        // Prune disposed scenes
        scenes.removeIf(s -> s == null || s.getWindow() == null);
        for (Scene s : scenes) {
            applyThemeToScene(s, theme);
        }

        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception e) {
                log.error("Theme change listener error", e);
            }
        }
    }

    public void applyCurrentTheme(Scene scene) {
        applyThemeToScene(scene, currentTheme);
    }

    public void applyThemeToScene(Scene scene, Theme theme) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        try {
            String base = getClass().getResource(theme.getBaseCss()).toExternalForm();
            scene.getStylesheets().add(base);

            if (theme.getOverrideCss() != null) {
                String override = getClass().getResource(theme.getOverrideCss()).toExternalForm();
                scene.getStylesheets().add(override);
            }
        } catch (Exception e) {
            log.warn("Could not apply theme {} to scene: {}", theme, e.getMessage());
        }
    }

    public void applyThemeToDialogPane(DialogPane pane) {
        if (pane == null) return;
        pane.getStylesheets().clear();
        try {
            pane.getStylesheets().add(getClass().getResource(currentTheme.getBaseCss()).toExternalForm());
            if (currentTheme.getOverrideCss() != null) {
                pane.getStylesheets().add(getClass().getResource(currentTheme.getOverrideCss()).toExternalForm());
            }
        } catch (Exception e) {
            log.warn("Could not apply theme to dialog: {}", e.getMessage());
        }
    }
}
