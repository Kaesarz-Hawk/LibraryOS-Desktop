package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.AuthenticationException;
import com.libraryos.core.security.UserSession;
import com.libraryos.service.AuthService;
import com.libraryos.view.LoginView;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.input.KeyCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private final LoginView view;
    private final AuthService authService;
    private Consumer<UserSession> onLoginSuccess;

    public LoginController(LoginView view) {
        this.view = view;
        this.authService = AppContext.getInstance().getAuthService();
        initialize();
    }

    private void initialize() {
        // Display database connectivity status
        if (DatabaseConfig.isDbAvailable()) {
            if (DatabaseConfig.isEmbedded()) {
                view.getDbStatusLabel().setText("⚡ Database: Embedded Local DB (Set MySQL password in config to switch)");
                view.getDbStatusLabel().setStyle("-fx-font-size: 11px; -fx-text-fill: #2563EB; -fx-font-weight: bold;");
            } else {
                view.getDbStatusLabel().setText("✓ Database connected (MySQL 8)");
                view.getDbStatusLabel().setStyle("-fx-font-size: 11px; -fx-text-fill: #16A34A;");
            }
        } else {
            view.getDbStatusLabel().setText("⚠ Database offline. Running in diagnostic mode.");
            view.getDbStatusLabel().setStyle("-fx-font-size: 11px; -fx-text-fill: #D97706;");
        }

        // Action handlers
        view.getLoginButton().setOnAction(e -> handleLogin());

        view.getPasswordField().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });

        view.getUsernameField().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                view.getPasswordField().requestFocus();
            }
        });
    }

    public void setOnLoginSuccess(Consumer<UserSession> callback) {
        this.onLoginSuccess = callback;
    }

    private void handleLogin() {
        String username = view.getUsernameField().getText();
        String password = view.getPasswordField().getText();

        view.clearError();

        if (username == null || username.isBlank()) {
            view.showError("Please enter your username.");
            view.getUsernameField().requestFocus();
            return;
        }

        if (password == null || password.isBlank()) {
            view.showError("Please enter your password.");
            view.getPasswordField().requestFocus();
            return;
        }

        view.setLoading(true);

        // Run authentication on background task to avoid blocking JavaFX UI thread
        Task<UserSession> loginTask = new Task<>() {
            @Override
            protected UserSession call() {
                return authService.login(username, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            view.setLoading(false);
            UserSession session = loginTask.getValue();
            log.info("Login succeeded for user: {}", session.getUsername());
            if (onLoginSuccess != null) {
                onLoginSuccess.accept(session);
            }
        });

        loginTask.setOnFailed(event -> {
            view.setLoading(false);
            Throwable error = loginTask.getException();
            log.warn("Login failed: {}", error.getMessage());

            if (error instanceof AuthenticationException authEx) {
                view.showError(authEx.getMessage());
            } else {
                view.showError("An unexpected error occurred during login. Please try again.");
            }
        });

        AppContext.getInstance().getExecutorService().submit(loginTask);
    }
}

