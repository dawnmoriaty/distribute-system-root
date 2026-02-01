package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker Server that processes client requests.
 * Can run multiple instances on different ports (e.g., 9001, 9002).
 */
public class WorkerServer {

    private static final int DEFAULT_PORT = 9001;
    private static final int THREAD_POOL_SIZE = 10;

    private final int port;
    private final String workerId;
    private final ExecutorService threadPool;
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private volatile boolean running = true;

    public WorkerServer(int port) {
        this.port = port;
        this.workerId = "Worker-" + port;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("========================================");
            System.out.println("  " + workerId + " started on port " + port);
            System.out.println("  Thread Pool Size: " + THREAD_POOL_SIZE);
            System.out.println("  Database: " + DatabaseConnection.getDatabaseInfo());
            System.out.println("========================================");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int connNum = connectionCount.incrementAndGet();
                    System.out.println("[" + workerId + "] Connection #" + connNum +
                            " from " + clientSocket.getRemoteSocketAddress());

                    threadPool.submit(new ClientHandler(clientSocket, workerId));
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
