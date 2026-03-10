package org.example.admin;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.LoadBalancer;

/**
 * JavaFX Application that launches the Load Balancer in a background thread
 * and provides an Admin Dashboard UI for managing TCP connections.
 */
public class LBAdminApplication extends Application {

    private LoadBalancer loadBalancer;

    @Override
    public void start(Stage primaryStage) {
        // Create load balancer
        loadBalancer = new LoadBalancer();

        // Start LB in background thread
        Thread lbThread = new Thread(() -> loadBalancer.start(), "LoadBalancer-Main");
        lbThread.setDaemon(true);
        lbThread.start();

        // Create the admin dashboard controller
        LBAdminController controller = new LBAdminController(loadBalancer);
        Scene scene = new Scene(controller.buildUI(), 1100, 750);

        primaryStage.setTitle("Load Balancer Admin Dashboard — Port " + loadBalancer.getPort());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setOnCloseRequest(e -> {
            loadBalancer.shutdown();
            System.exit(0);
        });
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (loadBalancer != null) {
            loadBalancer.shutdown();
        }
    }
}

