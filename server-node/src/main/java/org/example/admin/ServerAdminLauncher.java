package org.example.admin;

import javafx.application.Application;

/**
 * Launcher class for the Server Admin Dashboard.
 * Required because JavaFX Application class cannot be the main class
 * when using module path in certain configurations.
 */
public class ServerAdminLauncher {

    public static void main(String[] args) {
        Application.launch(ServerAdminApplication.class, args);
    }
}

