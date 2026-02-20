package org.example;

import org.example.common.NetworkConfig;
import org.example.common.SSLConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Software Load Balancer using Round Robin algorithm.
 * Routes client requests to available Worker Servers.
 *
 * Features:
 * - Round Robin load balancing
 * - Active Health Check (periodic ping to workers)
 * - Automatic failover to healthy workers
 * - SSL/TLS encryption support
 * - Length-Prefix Framing for packet handling
 */
public class LoadBalancer {

    private static final int THREAD_POOL_SIZE = 20;
    private static final int HEALTH_CHECK_INTERVAL_SECONDS = 10;
    private static final int HEALTH_CHECK_TIMEOUT_MS = 3000;

    private final int port;
    private final List<String> workerNodes;
    private final Map<String, Boolean> workerHealth = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final ExecutorService threadPool;
    private final ScheduledExecutorService healthCheckScheduler;
    private volatile boolean running = true;

    public LoadBalancer() {
        this.port = NetworkConfig.getLoadBalancerPort();
        this.workerNodes = Arrays.asList(NetworkConfig.getWorkerNodes());
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.healthCheckScheduler = Executors.newScheduledThreadPool(1);

        // Initialize all workers as healthy
        for (String worker : workerNodes) {
            workerHealth.put(worker, true);
        }
    }

    public void start() {
        NetworkConfig.printConfig();

        // Start health check scheduler
        startHealthCheckScheduler();

        // Use SSLConfig to create server socket (handles SSL/plain based on config)
        try (ServerSocket serverSocket = SSLConfig.createServerSocket(port)) {
            printStartupBanner();

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int connNum = totalConnections.incrementAndGet();
                    activeConnections.incrementAndGet();

                    System.out.println("[LB] Connection #" + connNum +
                            " from " + clientSocket.getRemoteSocketAddress() +
                            " | Active: " + activeConnections.get());

                    threadPool.submit(() -> handleClient(clientSocket, connNum));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[LB] Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[LB] Failed to start: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    /**
     * Starts the periodic health check scheduler.
     */
    private void startHealthCheckScheduler() {
        healthCheckScheduler.scheduleAtFixedRate(() -> {
            System.out.println("[LB] Running health check...");
            for (String worker : workerNodes) {
                boolean healthy = checkWorkerHealth(worker);
                boolean wasHealthy = workerHealth.put(worker, healthy);

                if (healthy != wasHealthy) {
                    if (healthy) {
                        System.out.println("[LB] ✅ Worker " + worker + " is now HEALTHY");
                    } else {
                        System.out.println("[LB] ❌ Worker " + worker + " is now UNHEALTHY");
                    }
                }
            }
            printHealthStatus();
        }, 5, HEALTH_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Checks if a worker is healthy by sending a PING request.
     */
    private boolean checkWorkerHealth(String workerAddress) {
        String[] parts = workerAddress.split(":");
        String host = parts[0];
        int workerPort = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, workerPort), HEALTH_CHECK_TIMEOUT_MS);
            socket.setSoTimeout(HEALTH_CHECK_TIMEOUT_MS);

            // Send PING request using Length-Prefix Framing
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            String pingRequest = "{\"command\":\"PING\",\"data\":null,\"requestId\":\"health-check\",\"timestamp\":" + System.currentTimeMillis() + "}";
            byte[] data = pingRequest.getBytes(StandardCharsets.UTF_8);

            out.writeInt(data.length);
            out.write(data);
            out.flush();

            // Read response
            int length = in.readInt();
            if (length > 0 && length < 1024 * 1024) {
                byte[] response = new byte[length];
                in.readFully(response);
                String responseStr = new String(response, StandardCharsets.UTF_8);
                return responseStr.contains("SUCCESS") || responseStr.contains("PONG");
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Prints the current health status of all workers.
     */
    private void printHealthStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("[LB] Health Status: ");
        for (String worker : workerNodes) {
            boolean healthy = workerHealth.getOrDefault(worker, false);
            sb.append(worker).append("=").append(healthy ? "✅" : "❌").append(" ");
        }
        System.out.println(sb.toString());
    }

    private void printStartupBanner() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║         LOAD BALANCER STARTED            ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  Port: " + port + "                               ║");
        System.out.println("║  Algorithm: Round Robin                  ║");
        System.out.println("║  Thread Pool: " + THREAD_POOL_SIZE + "                         ║");
        System.out.println("║  Health Check: Every " + HEALTH_CHECK_INTERVAL_SECONDS + "s               ║");
        System.out.printf("║  %-40s║%n", SSLConfig.getSSLStatus());
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  Worker Nodes:                           ║");
        for (String node : workerNodes) {
            System.out.printf("║    - %-35s║%n", node);
        }
        System.out.println("╚══════════════════════════════════════════╝");
    }

    private void handleClient(Socket clientSocket, int connNum) {
        String workerAddress = getNextWorkerNode();
        String[] parts = workerAddress.split(":");
        String workerHost = parts[0];
        int workerPort = Integer.parseInt(parts[1]);

        System.out.println("[LB] Routing connection #" + connNum + " to " + workerAddress);

        // Use SSLConfig to create socket to worker (handles SSL/plain based on config)
        try (Socket workerSocket = SSLConfig.createClientSocket(workerHost, workerPort)) {
            // Set socket options
            clientSocket.setSoTimeout(60000); // 60 seconds timeout
            workerSocket.setSoTimeout(60000);

            // Create bidirectional pipe using Length-Prefix Framing
            Thread clientToWorker = new Thread(() ->
                    pipePackets(clientSocket, workerSocket, "Client->Worker #" + connNum),
                    "C2W-" + connNum);
            Thread workerToClient = new Thread(() ->
                    pipePackets(workerSocket, clientSocket, "Worker->Client #" + connNum),
                    "W2C-" + connNum);

            clientToWorker.start();
            workerToClient.start();

            // Wait for both threads to complete
            clientToWorker.join();
            workerToClient.join();

            System.out.println("[LB] Connection #" + connNum + " completed");

        } catch (IOException e) {
            System.err.println("[LB] Worker connection error for #" + connNum + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[LB] Connection #" + connNum + " interrupted");
        } finally {
            activeConnections.decrementAndGet();
            closeSocket(clientSocket);
        }
    }

    private String getNextWorkerNode() {
        // Get list of healthy workers
        List<String> healthyWorkers = workerNodes.stream()
                .filter(w -> workerHealth.getOrDefault(w, false))
                .toList();

        // Use final variable for lambda
        final List<String> finalWorkers;
        if (healthyWorkers.isEmpty()) {
            // Fallback to all workers if none are healthy (let it fail at connection time)
            System.err.println("[LB] WARNING: No healthy workers available! Using all workers.");
            finalWorkers = workerNodes;
        } else {
            finalWorkers = healthyWorkers;
        }

        int index = roundRobinCounter.getAndUpdate(i -> (i + 1) % finalWorkers.size());
        return finalWorkers.get(index % finalWorkers.size());
    }

    /**
     * Pipes packets from input socket to output socket using Length-Prefix Framing.
     * Properly handles packet boundaries to prevent TCP stream issues.
     */
    private void pipePackets(Socket inputSocket, Socket outputSocket, String label) {
        try {
            DataInputStream in = new DataInputStream(inputSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(outputSocket.getOutputStream());

            while (!inputSocket.isClosed() && !outputSocket.isClosed()) {
                try {
                    // Read length prefix (4 bytes)
                    int length = in.readInt();

                    if (length < 0 || length > 10 * 1024 * 1024) {
                        System.err.println("[LB] " + label + " - Invalid packet size: " + length);
                        break;
                    }

                    // Read payload
                    byte[] data = new byte[length];
                    in.readFully(data);

                    // Write to output with length prefix
                    synchronized (out) {
                        out.writeInt(length);
                        out.write(data);
                        out.flush();
                    }

                } catch (EOFException e) {
                    // Connection closed gracefully
                    break;
                }
            }
        } catch (SocketException e) {
            // Socket closed, this is expected
        } catch (IOException e) {
            System.err.println("[LB] " + label + " - Pipe error: " + e.getMessage());
        }
    }

    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore close error
        }
    }

    public void shutdown() {
        running = false;
        threadPool.shutdown();
        healthCheckScheduler.shutdown();
        System.out.println("[LB] Shutting down...");
    }

    public static void main(String[] args) {
        LoadBalancer lb = new LoadBalancer();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(lb::shutdown));

        lb.start();
    }
}
