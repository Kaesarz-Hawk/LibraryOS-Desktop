package com.libraryos.app;

import com.libraryos.controller.LoginController;
import com.libraryos.controller.MainShellController;
import com.libraryos.view.LoginView;
import com.libraryos.view.MainShellView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryOSApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(LibraryOSApplication.class);

    private Stage primaryStage;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        log.info("Starting LibraryOS Desktop Application...");

        // Ensure AppContext is initialized
        AppContext.getInstance();

        primaryStage.setTitle("LibraryOS — Library Management System");
        primaryStage.setMinWidth(1280);
        primaryStage.setMinHeight(760);
        primaryStage.setWidth(1440);
        primaryStage.setHeight(860);

        // Show login view initially
        showLoginView();

        primaryStage.setOnCloseRequest(event -> {
            log.info("Application close requested. Shutting down resources...");
            AppContext.shutdown();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    public void showLoginView() {
        LoginView loginView = new LoginView();
        LoginController loginController = new LoginController(loginView);

        loginController.setOnLoginSuccess(session -> {
            showMainShellView();
        });

        if (scene == null) {
            scene = new Scene(loginView, 1440, 860);
            com.libraryos.view.theme.ThemeManager.getInstance().registerScene(scene);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(loginView);
            com.libraryos.view.theme.ThemeManager.getInstance().applyCurrentTheme(scene);
        }
    }

    public void showMainShellView() {
        MainShellView shellView = new MainShellView();
        MainShellController shellController = new MainShellController(shellView);

        shellController.setOnLogoutRequested(() -> {
            showLoginView();
        });

        scene.setRoot(shellView);
        com.libraryos.view.theme.ThemeManager.getInstance().applyCurrentTheme(scene);
    }

    private void applyTheme(Scene scene) {
        try {
            String cssPath = getClass().getResource("/css/theme.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            log.info("Applied theme stylesheet: {}", cssPath);
        } catch (Exception e) {
            log.warn("Could not load /css/theme.css: {}", e.getMessage());
        }
    }

    @Override
    public void stop() {
        log.info("Stopping LibraryOS JavaFX Application...");
        AppContext.shutdown();
    }
}

