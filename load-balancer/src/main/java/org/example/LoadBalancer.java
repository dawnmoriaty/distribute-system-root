package org.example;

import org.example.common.NetworkConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Software Load Balancer using Round Robin algorithm.
 * Routes client requests to available Worker Servers.
 *
 * Uses Length-Prefix Framing for packet handling.
 */
public class LoadBalancer {

    private static final int THREAD_POOL_SIZE = 20;

    private final int port;
    private final List<String> workerNodes;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final ExecutorService threadPool;
    private volatile boolean running = true;

    public LoadBalancer() {
        this.port = NetworkConfig.getLoadBalancerPort();
        this.workerNodes = Arrays.asList(NetworkConfig.getWorkerNodes());
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() {
        NetworkConfig.printConfig();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
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

    private void printStartupBanner() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║         LOAD BALANCER STARTED            ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  Port: " + port + "                               ║");
        System.out.println("║  Algorithm: Round Robin                  ║");
        System.out.println("║  Thread Pool: " + THREAD_POOL_SIZE + "                         ║");
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

        try (Socket workerSocket = new Socket(workerHost, workerPort)) {
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
        int index = roundRobinCounter.getAndUpdate(i -> (i + 1) % workerNodes.size());
        return workerNodes.get(index);
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
        System.out.println("[LB] Shutting down...");
    }

    public static void main(String[] args) {
        LoadBalancer lb = new LoadBalancer();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(lb::shutdown));

        lb.start();
    }
}
