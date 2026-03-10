package org.example;

import org.example.common.ConnectionAccessManager;
import org.example.common.SSLConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker Server that processes client requests.
 * Can run multiple instances on different ports (e.g., 9001, 9002).
 * Supports SSL/TLS encryption when enabled.
 * Supports dynamic TCP connection access control (approve/reject).
 */
public class WorkerServer {

    private static final int DEFAULT_PORT = 9001;
    private static final int THREAD_POOL_SIZE = 10;

    private final int port;
    private final String workerId;
    private final ExecutorService threadPool;
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private final ConnectionAccessManager accessManager = ConnectionAccessManager.getInstance();
    private volatile boolean running = true;

    public WorkerServer(int port) {
        this.port = port;
        this.workerId = "Worker-" + port;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public int getPort() {
        return port;
    }

    public String getWorkerId() {
        return workerId;
    }

    public ConnectionAccessManager getAccessManager() {
        return accessManager;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        // Use SSLConfig to create server socket (handles SSL/plain based on config)
        try (ServerSocket serverSocket = SSLConfig.createServerSocket(port)) {
            System.out.println("========================================");
            System.out.println("  " + workerId + " started on port " + port);
            System.out.println("  Thread Pool Size: " + THREAD_POOL_SIZE);
            System.out.println("  Database: " + DatabaseConnection.getDatabaseInfo());
            System.out.println("  " + SSLConfig.getSSLStatus());
            System.out.println("  Access Control Mode: " + accessManager.getApprovalMode());
            System.out.println("========================================");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    // ===== Dynamic TCP Access Control =====
                    // Check ACL rules (blacklist/whitelist) or wait for manual admin approval.
                    // This is the TCP-level "right to refuse" — not hard-coded!
                    if (!accessManager.evaluateConnection(clientSocket)) {
                        System.out.println("[" + workerId + "] Connection REFUSED from "
                                + clientSocket.getRemoteSocketAddress());
                        try { clientSocket.close(); } catch (IOException ignored) {}
                        continue;
                    }

                    int connNum = connectionCount.incrementAndGet();
                    System.out.println("[" + workerId + "] Connection #" + connNum +
                            " ACCEPTED from " + clientSocket.getRemoteSocketAddress());

                    // Wrap client handler to untrack connection on close
                    threadPool.submit(() -> {
                        try {
                            new ClientHandler(clientSocket, workerId).run();
                        } finally {
                            accessManager.untrackConnection(clientSocket);
                        }
                    });
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[" + workerId + "] Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[" + workerId + "] Failed to start: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        running = false;
        threadPool.shutdown();
        System.out.println("[" + workerId + "] Shutting down...");
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0] + ", using default: " + DEFAULT_PORT);
            }
        }

        WorkerServer server = new WorkerServer(port);

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        server.start();
    }
}
