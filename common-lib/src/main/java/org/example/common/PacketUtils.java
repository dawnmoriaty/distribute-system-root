package org.example.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for sending and receiving packets using Length-Prefix Framing protocol.
 *
 * Packet structure:
 * [4 bytes Integer (length)] + [JSON Payload String (UTF-8 encoded)]
 *
 * This prevents packet coalescing (TCP stream fragmentation issues).
 */
public class PacketUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_PACKET_SIZE = 10 * 1024 * 1024; // 10 MB max packet size

    /**
     * Sends an object as a JSON packet using Length-Prefix Framing.
     * Thread-safe: Uses synchronized block on output stream.
     *
     * @param socket  The socket to send data through
     * @param payload The object to serialize and send
     * @throws IOException If an I/O error occurs
     */
    public static void sendPacket(Socket socket, Object payload) throws IOException {
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        String json = objectMapper.writeValueAsString(payload);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        synchronized (outputStream) {
            outputStream.writeInt(data.length);
            outputStream.write(data);
            outputStream.flush();
        }
    }

    /**
     * Receives a JSON packet and deserializes it to the specified class type.
     * Uses Length-Prefix Framing to determine packet boundaries.
     *
     * @param socket The socket to receive data from
     * @param clazz  The class type to deserialize to
     * @param <T>    The type parameter
     * @return The deserialized object
     * @throws IOException If an I/O error occurs or packet is malformed
     */
    public static <T> T receivePacket(Socket socket, Class<T> clazz) throws IOException {
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

        int length = inputStream.readInt();

        // Validate packet size
        if (length < 0 || length > MAX_PACKET_SIZE) {
            throw new IOException("Invalid packet size: " + length);
        }

        byte[] data = new byte[length];
        inputStream.readFully(data);
        String json = new String(data, StandardCharsets.UTF_8);
        return objectMapper.readValue(json, clazz);
    }

    /**
     * Sends raw bytes using Length-Prefix Framing.
     * Used for piping data through Load Balancer.
     *
     * @param socket The socket to send data through
     * @param data   The raw bytes to send
     * @throws IOException If an I/O error occurs
     */
    public static void sendRawPacket(Socket socket, byte[] data) throws IOException {
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        synchronized (outputStream) {
            outputStream.writeInt(data.length);
            outputStream.write(data);
            outputStream.flush();
        }
    }

    /**
     * Receives raw bytes using Length-Prefix Framing.
     * Used for piping data through Load Balancer.
     *
     * @param socket The socket to receive data from
     * @return The raw bytes received, or null if connection is closed
     * @throws IOException If an I/O error occurs
     */
    public static byte[] receiveRawPacket(Socket socket) throws IOException {
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

        try {
            int length = inputStream.readInt();

            if (length < 0 || length > MAX_PACKET_SIZE) {
                throw new IOException("Invalid packet size: " + length);
            }

            byte[] data = new byte[length];
            inputStream.readFully(data);
            return data;
        } catch (EOFException e) {
            return null; // Connection closed gracefully
        }
    }

    /**
     * Gets the shared ObjectMapper instance for JSON operations.
     *
     * @return The ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
