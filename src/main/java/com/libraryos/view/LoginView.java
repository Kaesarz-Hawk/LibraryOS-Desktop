package com.libraryos.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

/**
 * Modern JavaFX Login Screen with Bangla/English localization and high-contrast responsive layout.
 */
public class LoginView extends StackPane {

    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final Label errorLabel;
    private final ProgressIndicator progressIndicator;
    private final Label dbStatusLabel;

    public LoginView() {
        getStyleClass().add("login-root");
        setPadding(new Insets(40));

        // Center card container
        VBox card = new VBox(20);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(460);
        card.setAlignment(Pos.TOP_LEFT);

        // Header: Brand & Institution
        VBox headerBox = new VBox(6);
        Label brandTitle = new Label("LibraryOS");
        brandTitle.getStyleClass().add("login-brand-title");

        Label brandSubtitle = new Label("Institutional Library Management System");
        brandSubtitle.getStyleClass().add("login-brand-subtitle");

        Label institutionLabel = new Label("বাংলাদেশ ইউনিভার্সিটি অব বিজনেস অ্যান্ড টেকনোলজি • BUBT Central Library");
        institutionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2563EB; -fx-font-weight: bold;");

        Label creditLabel = new Label("LibraryOS — BUBT CSE Edition • Developed by Kawsar Hoque, Dept. of CSE");
        creditLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        headerBox.getChildren().addAll(brandTitle, brandSubtitle, institutionLabel, creditLabel);

        // Separator
        Separator sep = new Separator();
        sep.setPadding(new Insets(4, 0, 4, 0));

        // Error Banner
        errorLabel = new Label();
        errorLabel.getStyleClass().add("banner-error");
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Form fields
        VBox form = new VBox(16);

        // Username
        VBox userBox = new VBox(6);
        Label userLabel = new Label("Username or ID / ব্যবহারকারী নাম");
        userLabel.getStyleClass().add("form-label");
        usernameField = new TextField();
        usernameField.setPromptText("Enter your username (e.g. admin)");
        usernameField.getStyleClass().add("form-input");
        usernameField.setText("admin"); // Default convenience for Phase 1 demo
        userBox.getChildren().addAll(userLabel, usernameField);

        // Password
        VBox passBox = new VBox(6);
        Label passLabel = new Label("Password / পাসওয়ার্ড");
        passLabel.getStyleClass().add("form-label");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("form-input");
        passwordField.setText("Admin@1234"); // Default convenience for Phase 1 demo
        passBox.getChildren().addAll(passLabel, passwordField);

        // Progress spinner
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(24, 24);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);

        // Login Button
        loginButton = new Button("Sign In / প্রবেশ করুন");
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(42);

        HBox btnBox = new HBox(10, loginButton, progressIndicator);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(loginButton, Priority.ALWAYS);

        form.getChildren().addAll(userBox, passBox, btnBox);

        // Footer / DB status & initial hints
        VBox footerBox = new VBox(8);
        footerBox.setPadding(new Insets(10, 0, 0, 0));

        dbStatusLabel = new Label();
        dbStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        dbStatusLabel.setWrapText(true);

        Label hintLabel = new Label("Default initial Super Admin: admin  |  Password: Admin@1234");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        footerBox.getChildren().addAll(dbStatusLabel, hintLabel);

        card.getChildren().addAll(headerBox, sep, errorLabel, form, footerBox);
        getChildren().add(card);
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Label getErrorLabel() {
        return errorLabel;
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    public Label getDbStatusLabel() {
        return dbStatusLabel;
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        progressIndicator.setVisible(loading);
        progressIndicator.setManaged(loading);
    }
}

