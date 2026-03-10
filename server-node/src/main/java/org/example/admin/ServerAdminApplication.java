package org.example.admin;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.WorkerServer;

/**
 * JavaFX Application that launches the Worker Server in a background thread
 * and provides an Admin Dashboard UI for managing TCP connections.
 *
 * Usage: pass the port number as the first argument (default: 9001).
 */
public class ServerAdminApplication extends Application {

    private WorkerServer workerServer;
    private Thread serverThread;

    @Override
    public void start(Stage primaryStage) {
        // Parse port from arguments
        int port = 9001;
        var params = getParameters().getRaw();
        if (!params.isEmpty()) {
            try {
                port = Integer.parseInt(params.get(0));
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using default: 9001");
            }
        }

        // Create worker server
        workerServer = new WorkerServer(port);

        // Start server in background thread
        serverThread = new Thread(() -> workerServer.start(), "WorkerServer-" + port);
        serverThread.setDaemon(true);
        serverThread.start();

        // Create the admin dashboard controller
        ServerAdminController controller = new ServerAdminController(workerServer);
        Scene scene = new Scene(controller.buildUI(), 1100, 750);

        primaryStage.setTitle("Server Admin Dashboard — " + workerServer.getWorkerId());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setOnCloseRequest(e -> {
            workerServer.shutdown();
            System.exit(0);
        });
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (workerServer != null) {
            workerServer.shutdown();
        }
    }
}

