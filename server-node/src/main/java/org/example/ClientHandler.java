package org.example;

import org.example.common.*;

import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles client requests on the Worker Server.
 * Each connection is processed in its own thread.
 */
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final String workerId;

    public ClientHandler(Socket clientSocket, String workerId) {
        this.clientSocket = clientSocket;
        this.workerId = workerId;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        try {
            RequestPayload request = PacketUtils.receivePacket(clientSocket, RequestPayload.class);
            System.out.println("[" + workerId + "] Received: " + request.getCommand() +
                    " | Data: " + request.getData());

            ResponsePayload response = handleRequest(request);
            response.setWorkerId(workerId);
            response.setRequestId(request.getRequestId());
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            PacketUtils.sendPacket(clientSocket, response);
            System.out.println("[" + workerId + "] Response sent in " +
                    response.getProcessingTimeMs() + "ms");

        } catch (IOException e) {
            System.err.println("[" + workerId + "] IO Error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore close error
            }
        }
    }

    private ResponsePayload handleRequest(RequestPayload request) {
        String command = request.getCommand();
        String data = request.getData();

        try {
            switch (command) {
                case Commands.GET_USER:
                    return queryUser(data);

                case Commands.GET_ALL_USERS:
                    return getAllUsers();

                case Commands.CREATE_USER:
                    return createUser(data);

                case Commands.UPDATE_USER:
                    return updateUser(data);

                case Commands.DELETE_USER:
                    return deleteUser(data);

                case Commands.SEARCH_USERS:
                    return searchUsers(data);

                case Commands.GET_LARGE_DATA:
                    return getLargeData();

                case Commands.PING:
                    return ResponsePayload.success("PONG", "Server is alive");

                case Commands.HEALTH_CHECK:
                    return healthCheck();

                case Commands.GET_STATS:
                    return getStats();

                default:
                    return ResponsePayload.error("Unknown command: " + command);
            }
        } catch (Exception e) {
            return ResponsePayload.error("Server error: " + e.getMessage());
        }
    }

    private ResponsePayload queryUser(String idStr) {
        // Sharding logic: ID chan vao Shard A, ID le vao Shard B
        try {
            long userId = Long.parseLong(idStr);
            String shard = (userId % 2 == 0) ? "SHARD_A" : "SHARD_B";
            System.out.println("[" + workerId + "] Routing user " + userId + " to " + shard);
        } catch (NumberFormatException e) {
            // Not a numeric ID, use default shard
        }

        try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
            String query = "SELECT id, username, email, full_name, created_at FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, idStr);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        UserDTO user = new UserDTO();
                        user.setId(rs.getLong("id"));
                        user.setUsername(rs.getString("username"));
                        user.setEmail(rs.getString("email"));
                        user.setFullName(rs.getString("full_name"));
                        user.setCreatedAt(rs.getString("created_at"));

                        String json = PacketUtils.getObjectMapper().writeValueAsString(user);
                        return ResponsePayload.success(json, "User found");
                    } else {
                        return ResponsePayload.notFound("User not found with ID: " + idStr);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[" + workerId + "] DB Error: " + e.getMessage());
            return ResponsePayload.error("Database error: " + e.getMessage());
        } catch (Exception e) {
            return ResponsePayload.error("Error: " + e.getMessage());
        }
    }

    private ResponsePayload getAllUsers() {
        List<UserDTO> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
            String query = "SELECT id, username, email, full_name, created_at FROM users LIMIT 100";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    UserDTO user = new UserDTO();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setFullName(rs.getString("full_name"));
                    user.setCreatedAt(rs.getString("created_at"));
                    users.add(user);
                }
            }

            ResponsePayload response = ResponsePayload.success(null, "Found " + users.size() + " users");
            response.setUsers(users);
            return response;

        } catch (SQLException e) {
            System.err.println("[" + workerId + "] DB Error: " + e.getMessage());
            return ResponsePayload.error("Database error: " + e.getMessage());
        }
    }

    private ResponsePayload createUser(String jsonData) {
        try {
            UserDTO user = PacketUtils.getObjectMapper().readValue(jsonData, UserDTO.class);

            try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
                String query = "INSERT INTO users (username, email, full_name) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, user.getUsername());
                    stmt.setString(2, user.getEmail());
                    stmt.setString(3, user.getFullName());

                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        return ResponsePayload.success(null, "User created successfully");
                    } else {
                        return ResponsePayload.error("Failed to create user");
                    }
                }
            }
        } catch (Exception e) {
            return ResponsePayload.error("Error creating user: " + e.getMessage());
        }
    }

    private ResponsePayload searchUsers(String keyword) {
        List<UserDTO> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
            String query = "SELECT id, username, email, full_name, created_at FROM users " +
                    "WHERE username LIKE ? OR email LIKE ? OR full_name LIKE ? LIMIT 50";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                String pattern = "%" + keyword + "%";
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                stmt.setString(3, pattern);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        UserDTO user = new UserDTO();
                        user.setId(rs.getLong("id"));
                        user.setUsername(rs.getString("username"));
                        user.setEmail(rs.getString("email"));
                        user.setFullName(rs.getString("full_name"));
                        user.setCreatedAt(rs.getString("created_at"));
                        users.add(user);
                    }
                }
            }

            ResponsePayload response = ResponsePayload.success(null, "Found " + users.size() + " users");
            response.setUsers(users);
            return response;

        } catch (SQLException e) {
            return ResponsePayload.error("Database error: " + e.getMessage());
        }
    }

    private ResponsePayload getLargeData() {
        // Simulate large data processing
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Data row ").append(i).append(": Lorem ipsum dolor sit amet\n");
        }

        // Simulate processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponsePayload.success(sb.toString(), "Large data generated");
    }

    private ResponsePayload healthCheck() {
        try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
            if (conn.isValid(5)) {
                return ResponsePayload.success("HEALTHY", "Database connection OK");
            } else {
                return ResponsePayload.error("Database connection invalid");
            }
        } catch (SQLException e) {
            return ResponsePayload.error("Health check failed: " + e.getMessage());
        }
    }

    /**
     * Updates an existing user.
     * Expected data format: JSON with id, username, email, fullName
     */
    private ResponsePayload updateUser(String jsonData) {
        try {
            UserDTO user = PacketUtils.getObjectMapper().readValue(jsonData, UserDTO.class);

            if (user.getId() == null) {
                return ResponsePayload.error("User ID is required for update");
            }

            try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
                String query = "UPDATE users SET username = ?, email = ?, full_name = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, user.getUsername());
                    stmt.setString(2, user.getEmail());
                    stmt.setString(3, user.getFullName());
                    stmt.setLong(4, user.getId());

                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        return ResponsePayload.success(null, "User updated successfully");
                    } else {
                        return ResponsePayload.notFound("User not found with ID: " + user.getId());
                    }
                }
            }
        } catch (Exception e) {
            return ResponsePayload.error("Error updating user: " + e.getMessage());
        }
    }

    /**
     * Deletes a user by ID.
     * Expected data: user ID as string
     */
    private ResponsePayload deleteUser(String idStr) {
        try {
            long userId = Long.parseLong(idStr);

            try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
                String query = "DELETE FROM users WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setLong(1, userId);

                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        return ResponsePayload.success(null, "User deleted successfully");
                    } else {
                        return ResponsePayload.notFound("User not found with ID: " + idStr);
                    }
                }
            }
        } catch (NumberFormatException e) {
            return ResponsePayload.error("Invalid user ID format: " + idStr);
        } catch (SQLException e) {
            return ResponsePayload.error("Database error: " + e.getMessage());
        }
    }

    /**
     * Returns server statistics.
     */
    private ResponsePayload getStats() {
        try (Connection conn = DatabaseConnection.getDataSource().getConnection()) {
            String query = "SELECT COUNT(*) as total FROM users";
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    long totalUsers = rs.getLong("total");
                    String stats = String.format(
                        "{\"workerId\":\"%s\",\"totalUsers\":%d,\"timestamp\":%d}",
                        workerId, totalUsers, System.currentTimeMillis()
                    );
                    return ResponsePayload.success(stats, "Stats retrieved");
                }
            }
            return ResponsePayload.error("Could not retrieve stats");
        } catch (SQLException e) {
            return ResponsePayload.error("Database error: " + e.getMessage());
        }
    }
}
