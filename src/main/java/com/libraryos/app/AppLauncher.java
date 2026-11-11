package com.libraryos.app;

import javafx.application.Application;

/**
 * Main application launcher class that does not extend Application.
 * This prevents the JVM from requiring the JavaFX runtime to be available on the bootclasspath
 * when running from standard fat-jar packaging.
 */
public final class AppLauncher {

    private AppLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(LibraryOSApplication.class, args);
    }
}

