package org.example.common;

import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a TCP connection that is pending admin approval.
 * When approval mode is MANUAL, each incoming socket.accept() result
 * is wrapped in this class and placed in a queue for the admin to approve/reject.
 */
public class PendingConnection {

    public enum Decision { PENDING, APPROVED, REJECTED }

    private final String id;
    private final Socket socket;
    private final String remoteIP;
    private final int remotePort;
    private final int localPort;
    private final LocalDateTime arrivedAt;
    private volatile Decision decision = Decision.PENDING;

    // Future that completes when admin makes a decision
    private final CompletableFuture<Decision> decisionFuture = new CompletableFuture<>();

    public PendingConnection(Socket socket) {
        this.socket = socket;
        this.remoteIP = socket.getInetAddress().getHostAddress();
        this.remotePort = socket.getPort();
        this.localPort = socket.getLocalPort();
        this.arrivedAt = LocalDateTime.now();
        this.id = remoteIP + ":" + remotePort + "@" + arrivedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    public String getId() { return id; }
    public Socket getSocket() { return socket; }
    public String getRemoteIP() { return remoteIP; }
    public int getRemotePort() { return remotePort; }
    public int getLocalPort() { return localPort; }
    public LocalDateTime getArrivedAt() { return arrivedAt; }
    public Decision getDecision() { return decision; }

    /**
     * Admin approves this connection.
     */
    public void approve() {
        this.decision = Decision.APPROVED;
        decisionFuture.complete(Decision.APPROVED);
    }

    /**
     * Admin rejects this connection.
     */
    public void reject() {
        this.decision = Decision.REJECTED;
        decisionFuture.complete(Decision.REJECTED);
    }

    /**
     * Wait for admin decision (blocks until approve/reject or timeout).
     * @param timeoutMs max wait time in milliseconds
     * @return the decision, or REJECTED if timeout
     */
    public Decision waitForDecision(long timeoutMs) {
        try {
            return decisionFuture.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Timeout or interruption → auto-reject
            this.decision = Decision.REJECTED;
            return Decision.REJECTED;
        }
    }

    /**
     * Returns display string for UI.
     */
    public String getDisplayInfo() {
        return remoteIP + ":" + remotePort + " → port " + localPort +
                " [" + arrivedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "]";
    }

    @Override
    public String toString() {
        return "PendingConnection{" + remoteIP + ":" + remotePort +
                " → " + localPort + ", decision=" + decision + "}";
    }
}

