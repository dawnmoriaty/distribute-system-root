package org.example;

import org.example.common.*;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

/**
 * Socket client for communicating with the Load Balancer.
 * Uses Length-Prefix Framing protocol.
 *
 * Note: Creates new connection for each request (stateless).
 */
public class SocketClient {

    private static final int SOCKET_TIMEOUT = 60000; // 60 seconds

    private static volatile SocketClient instance;

    private final String loadBalancerHost;
    private final int loadBalancerPort;

    private SocketClient() {
        this.loadBalancerHost = NetworkConfig.getLoadBalancerHost();
        this.loadBalancerPort = NetworkConfig.getLoadBalancerPort();
        System.out.println("[Client] Connecting to Load Balancer at " + getConnectionInfo());
    }

    public static SocketClient getInstance() {
        if (instance == null) {
            synchronized (SocketClient.class) {
                if (instance == null) {
                    instance = new SocketClient();
                }
            }
        }
        return instance;
    }

    /**
     * Sends a request and receives a response.
     * Creates a new connection for each request (connection per request pattern).
     *
     * @param request The request payload to send
     * @return The response from the server
     * @throws IOException If connection fails
     */
    public ResponsePayload sendRequest(RequestPayload request) throws IOException {
        // Generate request ID if not set
        if (request.getRequestId() == null) {
            request.setRequestId(UUID.randomUUID().toString().substring(0, 8));
        }

        try (Socket socket = new Socket(loadBalancerHost, loadBalancerPort)) {
            socket.setSoTimeout(SOCKET_TIMEOUT);

            // Send request
            PacketUtils.sendPacket(socket, request);

            // Receive response
            return PacketUtils.receivePacket(socket, ResponsePayload.class);
        }
    }

    /**
     * Sends a simple command with data.
     */
    public ResponsePayload send(String command, String data) throws IOException {
        return sendRequest(new RequestPayload(command, data));
    }

    /**
     * Pings the server to check connectivity.
     */
    public boolean ping() {
        try {
            ResponsePayload response = send(Commands.PING, null);
            return response.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Gets the connection info string.
     */
    public String getConnectionInfo() {
        return loadBalancerHost + ":" + loadBalancerPort;
    }
}
