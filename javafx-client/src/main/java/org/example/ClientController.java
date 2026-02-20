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

    // CRUD fields
    @FXML private TextField crudIdField;
    @FXML private TextField crudUsernameField;
    @FXML private TextField crudEmailField;
    @FXML private TextField crudFullNameField;

    @FXML private Button fetchAllButton;
    @FXML private Button searchButton;
    @FXML private Button getUserButton;
    @FXML private Button pingButton;
    @FXML private Button healthCheckButton;

    // CRUD buttons
    @FXML private Button createUserButton;
    @FXML private Button updateUserButton;
    @FXML private Button deleteUserButton;
    @FXML private Button clearFormButton;
    @FXML private Button loadSelectedButton;

    @FXML private Label statusLabel;
    @FXML private Label workerLabel;
    @FXML private Label responseTimeLabel;
    @FXML private Label sslLabel;
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

        // Health check button
        if (healthCheckButton != null) {
            healthCheckButton.setOnAction(e -> healthCheck());
        }

        // CRUD button handlers
        if (createUserButton != null) {
            createUserButton.setOnAction(e -> createUser());
        }
        if (updateUserButton != null) {
            updateUserButton.setOnAction(e -> updateUser());
        }
        if (deleteUserButton != null) {
            deleteUserButton.setOnAction(e -> deleteUser());
        }
        if (clearFormButton != null) {
            clearFormButton.setOnAction(e -> clearForm());
        }
        if (loadSelectedButton != null) {
            loadSelectedButton.setOnAction(e -> loadSelectedUser());
        }

        // Table selection listener
        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadUserToForm(newSelection);
                }
            }
        );

        // Update SSL status
        if (sslLabel != null) {
            boolean sslEnabled = socketClient.isSSLEnabled();
            sslLabel.setText(sslEnabled ? "🔒 SSL: Enabled" : "🔓 SSL: Disabled");
            sslLabel.setStyle(sslEnabled ? "-fx-text-fill: green;" : "-fx-text-fill: orange;");
        }

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
     * Creates a new user.
     */
    private void createUser() {
        if (!validateCrudFields(false)) {
            return;
        }

        try {
            UserDTO user = new UserDTO();
            user.setUsername(crudUsernameField.getText().trim());
            user.setEmail(crudEmailField.getText().trim());
            user.setFullName(crudFullNameField.getText().trim());

            String json = PacketUtils.getObjectMapper().writeValueAsString(user);
            executeAsyncWithCallback(Commands.CREATE_USER, json, "Creating user...", () -> {
                clearForm();
                fetchAllUsers(); // Refresh list
            });
        } catch (Exception e) {
            showAlert("Error creating user: " + e.getMessage());
        }
    }

    /**
     * Updates an existing user.
     */
    private void updateUser() {
        if (!validateCrudFields(true)) {
            return;
        }

        try {
            UserDTO user = new UserDTO();
            user.setId(Long.parseLong(crudIdField.getText().trim()));
            user.setUsername(crudUsernameField.getText().trim());
            user.setEmail(crudEmailField.getText().trim());
            user.setFullName(crudFullNameField.getText().trim());

            String json = PacketUtils.getObjectMapper().writeValueAsString(user);
            executeAsyncWithCallback(Commands.UPDATE_USER, json, "Updating user...", () -> {
                fetchAllUsers(); // Refresh list
            });
        } catch (Exception e) {
            showAlert("Error updating user: " + e.getMessage());
        }
    }

    /**
     * Deletes a user by ID.
     */
    private void deleteUser() {
        String idText = crudIdField.getText();
        if (idText == null || idText.trim().isEmpty()) {
            showAlert("Please enter or select a user ID to delete");
            return;
        }

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete User?");
        confirm.setContentText("Are you sure you want to delete user with ID: " + idText + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executeAsyncWithCallback(Commands.DELETE_USER, idText.trim(), "Deleting user...", () -> {
                    clearForm();
                    fetchAllUsers(); // Refresh list
                });
            }
        });
    }

    /**
     * Clears the CRUD form fields.
     */
    private void clearForm() {
        Platform.runLater(() -> {
            if (crudIdField != null) crudIdField.clear();
            if (crudUsernameField != null) crudUsernameField.clear();
            if (crudEmailField != null) crudEmailField.clear();
            if (crudFullNameField != null) crudFullNameField.clear();
        });
    }

    /**
     * Loads the selected user from table to form.
     */
    private void loadSelectedUser() {
        UserDTO selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a user from the table");
            return;
        }
        loadUserToForm(selected);
    }

    /**
     * Loads a user DTO to the form fields.
     */
    private void loadUserToForm(UserDTO user) {
        Platform.runLater(() -> {
            if (crudIdField != null && user.getId() != null) {
                crudIdField.setText(String.valueOf(user.getId()));
            }
            if (crudUsernameField != null) {
                crudUsernameField.setText(user.getUsername() != null ? user.getUsername() : "");
            }
            if (crudEmailField != null) {
                crudEmailField.setText(user.getEmail() != null ? user.getEmail() : "");
            }
            if (crudFullNameField != null) {
                crudFullNameField.setText(user.getFullName() != null ? user.getFullName() : "");
            }
        });
    }

    /**
     * Validates CRUD form fields.
     */
    private boolean validateCrudFields(boolean requireId) {
        if (requireId) {
            String idText = crudIdField != null ? crudIdField.getText() : null;
            if (idText == null || idText.trim().isEmpty()) {
                showAlert("User ID is required for update");
                return false;
            }
            try {
                Long.parseLong(idText.trim());
            } catch (NumberFormatException e) {
                showAlert("Invalid User ID format");
                return false;
            }
        }

        String username = crudUsernameField != null ? crudUsernameField.getText() : null;
        if (username == null || username.trim().isEmpty()) {
            showAlert("Username is required");
            return false;
        }

        String email = crudEmailField != null ? crudEmailField.getText() : null;
        if (email == null || email.trim().isEmpty()) {
            showAlert("Email is required");
            return false;
        }

        return true;
    }

    /**
     * Performs health check on the server.
     */
    private void healthCheck() {
        executeAsync(Commands.HEALTH_CHECK, null, "Checking health...");
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
        executeAsyncWithCallback(command, data, loadingMessage, null);
    }

    /**
     * Executes an async request with a callback on success.
     */
    private void executeAsyncWithCallback(String command, String data, String loadingMessage, Runnable onSuccess) {
        Task<ResponsePayload> task = createRequestTask(command, data);

        task.setOnSucceeded(e -> {
            ResponsePayload response = task.getValue();
            handleResponse(response);
            hideProgress();

            if (response.isSuccess() && onSuccess != null) {
                Platform.runLater(onSuccess);
            }
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
