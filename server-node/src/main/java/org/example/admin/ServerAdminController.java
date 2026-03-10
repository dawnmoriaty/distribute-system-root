package org.example.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.WorkerServer;
import org.example.common.ConnectionAccessManager;
import org.example.common.ConnectionAccessManager.ActiveConnection;
import org.example.common.PendingConnection;

import java.time.format.DateTimeFormatter;

/**
 * Controller for the Server Admin Dashboard.
 * Builds the entire UI programmatically (no FXML needed).
 *
 * Tabs:
 * 1. Active Connections — View and kick active TCP connections
 * 2. Pending Approvals — Approve/reject connections in MANUAL mode
 * 3. Access Control — Manage whitelist/blacklist and approval mode
 * 4. Logs — Real-time connection event log
 */
public class ServerAdminController {

    private final WorkerServer workerServer;
    private final ConnectionAccessManager accessManager;

    // Observable lists for UI binding
    private final ObservableList<ActiveConnection> activeConnectionsList = FXCollections.observableArrayList();
    private final ObservableList<PendingConnection> pendingConnectionsList = FXCollections.observableArrayList();
    private final ObservableList<String> whitelistItems = FXCollections.observableArrayList();
    private final ObservableList<String> blacklistItems = FXCollections.observableArrayList();

    // Log area
    private TextArea logArea;

    // Status labels
    private Label statusLabel;
    private Label modeLabel;
    private Label activeCountLabel;
    private Label pendingCountLabel;

    public ServerAdminController(WorkerServer workerServer) {
        this.workerServer = workerServer;
        this.accessManager = workerServer.getAccessManager();
        setupListeners();
        loadCurrentState();
    }

    /**
     * Build the entire UI layout.
     */
    public Parent buildUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // ===== Top: Header =====
        root.setTop(buildHeader());

        // ===== Center: TabPane =====
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab activeTab = new Tab("🔌 Active Connections", buildActiveConnectionsTab());
        Tab pendingTab = new Tab("⏳ Pending Approvals", buildPendingApprovalsTab());
        Tab aclTab = new Tab("🛡️ Access Control", buildAccessControlTab());
        Tab logTab = new Tab("📋 Logs", buildLogsTab());

        tabPane.getTabs().addAll(activeTab, pendingTab, aclTab, logTab);
        root.setCenter(tabPane);

        // ===== Bottom: Status bar =====
        root.setBottom(buildStatusBar());

        return root;
    }

    // ==================== HEADER ====================

    private VBox buildHeader() {
        VBox header = new VBox(5);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label title = new Label("🖥️ Server Admin Dashboard — " + workerServer.getWorkerId());
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        Label subtitle = new Label("Port: " + workerServer.getPort() +
                " | TCP Connection Access Control");
        subtitle.setTextFill(Color.GRAY);

        header.getChildren().addAll(title, subtitle, new Separator());
        return header;
    }

    // ==================== TAB 1: ACTIVE CONNECTIONS ====================

    private VBox buildActiveConnectionsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label info = new Label("Currently active TCP connections to this server. " +
                "You can forcefully disconnect (kick) any connection.");
        info.setWrapText(true);
        info.setTextFill(Color.DARKSLATEGRAY);

        // Table
        TableView<ActiveConnection> table = new TableView<>(activeConnectionsList);
        table.setPlaceholder(new Label("No active connections"));

        TableColumn<ActiveConnection, String> ipCol = new TableColumn<>("Remote IP");
        ipCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRemoteIP()));
        ipCol.setPrefWidth(150);

        TableColumn<ActiveConnection, Integer> remotePortCol = new TableColumn<>("Remote Port");
        remotePortCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getRemotePort()));
        remotePortCol.setPrefWidth(100);

        TableColumn<ActiveConnection, Integer> localPortCol = new TableColumn<>("Server Port");
        localPortCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getLocalPort()));
        localPortCol.setPrefWidth(100);

        TableColumn<ActiveConnection, String> timeCol = new TableColumn<>("Connected At");
        timeCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getConnectedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        timeCol.setPrefWidth(120);

        TableColumn<ActiveConnection, String> idCol = new TableColumn<>("Connection ID");
        idCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getId()));
        idCol.setPrefWidth(250);

        table.getColumns().addAll(ipCol, remotePortCol, localPortCol, timeCol, idCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Buttons
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Button kickBtn = new Button("🚫 Kick Selected");
        kickBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        kickBtn.setOnAction(e -> {
            ActiveConnection selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                accessManager.kickConnection(selected.getId());
                Platform.runLater(() -> activeConnectionsList.remove(selected));
            } else {
                showAlert("Please select a connection to kick.");
            }
        });

        Button kickIPBtn = new Button("🚫 Kick All from Selected IP");
        kickIPBtn.setStyle("-fx-background-color: #e65100; -fx-text-fill: white;");
        kickIPBtn.setOnAction(e -> {
            ActiveConnection selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String ip = selected.getRemoteIP();
                accessManager.kickConnectionsByIP(ip);
                Platform.runLater(() -> activeConnectionsList.removeIf(ac -> ac.getRemoteIP().equals(ip)));
            }
        });

        Button blockAndKickBtn = new Button("⛔ Block IP & Kick");
        blockAndKickBtn.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-font-weight: bold;");
        blockAndKickBtn.setOnAction(e -> {
            ActiveConnection selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String ip = selected.getRemoteIP();
                accessManager.addToBlacklist(ip);
                Platform.runLater(() -> {
                    activeConnectionsList.removeIf(ac -> ac.getRemoteIP().equals(ip));
                    refreshACLLists();
                });
            }
        });

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setOnAction(e -> refreshActiveConnections());

        buttons.getChildren().addAll(kickBtn, kickIPBtn, blockAndKickBtn, refreshBtn);

        content.getChildren().addAll(info, table, buttons);
        return content;
    }

    // ==================== TAB 2: PENDING APPROVALS ====================

    private VBox buildPendingApprovalsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label info = new Label("⚠️ These connections are waiting for your approval (MANUAL mode). " +
                "If not approved within the timeout, they are automatically rejected.\n" +
                "Timeout: " + accessManager.getManualApprovalTimeoutMs() / 1000 + " seconds");
        info.setWrapText(true);
        info.setTextFill(Color.DARKORANGE);

        // Table
        TableView<PendingConnection> table = new TableView<>(pendingConnectionsList);
        table.setPlaceholder(new Label("No pending connections. Mode: " + accessManager.getApprovalMode()));

        TableColumn<PendingConnection, String> ipCol = new TableColumn<>("Remote IP");
        ipCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRemoteIP()));
        ipCol.setPrefWidth(150);

        TableColumn<PendingConnection, Integer> portCol = new TableColumn<>("Remote Port");
        portCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getRemotePort()));
        portCol.setPrefWidth(100);

        TableColumn<PendingConnection, Integer> localPortCol = new TableColumn<>("Server Port");
        localPortCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getLocalPort()));
        localPortCol.setPrefWidth(100);

        TableColumn<PendingConnection, String> timeCol = new TableColumn<>("Arrived At");
        timeCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getArrivedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))));
        timeCol.setPrefWidth(130);

        TableColumn<PendingConnection, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getDecision().name()));
        statusCol.setPrefWidth(100);

        table.getColumns().addAll(ipCol, portCol, localPortCol, timeCol, statusCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Buttons
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Button approveBtn = new Button("✅ Approve Selected");
        approveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        approveBtn.setOnAction(e -> {
            PendingConnection selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                accessManager.approvePending(selected.getId());
                Platform.runLater(() -> pendingConnectionsList.remove(selected));
            } else {
                showAlert("Please select a connection to approve.");
            }
        });

        Button rejectBtn = new Button("❌ Reject Selected");
        rejectBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        rejectBtn.setOnAction(e -> {
            PendingConnection selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                accessManager.rejectPending(selected.getId());
                Platform.runLater(() -> pendingConnectionsList.remove(selected));
            } else {
                showAlert("Please select a connection to reject.");
            }
        });

        Button rejectAllBtn = new Button("⛔ Reject All Pending");
        rejectAllBtn.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white;");
        rejectAllBtn.setOnAction(e -> {
            accessManager.rejectAllPending();
            Platform.runLater(() -> pendingConnectionsList.clear());
        });

        Button approveAndWhitelistBtn = new Button("✅ Approve & Add to Whitelist");
        approveAndWhitelistBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white;");
        approveAndWhitelistBtn.setOnAction(e -> {
            PendingConnection selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                accessManager.addToWhitelist(selected.getRemoteIP());
                accessManager.approvePending(selected.getId());
                Platform.runLater(() -> {
                    pendingConnectionsList.remove(selected);
                    refreshACLLists();
                });
            }
        });

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setOnAction(e -> refreshPendingConnections());

        buttons.getChildren().addAll(approveBtn, rejectBtn, rejectAllBtn, approveAndWhitelistBtn, refreshBtn);

        content.getChildren().addAll(info, table, buttons);
        return content;
    }

    // ==================== TAB 3: ACCESS CONTROL ====================

    private VBox buildAccessControlTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        // ===== Mode Selection =====
        TitledPane modePane = new TitledPane();
        modePane.setText("🔧 Approval Mode");
        modePane.setCollapsible(false);

        VBox modeContent = new VBox(10);
        modeContent.setPadding(new Insets(10));

        Label modeDesc = new Label(
                "AUTO Mode: Connections are automatically allowed/denied based on whitelist/blacklist rules.\n" +
                "MANUAL Mode: Every incoming TCP connection requires YOUR explicit approval in the 'Pending Approvals' tab.\n\n" +
                "💡 TCP's nature: The server has the RIGHT to accept or refuse any connection. " +
                "This is not hard-coded — you control it dynamically from this UI!");
        modeDesc.setWrapText(true);
        modeDesc.setTextFill(Color.DARKSLATEGRAY);

        HBox modeSwitch = new HBox(15);
        modeSwitch.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton autoRadio = new RadioButton("🟢 AUTO (Use ACL Rules)");
        autoRadio.setToggleGroup(modeGroup);
        autoRadio.setStyle("-fx-font-size: 14;");

        RadioButton manualRadio = new RadioButton("🟠 MANUAL (Admin Approves Each Connection)");
        manualRadio.setToggleGroup(modeGroup);
        manualRadio.setStyle("-fx-font-size: 14;");

        if (accessManager.getApprovalMode() == ConnectionAccessManager.ApprovalMode.AUTO) {
            autoRadio.setSelected(true);
        } else {
            manualRadio.setSelected(true);
        }

        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == autoRadio) {
                accessManager.setApprovalMode(ConnectionAccessManager.ApprovalMode.AUTO);
            } else {
                accessManager.setApprovalMode(ConnectionAccessManager.ApprovalMode.MANUAL);
            }
            updateModeLabel();
        });

        // Timeout setting
        HBox timeoutBox = new HBox(10);
        timeoutBox.setAlignment(Pos.CENTER_LEFT);
        Label timeoutLabel = new Label("Manual Approval Timeout (seconds):");
        Spinner<Integer> timeoutSpinner = new Spinner<>(5, 120, (int)(accessManager.getManualApprovalTimeoutMs() / 1000));
        timeoutSpinner.setEditable(true);
        timeoutSpinner.setPrefWidth(80);
        Button applyTimeoutBtn = new Button("Apply");
        applyTimeoutBtn.setOnAction(e -> {
            accessManager.setManualApprovalTimeoutMs(timeoutSpinner.getValue() * 1000L);
        });
        timeoutBox.getChildren().addAll(timeoutLabel, timeoutSpinner, applyTimeoutBtn);

        modeSwitch.getChildren().addAll(autoRadio, manualRadio);
        modeContent.getChildren().addAll(modeDesc, modeSwitch, timeoutBox);
        modePane.setContent(modeContent);

        // ===== Whitelist =====
        TitledPane whitelistPane = buildIPListPane(
                "✅ Whitelist (Allowed IPs)",
                "If the whitelist is non-empty, ONLY these IPs can connect (in AUTO mode). " +
                "If empty, all IPs are allowed (unless blacklisted).",
                whitelistItems,
                ip -> {
                    accessManager.addToWhitelist(ip);
                    refreshACLLists();
                },
                ip -> {
                    accessManager.removeFromWhitelist(ip);
                    refreshACLLists();
                }
        );

        // ===== Blacklist =====
        TitledPane blacklistPane = buildIPListPane(
                "⛔ Blacklist (Blocked IPs)",
                "These IPs are ALWAYS blocked, even if they are in the whitelist. " +
                "Adding an IP here will also kick all existing connections from that IP.",
                blacklistItems,
                ip -> {
                    accessManager.addToBlacklist(ip);
                    refreshACLLists();
                },
                ip -> {
                    accessManager.removeFromBlacklist(ip);
                    refreshACLLists();
                }
        );

        content.getChildren().addAll(modePane, whitelistPane, blacklistPane);
        return content;
    }

    /**
     * Helper: builds a TitledPane for whitelist or blacklist management.
     */
    private TitledPane buildIPListPane(String title, String description,
                                       ObservableList<String> items,
                                       java.util.function.Consumer<String> onAdd,
                                       java.util.function.Consumer<String> onRemove) {
        TitledPane pane = new TitledPane();
        pane.setText(title);
        pane.setCollapsible(true);
        pane.setExpanded(true);

        VBox paneContent = new VBox(8);
        paneContent.setPadding(new Insets(10));

        Label desc = new Label(description);
        desc.setWrapText(true);
        desc.setTextFill(Color.DARKSLATEGRAY);

        ListView<String> listView = new ListView<>(items);
        listView.setPrefHeight(120);

        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        TextField ipField = new TextField();
        ipField.setPromptText("Enter IP address (e.g., 192.168.1.100 or 127.0.0.1)");
        ipField.setPrefWidth(300);

        Button addBtn = new Button("➕ Add");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            String ip = ipField.getText().trim();
            if (!ip.isEmpty() && isValidIP(ip)) {
                onAdd.accept(ip);
                ipField.clear();
            } else {
                showAlert("Please enter a valid IP address.");
            }
        });

        Button removeBtn = new Button("🗑️ Remove Selected");
        removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        removeBtn.setOnAction(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                onRemove.accept(selected);
            }
        });

        inputBox.getChildren().addAll(ipField, addBtn, removeBtn);
        paneContent.getChildren().addAll(desc, listView, inputBox);
        pane.setContent(paneContent);

        return pane;
    }

    // ==================== TAB 4: LOGS ====================

    private VBox buildLogsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label info = new Label("Real-time log of all TCP connection events (accept/reject/kick/ACL changes).");
        info.setTextFill(Color.DARKSLATEGRAY);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        // Load existing log entries
        for (ConnectionAccessManager.ConnectionLogEntry entry : accessManager.getConnectionLog()) {
            logArea.appendText(entry.toString() + "\n");
        }

        HBox buttons = new HBox(10);
        Button clearLogBtn = new Button("🗑️ Clear Log");
        clearLogBtn.setOnAction(e -> logArea.clear());

        buttons.getChildren().add(clearLogBtn);

        content.getChildren().addAll(info, logArea, buttons);
        return content;
    }

    // ==================== STATUS BAR ====================

    private HBox buildStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(8));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("🟢 Server Running — " + workerServer.getWorkerId());
        statusLabel.setStyle("-fx-font-weight: bold;");

        modeLabel = new Label();
        updateModeLabel();

        activeCountLabel = new Label("Active: 0");
        pendingCountLabel = new Label("Pending: 0");

        statusBar.getChildren().addAll(
                statusLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                modeLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                activeCountLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                pendingCountLabel
        );

        return statusBar;
    }

    // ==================== LISTENERS ====================

    private void setupListeners() {
        // Log listener — append to log area
        accessManager.addLogListener(message ->
                Platform.runLater(() -> {
                    if (logArea != null) {
                        logArea.appendText(message + "\n");
                        logArea.setScrollTop(Double.MAX_VALUE);
                    }
                    updateCountLabels();
                })
        );

        // Pending connection listener — add to pending table
        accessManager.addPendingListener(pending ->
                Platform.runLater(() -> {
                    pendingConnectionsList.add(pending);
                    updateCountLabels();
                })
        );

        // Active connection added listener
        accessManager.addActiveConnectionListener(active ->
                Platform.runLater(() -> {
                    activeConnectionsList.add(active);
                    updateCountLabels();
                })
        );

        // Active connection removed listener
        accessManager.addConnectionRemovedListener(removed ->
                Platform.runLater(() -> {
                    activeConnectionsList.removeIf(ac -> ac.getId().equals(removed.getId()));
                    updateCountLabels();
                })
        );
    }

    // ==================== HELPERS ====================

    private void loadCurrentState() {
        refreshACLLists();
        refreshActiveConnections();
        refreshPendingConnections();
    }

    private void refreshACLLists() {
        Platform.runLater(() -> {
            whitelistItems.setAll(accessManager.getWhitelist());
            blacklistItems.setAll(accessManager.getBlacklist());
        });
    }

    private void refreshActiveConnections() {
        Platform.runLater(() -> {
            activeConnectionsList.setAll(accessManager.getActiveConnections());
            updateCountLabels();
        });
    }

    private void refreshPendingConnections() {
        Platform.runLater(() -> {
            pendingConnectionsList.setAll(accessManager.getPendingConnections());
            updateCountLabels();
        });
    }

    private void updateModeLabel() {
        Platform.runLater(() -> {
            if (modeLabel != null) {
                ConnectionAccessManager.ApprovalMode mode = accessManager.getApprovalMode();
                if (mode == ConnectionAccessManager.ApprovalMode.AUTO) {
                    modeLabel.setText("Mode: 🟢 AUTO");
                    modeLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                } else {
                    modeLabel.setText("Mode: 🟠 MANUAL");
                    modeLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void updateCountLabels() {
        Platform.runLater(() -> {
            if (activeCountLabel != null) {
                activeCountLabel.setText("Active: " + accessManager.getActiveConnectionCount());
            }
            if (pendingCountLabel != null) {
                pendingCountLabel.setText("Pending: " + accessManager.getPendingCount());
            }
        });
    }

    private boolean isValidIP(String ip) {
        // Simple IP validation
        if (ip.equals("localhost")) return true;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        try {
            for (String part : parts) {
                int val = Integer.parseInt(part);
                if (val < 0 || val > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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

