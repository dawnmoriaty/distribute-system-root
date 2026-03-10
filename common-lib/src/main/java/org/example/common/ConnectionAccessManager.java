package org.example.common;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Dynamic TCP Connection Access Control Manager.
 *
 * Manages:
 * - Whitelist / Blacklist of IPs (dynamic, not hard-coded)
 * - Approval mode: AUTO (use ACL rules) or MANUAL (admin approves each connection)
 * - Pending connection queue for MANUAL mode
 * - Connection event logging for UI display
 *
 * This is the core of TCP accept/reject — after serverSocket.accept(),
 * the socket is checked through this manager before being processed.
 */
public class ConnectionAccessManager {

    // ===================== Enums =====================

    public enum ApprovalMode {
        /** Automatically allow/deny based on whitelist/blacklist rules */
        AUTO,
        /** Every connection requires manual admin approval via UI */
        MANUAL
    }

    // ===================== Singleton =====================

    private static volatile ConnectionAccessManager instance;

    public static ConnectionAccessManager getInstance() {
        if (instance == null) {
            synchronized (ConnectionAccessManager.class) {
                if (instance == null) {
                    instance = new ConnectionAccessManager();
                }
            }
        }
        return instance;
    }

    // ===================== State =====================

    /** Current approval mode */
    private volatile ApprovalMode approvalMode = ApprovalMode.AUTO;

    /** Whitelisted IPs — if non-empty, ONLY these IPs are allowed (in AUTO mode) */
    private final Set<String> whitelist = ConcurrentHashMap.newKeySet();

    /** Blacklisted IPs — these IPs are ALWAYS blocked (checked before whitelist) */
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    /** Pending connections waiting for admin approval (MANUAL mode) */
    private final ConcurrentLinkedQueue<PendingConnection> pendingQueue = new ConcurrentLinkedQueue<>();

    /** Currently active connections being tracked */
    private final ConcurrentHashMap<String, ActiveConnection> activeConnections = new ConcurrentHashMap<>();

    /** Connection log entries for UI display */
    private final ConcurrentLinkedQueue<ConnectionLogEntry> connectionLog = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOG_ENTRIES = 500;

    /** Timeout for manual approval (ms) — default 30 seconds */
    private volatile long manualApprovalTimeoutMs = 30_000;

    /** File path for persisting ACL rules */
    private static final String ACL_FILE = "acl.properties";

    /** Listeners for UI updates */
    private final List<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<PendingConnection>> pendingListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<ActiveConnection>> activeConnectionListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<ActiveConnection>> connectionRemovedListeners = new CopyOnWriteArrayList<>();

    // ===================== Constructor =====================

    private ConnectionAccessManager() {
        loadACLFromFile();
    }

    // ===================== Core: Check & Gate =====================

    /**
     * Main entry point: evaluates an incoming socket connection.
     * Called right after serverSocket.accept().
     *
     * @param socket the accepted socket
     * @return true if connection is allowed, false if rejected
     */
    public boolean evaluateConnection(Socket socket) {
        String ip = socket.getInetAddress().getHostAddress();
        int remotePort = socket.getPort();
        int localPort = socket.getLocalPort();

        // Step 1: Always check blacklist first
        if (blacklist.contains(ip)) {
            logConnection(ip, remotePort, localPort, "BLOCKED (Blacklisted)");
            return false;
        }

        // Step 2: Based on approval mode
        if (approvalMode == ApprovalMode.AUTO) {
            return evaluateAutoMode(socket, ip, remotePort, localPort);
        } else {
            return evaluateManualMode(socket, ip, remotePort, localPort);
        }
    }

    /**
     * AUTO mode: check whitelist rules automatically.
     */
    private boolean evaluateAutoMode(Socket socket, String ip, int remotePort, int localPort) {
        // If whitelist is empty → allow all (no restriction)
        if (whitelist.isEmpty()) {
            logConnection(ip, remotePort, localPort, "ALLOWED (Auto - No restrictions)");
            trackConnection(socket);
            return true;
        }

        // If whitelist is non-empty → only allow whitelisted IPs
        if (whitelist.contains(ip)) {
            logConnection(ip, remotePort, localPort, "ALLOWED (Auto - Whitelisted)");
            trackConnection(socket);
            return true;
        } else {
            logConnection(ip, remotePort, localPort, "BLOCKED (Auto - Not in whitelist)");
            return false;
        }
    }

    /**
     * MANUAL mode: put connection in pending queue, wait for admin decision.
     */
    private boolean evaluateManualMode(Socket socket, String ip, int remotePort, int localPort) {
        PendingConnection pending = new PendingConnection(socket);
        pendingQueue.add(pending);
        logConnection(ip, remotePort, localPort, "PENDING (Waiting for admin approval)");

        // Notify UI listeners about new pending connection
        for (Consumer<PendingConnection> listener : pendingListeners) {
            try {
                listener.accept(pending);
            } catch (Exception e) {
                // Don't let listener errors break the flow
            }
        }

        // Block this thread until admin decides (or timeout)
        PendingConnection.Decision decision = pending.waitForDecision(manualApprovalTimeoutMs);
        pendingQueue.remove(pending);

        if (decision == PendingConnection.Decision.APPROVED) {
            logConnection(ip, remotePort, localPort, "APPROVED (By admin)");
            trackConnection(socket);
            return true;
        } else {
            String reason = (decision == PendingConnection.Decision.REJECTED)
                    ? "REJECTED (By admin)" : "REJECTED (Timeout)";
            logConnection(ip, remotePort, localPort, reason);
            return false;
        }
    }

    // ===================== Active Connection Tracking =====================

    /**
     * Track an active connection.
     */
    private void trackConnection(Socket socket) {
        ActiveConnection ac = new ActiveConnection(socket);
        activeConnections.put(ac.getId(), ac);

        for (Consumer<ActiveConnection> listener : activeConnectionListeners) {
            try {
                listener.accept(ac);
            } catch (Exception e) { /* ignore */ }
        }
    }

    /**
     * Remove a connection from tracking (called when connection closes).
     */
    public void untrackConnection(Socket socket) {
        String id = ActiveConnection.buildId(socket);
        ActiveConnection removed = activeConnections.remove(id);
        if (removed != null) {
            for (Consumer<ActiveConnection> listener : connectionRemovedListeners) {
                try {
                    listener.accept(removed);
                } catch (Exception e) { /* ignore */ }
            }
        }
    }

    /**
     * Kick (forcefully close) an active connection by ID.
     */
    public boolean kickConnection(String connectionId) {
        ActiveConnection ac = activeConnections.remove(connectionId);
        if (ac != null) {
            try {
                ac.getSocket().close();
                logEvent("KICKED connection: " + ac.getDisplayInfo());
                for (Consumer<ActiveConnection> listener : connectionRemovedListeners) {
                    try { listener.accept(ac); } catch (Exception e) { /* ignore */ }
                }
                return true;
            } catch (IOException e) {
                logEvent("Failed to kick connection: " + connectionId);
            }
        }
        return false;
    }

    // ===================== Whitelist / Blacklist Management =====================

    public void addToWhitelist(String ip) {
        whitelist.add(ip);
        logEvent("Added to WHITELIST: " + ip);
        saveACLToFile();
    }

    public void removeFromWhitelist(String ip) {
        whitelist.remove(ip);
        logEvent("Removed from WHITELIST: " + ip);
        saveACLToFile();
    }

    public void addToBlacklist(String ip) {
        blacklist.add(ip);
        logEvent("Added to BLACKLIST: " + ip);
        saveACLToFile();

        // Also kick any existing connections from this IP
        kickConnectionsByIP(ip);
    }

    public void removeFromBlacklist(String ip) {
        blacklist.remove(ip);
        logEvent("Removed from BLACKLIST: " + ip);
        saveACLToFile();
    }

    /**
     * Kick all active connections from a specific IP.
     */
    public void kickConnectionsByIP(String ip) {
        List<String> toKick = new ArrayList<>();
        for (Map.Entry<String, ActiveConnection> entry : activeConnections.entrySet()) {
            if (entry.getValue().getRemoteIP().equals(ip)) {
                toKick.add(entry.getKey());
            }
        }
        for (String id : toKick) {
            kickConnection(id);
        }
    }

    public Set<String> getWhitelist() {
        return Collections.unmodifiableSet(new HashSet<>(whitelist));
    }

    public Set<String> getBlacklist() {
        return Collections.unmodifiableSet(new HashSet<>(blacklist));
    }

    // ===================== Mode Management =====================

    public ApprovalMode getApprovalMode() {
        return approvalMode;
    }

    public void setApprovalMode(ApprovalMode mode) {
        ApprovalMode oldMode = this.approvalMode;
        this.approvalMode = mode;
        logEvent("Approval mode changed: " + oldMode + " → " + mode);

        // If switching from MANUAL to AUTO, auto-reject all pending
        if (oldMode == ApprovalMode.MANUAL && mode == ApprovalMode.AUTO) {
            rejectAllPending();
        }
    }

    public long getManualApprovalTimeoutMs() {
        return manualApprovalTimeoutMs;
    }

    public void setManualApprovalTimeoutMs(long timeoutMs) {
        this.manualApprovalTimeoutMs = timeoutMs;
        logEvent("Manual approval timeout set to: " + timeoutMs + "ms");
    }

    // ===================== Pending Queue Operations =====================

    public List<PendingConnection> getPendingConnections() {
        return new ArrayList<>(pendingQueue);
    }

    public void approvePending(String pendingId) {
        for (PendingConnection pc : pendingQueue) {
            if (pc.getId().equals(pendingId)) {
                pc.approve();
                logEvent("Admin APPROVED: " + pc.getDisplayInfo());
                return;
            }
        }
    }

    public void rejectPending(String pendingId) {
        for (PendingConnection pc : pendingQueue) {
            if (pc.getId().equals(pendingId)) {
                pc.reject();
                logEvent("Admin REJECTED: " + pc.getDisplayInfo());
                return;
            }
        }
    }

    public void rejectAllPending() {
        for (PendingConnection pc : pendingQueue) {
            if (pc.getDecision() == PendingConnection.Decision.PENDING) {
                pc.reject();
            }
        }
        pendingQueue.clear();
        logEvent("All pending connections REJECTED");
    }

    // ===================== Getters =====================

    public Collection<ActiveConnection> getActiveConnections() {
        return Collections.unmodifiableCollection(activeConnections.values());
    }

    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    public int getPendingCount() {
        return pendingQueue.size();
    }

    // ===================== Logging =====================

    private void logConnection(String ip, int remotePort, int localPort, String action) {
        String message = String.format("[ACL] %s:%d → port %d | %s", ip, remotePort, localPort, action);
        System.out.println(message);
        addLogEntry(new ConnectionLogEntry(ip, remotePort, localPort, action));
    }

    private void logEvent(String message) {
        String fullMsg = "[ACL] " + message;
        System.out.println(fullMsg);
        addLogEntry(new ConnectionLogEntry("SYSTEM", 0, 0, message));
    }

    private void addLogEntry(ConnectionLogEntry entry) {
        connectionLog.add(entry);
        while (connectionLog.size() > MAX_LOG_ENTRIES) {
            connectionLog.poll();
        }
        // Notify log listeners
        for (Consumer<String> listener : logListeners) {
            try {
                listener.accept(entry.toString());
            } catch (Exception e) { /* ignore */ }
        }
    }

    public List<ConnectionLogEntry> getConnectionLog() {
        return new ArrayList<>(connectionLog);
    }

    // ===================== Listener Registration =====================

    public void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }

    public void removeLogListener(Consumer<String> listener) {
        logListeners.remove(listener);
    }

    public void addPendingListener(Consumer<PendingConnection> listener) {
        pendingListeners.add(listener);
    }

    public void removePendingListener(Consumer<PendingConnection> listener) {
        pendingListeners.remove(listener);
    }

    public void addActiveConnectionListener(Consumer<ActiveConnection> listener) {
        activeConnectionListeners.add(listener);
    }

    public void addConnectionRemovedListener(Consumer<ActiveConnection> listener) {
        connectionRemovedListeners.add(listener);
    }

    // ===================== Persistence =====================

    /**
     * Save ACL rules to file for persistence across restarts.
     */
    public void saveACLToFile() {
        Properties props = new Properties();
        props.setProperty("whitelist", String.join(",", whitelist));
        props.setProperty("blacklist", String.join(",", blacklist));
        props.setProperty("approval_mode", approvalMode.name());
        props.setProperty("manual_timeout_ms", String.valueOf(manualApprovalTimeoutMs));

        try (OutputStream out = new FileOutputStream(ACL_FILE)) {
            props.store(out, "Connection Access Control List — Auto-generated, editable from Admin UI");
            System.out.println("[ACL] Rules saved to " + ACL_FILE);
        } catch (IOException e) {
            System.err.println("[ACL] Failed to save rules: " + e.getMessage());
        }
    }

    /**
     * Load ACL rules from file.
     */
    private void loadACLFromFile() {
        File file = new File(ACL_FILE);
        if (!file.exists()) {
            System.out.println("[ACL] No " + ACL_FILE + " found. Using defaults (AUTO mode, no restrictions).");
            return;
        }

        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);

            // Load whitelist
            String wl = props.getProperty("whitelist", "");
            if (!wl.isEmpty()) {
                for (String ip : wl.split(",")) {
                    String trimmed = ip.trim();
                    if (!trimmed.isEmpty()) whitelist.add(trimmed);
                }
            }

            // Load blacklist
            String bl = props.getProperty("blacklist", "");
            if (!bl.isEmpty()) {
                for (String ip : bl.split(",")) {
                    String trimmed = ip.trim();
                    if (!trimmed.isEmpty()) blacklist.add(trimmed);
                }
            }

            // Load mode
            String mode = props.getProperty("approval_mode", "AUTO");
            try {
                approvalMode = ApprovalMode.valueOf(mode);
            } catch (IllegalArgumentException e) {
                approvalMode = ApprovalMode.AUTO;
            }

            // Load timeout
            String timeout = props.getProperty("manual_timeout_ms", "30000");
            try {
                manualApprovalTimeoutMs = Long.parseLong(timeout);
            } catch (NumberFormatException e) {
                manualApprovalTimeoutMs = 30_000;
            }

            System.out.println("[ACL] Loaded rules from " + ACL_FILE);
            System.out.println("[ACL] Mode: " + approvalMode);
            System.out.println("[ACL] Whitelist: " + whitelist);
            System.out.println("[ACL] Blacklist: " + blacklist);

        } catch (IOException e) {
            System.err.println("[ACL] Failed to load rules: " + e.getMessage());
        }
    }

    // ===================== Inner Classes =====================

    /**
     * Represents a currently active (approved) connection.
     */
    public static class ActiveConnection {
        private final String id;
        private final Socket socket;
        private final String remoteIP;
        private final int remotePort;
        private final int localPort;
        private final LocalDateTime connectedAt;

        public ActiveConnection(Socket socket) {
            this.socket = socket;
            this.remoteIP = socket.getInetAddress().getHostAddress();
            this.remotePort = socket.getPort();
            this.localPort = socket.getLocalPort();
            this.connectedAt = LocalDateTime.now();
            this.id = buildId(socket);
        }

        public static String buildId(Socket socket) {
            return socket.getInetAddress().getHostAddress() + ":" + socket.getPort()
                    + "→" + socket.getLocalPort();
        }

        public String getId() { return id; }
        public Socket getSocket() { return socket; }
        public String getRemoteIP() { return remoteIP; }
        public int getRemotePort() { return remotePort; }
        public int getLocalPort() { return localPort; }
        public LocalDateTime getConnectedAt() { return connectedAt; }

        public String getDisplayInfo() {
            return remoteIP + ":" + remotePort + " → port " + localPort +
                    " [" + connectedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]";
        }
    }

    /**
     * A log entry for connection events.
     */
    public static class ConnectionLogEntry {
        private final String ip;
        private final int remotePort;
        private final int localPort;
        private final String action;
        private final LocalDateTime timestamp;

        public ConnectionLogEntry(String ip, int remotePort, int localPort, String action) {
            this.ip = ip;
            this.remotePort = remotePort;
            this.localPort = localPort;
            this.action = action;
            this.timestamp = LocalDateTime.now();
        }

        public String getIp() { return ip; }
        public int getRemotePort() { return remotePort; }
        public int getLocalPort() { return localPort; }
        public String getAction() { return action; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("[%s] %s:%d → %d | %s",
                    timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")),
                    ip, remotePort, localPort, action);
        }
    }
}

