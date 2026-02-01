package org.example;

import org.example.common.*;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.util.List;

/**
 * Controller for the main client view.
 * Implements MVC pattern with async socket operations.
 *
 * IMPORTANT: All socket operations must run off the JavaFX Application Thread!
 */
public class ClientController {

    @FXML private TableView<UserDTO> userTable;
    @FXML private TableColumn<UserDTO, Long> idColumn;
    @FXML private TableColumn<UserDTO, String> usernameColumn;
    @FXML private TableColumn<UserDTO, String> emailColumn;
    @FXML private TableColumn<UserDTO, String> fullNameColumn;

    @FXML private TextField searchField;
    @FXML private TextField userIdField;

    @FXML private Button fetchAllButton;
    @FXML private Button searchButton;
    @FXML private Button getUserButton;
    @FXML private Button pingButton;

    @FXML private Label statusLabel;
    @FXML private Label workerLabel;
    @FXML private Label responseTimeLabel;
    @FXML private ProgressIndicator progressIndicator;

    private final ObservableList<UserDTO> userData = FXCollections.observableArrayList();
    private final SocketClient socketClient = SocketClient.getInstance();

    @FXML
    public void initialize() {
        // Setup table columns
        setupTableColumns();

        // Bind table to observable list
        userTable.setItems(userData);

        // Setup button handlers
        fetchAllButton.setOnAction(e -> fetchAllUsers());
        searchButton.setOnAction(e -> searchUsers());
        getUserButton.setOnAction(e -> getUserById());
        pingButton.setOnAction(e -> pingServer());

        // Initial status
        updateStatus("Ready. Connected to: " + socketClient.getConnectionInfo());
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
    }

    /**
     * Fetches all users from the server.
     */
    private void fetchAllUsers() {
        executeAsync(Commands.GET_ALL_USERS, null, "Fetching all users...");
    }

    /**
     * Searches users by keyword.
     */
    private void searchUsers() {
        String keyword = searchField.getText();
        if (keyword == null || keyword.trim().isEmpty()) {
            showAlert("Please enter a search keyword");
            return;
        }
        executeAsync(Commands.SEARCH_USERS, keyword.trim(), "Searching...");
    }

    /**
     * Gets a specific user by ID.
     */
    private void getUserById() {
        String userId = userIdField.getText();
        if (userId == null || userId.trim().isEmpty()) {
            showAlert("Please enter a user ID");
            return;
        }
        executeAsync(Commands.GET_USER, userId.trim(), "Fetching user...");
    }

    /**
     * Pings the server to check connectivity.
     */
    private void pingServer() {
        Task<ResponsePayload> task = createRequestTask(Commands.PING, null);

        task.setOnSucceeded(e -> {
            ResponsePayload response = task.getValue();
            if (response.isSuccess()) {
                updateStatus("PONG! Server is alive. Worker: " + response.getWorkerId());
            } else {
                updateStatus("Ping failed: " + response.getMessage());
            }
            hideProgress();
        });

        task.setOnFailed(e -> {
            updateStatus("Connection failed: " + task.getException().getMessage());
            hideProgress();
        });

        showProgress("Pinging...");
        new Thread(task).start();
    }

    /**
     * Executes an async request to the server.
     */
    private void executeAsync(String command, String data, String loadingMessage) {
        Task<ResponsePayload> task = createRequestTask(command, data);

        task.setOnSucceeded(e -> {
            ResponsePayload response = task.getValue();
            handleResponse(response);
            hideProgress();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            updateStatus("Error: " + ex.getMessage());
            showAlert("Request failed: " + ex.getMessage());
            hideProgress();
        });

        showProgress(loadingMessage);
        new Thread(task).start();
    }

    /**
     * Creates a Task for async socket operations.
     * This is the key to not blocking the JavaFX Application Thread!
     */
    private Task<ResponsePayload> createRequestTask(String command, String data) {
        return new Task<>() {
            @Override
            protected ResponsePayload call() throws IOException {
                RequestPayload request = new RequestPayload(command, data);
                return socketClient.sendRequest(request);
            }
        };
    }

    /**
     * Handles the server response and updates the UI.
     */
    private void handleResponse(ResponsePayload response) {
        // Update worker info
        if (workerLabel != null && response.getWorkerId() != null) {
            workerLabel.setText("Worker: " + response.getWorkerId());
        }

        // Update response time
        if (responseTimeLabel != null) {
            responseTimeLabel.setText("Time: " + response.getProcessingTimeMs() + "ms");
        }

        if (response.isSuccess()) {
            // Update table with users if available
            List<UserDTO> users = response.getUsers();
            if (users != null) {
                userData.clear();
                userData.addAll(users);
                updateStatus("Loaded " + users.size() + " users");
            } else if (response.getData() != null) {
                // Try to parse single user from data
                try {
                    UserDTO user = PacketUtils.getObjectMapper()
                            .readValue(response.getData(), UserDTO.class);
                    userData.clear();
                    userData.add(user);
                    updateStatus("User found");
                } catch (Exception e) {
                    updateStatus(response.getMessage() != null ?
                            response.getMessage() : "Success");
                }
            } else {
                updateStatus(response.getMessage() != null ?
                        response.getMessage() : "Success");
            }
        } else {
            updateStatus("Error: " + response.getMessage());
            if (!ResponsePayload.STATUS_NOT_FOUND.equals(response.getStatus())) {
                showAlert(response.getMessage());
            }
        }
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message);
            }
        });
    }

    private void showProgress(String message) {
        Platform.runLater(() -> {
            if (progressIndicator != null) {
                progressIndicator.setVisible(true);
            }
            updateStatus(message);
        });
    }

    private void hideProgress() {
        Platform.runLater(() -> {
            if (progressIndicator != null) {
                progressIndicator.setVisible(false);
            }
        });
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
