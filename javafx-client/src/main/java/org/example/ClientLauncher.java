package org.example;

import javafx.application.Application;

/**
 * Launcher class for the JavaFX Client.
 * Required because JavaFX Application class cannot be launched directly in modular projects.
 */
public class ClientLauncher {

    public static void main(String[] args) {
        Application.launch(ClientApplication.class, args);
    }
}
