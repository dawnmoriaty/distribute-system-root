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
import org.example.LoadBalancer;
import org.example.common.ConnectionAccessManager;
import org.example.common.ConnectionAccessManager.ActiveConnection;
import org.example.common.PendingConnection;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller for the Load Balancer Admin Dashboard.
 * Similar to ServerAdminController but includes LB-specific info (worker health, routing).
 *
 * Tabs:
 * 1. Active Connections — View and kick active TCP connections
 * 2. Pending Approvals — Approve/reject connections in MANUAL mode
 * 3. Access Control — Manage whitelist/blacklist and approval mode
 * 4. Worker Health — Monitor worker node health status
 * 5. Logs — Real-time connection event log
 */
public class LBAdminController {

    private final LoadBalancer loadBalancer;
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

    public LBAdminController(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        this.accessManager = loadBalancer.getAccessManager();
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
        Tab healthTab = new Tab("💓 Worker Health", buildWorkerHealthTab());
        Tab logTab = new Tab("📋 Logs", buildLogsTab());

        tabPane.getTabs().addAll(activeTab, pendingTab, aclTab, healthTab, logTab);
        root.setCenter(tabPane);

        // ===== Bottom: Status bar =====
        root.setBottom(buildStatusBar());

        return root;
    }

    // ==================== HEADER ====================

    private VBox buildHeader() {
        VBox header = new VBox(5);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label title = new Label("⚖️ Load Balancer Admin Dashboard");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        Label subtitle = new Label("Port: " + loadBalancer.getPort() +
                " | Round Robin | TCP Connection Access Control");
        subtitle.setTextFill(Color.GRAY);

        header.getChildren().addAll(title, subtitle, new Separator());
        return header;
    }

    // ==================== TAB 1: ACTIVE CONNECTIONS ====================

    private VBox buildActiveConnectionsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label info = new Label("Currently active TCP connections passing through the Load Balancer. " +
                "You can forcefully disconnect (kick) any connection.");
        info.setWrapText(true);
        info.setTextFill(Color.DARKSLATEGRAY);

        TableView<ActiveConnection> table = new TableView<>(activeConnectionsList);
        table.setPlaceholder(new Label("No active connections"));

        TableColumn<ActiveConnection, String> ipCol = new TableColumn<>("Remote IP");
        ipCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRemoteIP()));
        ipCol.setPrefWidth(150);

        TableColumn<ActiveConnection, Integer> remotePortCol = new TableColumn<>("Remote Port");
        remotePortCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getRemotePort()));
        remotePortCol.setPrefWidth(100);

        TableColumn<ActiveConnection, Integer> localPortCol = new TableColumn<>("LB Port");
        localPortCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getLocalPort()));
        localPortCol.setPrefWidth(80);

        TableColumn<ActiveConnection, String> timeCol = new TableColumn<>("Connected At");
        timeCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getConnectedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        timeCol.setPrefWidth(120);

        TableColumn<ActiveConnection, String> idCol = new TableColumn<>("Connection ID");
        idCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getId()));
        idCol.setPrefWidth(250);

        table.getColumns().addAll(ipCol, remotePortCol, localPortCol, timeCol, idCol);
        VBox.setVgrow(table, Priority.ALWAYS);

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

        buttons.getChildren().addAll(kickBtn, blockAndKickBtn, refreshBtn);
        content.getChildren().addAll(info, table, buttons);
        return content;
    }

    // ==================== TAB 2: PENDING APPROVALS ====================

    private VBox buildPendingApprovalsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label info = new Label("⚠️ These connections are waiting for your approval (MANUAL mode). " +
                "Timeout: " + accessManager.getManualApprovalTimeoutMs() / 1000 + " seconds");
        info.setWrapText(true);
        info.setTextFill(Color.DARKORANGE);

        TableView<PendingConnection> table = new TableView<>(pendingConnectionsList);
        table.setPlaceholder(new Label("No pending connections. Mode: " + accessManager.getApprovalMode()));

        TableColumn<PendingConnection, String> ipCol = new TableColumn<>("Remote IP");
        ipCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getRemoteIP()));
        ipCol.setPrefWidth(150);

        TableColumn<PendingConnection, Integer> portCol = new TableColumn<>("Remote Port");
        portCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getRemotePort()));
        portCol.setPrefWidth(100);

        TableColumn<PendingConnection, String> timeCol = new TableColumn<>("Arrived At");
        timeCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getArrivedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))));
        timeCol.setPrefWidth(130);

        TableColumn<PendingConnection, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getDecision().name()));
        statusCol.setPrefWidth(100);

        table.getColumns().addAll(ipCol, portCol, timeCol, statusCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Button approveBtn = new Button("✅ Approve");
        approveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        approveBtn.setOnAction(e -> {
            PendingConnection selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                accessManager.approvePending(selected.getId());
                Platform.runLater(() -> pendingConnectionsList.remove(selected));
            }
        });

        Button rejectBtn = new Button("❌ Reject");
        rejectBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14;");
        rejectBtn.setOnAction(e -> {
            PendingConnection selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                accessManager.rejectPending(selected.getId());
                Platform.runLater(() -> pendingConnectionsList.remove(selected));
            }
        });

        Button rejectAllBtn = new Button("⛔ Reject All");
        rejectAllBtn.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white;");
        rejectAllBtn.setOnAction(e -> {
            accessManager.rejectAllPending();
            Platform.runLater(() -> pendingConnectionsList.clear());
        });

        Button approveAndWLBtn = new Button("✅ Approve & Whitelist");
        approveAndWLBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white;");
        approveAndWLBtn.setOnAction(e -> {
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

        buttons.getChildren().addAll(approveBtn, rejectBtn, rejectAllBtn, approveAndWLBtn);
        content.getChildren().addAll(info, table, buttons);
        return content;
    }

    // ==================== TAB 3: ACCESS CONTROL ====================

    private VBox buildAccessControlTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        // Mode selection
        TitledPane modePane = new TitledPane();
        modePane.setText("🔧 Approval Mode");
        modePane.setCollapsible(false);

        VBox modeContent = new VBox(10);
        modeContent.setPadding(new Insets(10));

        Label modeDesc = new Label(
                "AUTO: Automatically allow/deny based on whitelist/blacklist.\n" +
                "MANUAL: Every TCP connection requires your explicit approval.\n\n" +
                "💡 The Load Balancer is the FIRST entry point — blocking here prevents " +
                "requests from ever reaching worker servers.");
        modeDesc.setWrapText(true);
        modeDesc.setTextFill(Color.DARKSLATEGRAY);

        HBox modeSwitch = new HBox(15);
        modeSwitch.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton autoRadio = new RadioButton("🟢 AUTO (ACL Rules)");
        autoRadio.setToggleGroup(modeGroup);
        autoRadio.setStyle("-fx-font-size: 14;");

        RadioButton manualRadio = new RadioButton("🟠 MANUAL (Admin Approval)");
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

        HBox timeoutBox = new HBox(10);
        timeoutBox.setAlignment(Pos.CENTER_LEFT);
        Label timeoutLabel = new Label("Timeout (sec):");
        Spinner<Integer> timeoutSpinner = new Spinner<>(5, 120, (int)(accessManager.getManualApprovalTimeoutMs() / 1000));
        timeoutSpinner.setEditable(true);
        timeoutSpinner.setPrefWidth(80);
        Button applyBtn = new Button("Apply");
        applyBtn.setOnAction(e -> accessManager.setManualApprovalTimeoutMs(timeoutSpinner.getValue() * 1000L));
        timeoutBox.getChildren().addAll(timeoutLabel, timeoutSpinner, applyBtn);

        modeSwitch.getChildren().addAll(autoRadio, manualRadio);
        modeContent.getChildren().addAll(modeDesc, modeSwitch, timeoutBox);
        modePane.setContent(modeContent);

        // Whitelist
        TitledPane whitelistPane = buildIPListPane(
                "✅ Whitelist (Allowed IPs)",
                "Only these IPs can connect (if non-empty). Empty = allow all.",
                whitelistItems,
                ip -> { accessManager.addToWhitelist(ip); refreshACLLists(); },
                ip -> { accessManager.removeFromWhitelist(ip); refreshACLLists(); }
        );

        // Blacklist
        TitledPane blacklistPane = buildIPListPane(
                "⛔ Blacklist (Blocked IPs)",
                "These IPs are ALWAYS blocked. Adding also kicks existing connections.",
                blacklistItems,
                ip -> { accessManager.addToBlacklist(ip); refreshACLLists(); },
                ip -> { accessManager.removeFromBlacklist(ip); refreshACLLists(); }
        );

        content.getChildren().addAll(modePane, whitelistPane, blacklistPane);
        return content;
    }

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
        listView.setPrefHeight(100);

        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        TextField ipField = new TextField();
        ipField.setPromptText("IP address (e.g., 192.168.1.100)");
        ipField.setPrefWidth(250);

        Button addBtn = new Button("➕ Add");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addBtn.setOnAction(e -> {
            String ip = ipField.getText().trim();
            if (!ip.isEmpty() && isValidIP(ip)) {
                onAdd.accept(ip);
                ipField.clear();
            } else {
                showAlert("Invalid IP address.");
            }
        });

        Button removeBtn = new Button("🗑️ Remove");
        removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        removeBtn.setOnAction(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) onRemove.accept(selected);
        });

        inputBox.getChildren().addAll(ipField, addBtn, removeBtn);
        paneContent.getChildren().addAll(desc, listView, inputBox);
        pane.setContent(paneContent);
        return pane;
    }

    // ==================== TAB 4: WORKER HEALTH ====================

    private VBox buildWorkerHealthTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label info = new Label("Real-time health status of backend worker servers. " +
                "The Load Balancer routes traffic only to HEALTHY workers.");
        info.setWrapText(true);
        info.setTextFill(Color.DARKSLATEGRAY);

        VBox workerCards = new VBox(10);

        for (String worker : loadBalancer.getWorkerNodes()) {
            HBox card = new HBox(15);
            card.setPadding(new Insets(12));
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5; -fx-background-radius: 5; -fx-background-color: #fafafa;");

            Label nameLabel = new Label("🖥️ " + worker);
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            nameLabel.setPrefWidth(200);

            Label healthLabel = new Label();
            healthLabel.setPrefWidth(150);

            // Periodic update
            javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), event -> {
                        Boolean healthy = loadBalancer.getWorkerHealth().get(worker);
                        if (Boolean.TRUE.equals(healthy)) {
                            healthLabel.setText("✅ HEALTHY");
                            healthLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 13;");
                            card.setStyle("-fx-border-color: #4CAF50; -fx-border-radius: 5; -fx-background-radius: 5; -fx-background-color: #e8f5e9;");
                        } else {
                            healthLabel.setText("❌ UNHEALTHY");
                            healthLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 13;");
                            card.setStyle("-fx-border-color: #f44336; -fx-border-radius: 5; -fx-background-radius: 5; -fx-background-color: #ffebee;");
                        }
                    })
            );
            timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            timeline.play();
            // Trigger first update
            Boolean healthy = loadBalancer.getWorkerHealth().get(worker);
            if (Boolean.TRUE.equals(healthy)) {
                healthLabel.setText("✅ HEALTHY");
                healthLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-font-size: 13;");
            } else {
                healthLabel.setText("⏳ Checking...");
                healthLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 13;");
            }

            card.getChildren().addAll(nameLabel, healthLabel);
            workerCards.getChildren().add(card);
        }

        // Stats
        Label statsLabel = new Label();
        javafx.animation.Timeline statsTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), event -> {
                    statsLabel.setText(String.format(
                            "Total connections processed: %d | Currently active: %d",
                            loadBalancer.getTotalConnectionCount(),
                            loadBalancer.getActiveConnectionCount()));
                })
        );
        statsTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        statsTimeline.play();

        content.getChildren().addAll(info, workerCards, new Separator(), statsLabel);
        return content;
    }

    // ==================== TAB 5: LOGS ====================

    private VBox buildLogsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label info = new Label("Real-time log of all TCP connection events.");
        info.setTextFill(Color.DARKSLATEGRAY);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        for (ConnectionAccessManager.ConnectionLogEntry entry : accessManager.getConnectionLog()) {
            logArea.appendText(entry.toString() + "\n");
        }

        Button clearBtn = new Button("🗑️ Clear Log");
        clearBtn.setOnAction(e -> logArea.clear());

        content.getChildren().addAll(info, logArea, clearBtn);
        return content;
    }

    // ==================== STATUS BAR ====================

    private HBox buildStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(8));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("⚖️ Load Balancer Running — Port " + loadBalancer.getPort());
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
        accessManager.addLogListener(message ->
                Platform.runLater(() -> {
                    if (logArea != null) {
                        logArea.appendText(message + "\n");
                        logArea.setScrollTop(Double.MAX_VALUE);
                    }
                    updateCountLabels();
                })
        );

        accessManager.addPendingListener(pending ->
                Platform.runLater(() -> {
                    pendingConnectionsList.add(pending);
                    updateCountLabels();
                })
        );

        accessManager.addActiveConnectionListener(active ->
                Platform.runLater(() -> {
                    activeConnectionsList.add(active);
                    updateCountLabels();
                })
        );

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
            if (activeCountLabel != null)
                activeCountLabel.setText("Active: " + accessManager.getActiveConnectionCount());
            if (pendingCountLabel != null)
                pendingCountLabel.setText("Pending: " + accessManager.getPendingCount());
        });
    }

    private boolean isValidIP(String ip) {
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

