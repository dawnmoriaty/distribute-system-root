package org.example.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Network configuration for distributed system.
 * Loads configuration from config.properties or environment variables.
 */
public class NetworkConfig {

    private static final Properties properties = new Properties();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        // Try to load from config.properties file
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            System.out.println("[Config] Loaded from config.properties");
        } catch (IOException e) {
            System.out.println("[Config] No config.properties found, using defaults/env vars");
        }
    }

    /**
     * Get property with priority: Environment Variable > config.properties > default
     */
    public static String get(String key, String defaultValue) {
        // 1. Check environment variable first
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // 2. Check properties file
        String propValue = properties.getProperty(key);
        if (propValue != null && !propValue.isEmpty()) {
            return propValue;
        }

        // 3. Return default
        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ============== Load Balancer Config ==============
    public static String getLoadBalancerHost() {
        return get("LOAD_BALANCER_HOST", "localhost");
    }

    public static int getLoadBalancerPort() {
        return getInt("LOAD_BALANCER_PORT", 8080);
    }

    // ============== Worker Config ==============
    public static String getWorker1Host() {
        return get("WORKER1_HOST", "localhost");
    }

    public static int getWorker1Port() {
        return getInt("WORKER1_PORT", 9001);
    }

    public static String getWorker2Host() {
        return get("WORKER2_HOST", "localhost");
    }

    public static int getWorker2Port() {
        return getInt("WORKER2_PORT", 9002);
    }

    /**
     * Get all worker nodes as "host:port" array
     */
    public static String[] getWorkerNodes() {
        return new String[] {
            getWorker1Host() + ":" + getWorker1Port(),
            getWorker2Host() + ":" + getWorker2Port()
        };
    }

    // ============== Database Config ==============
    public static String getDbUrl() {
        return get("DB_URL", "jdbc:mysql://localhost:3306/distributed_db");
    }

    public static String getDbUser() {
        return get("DB_USER", "root");
    }

    public static String getDbPassword() {
        return get("DB_PASSWORD", "password");
    }

    /**
     * Print current configuration
     */
    public static void printConfig() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║            NETWORK CONFIGURATION                 ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║ Load Balancer: " + padRight(getLoadBalancerHost() + ":" + getLoadBalancerPort(), 33) + "║");
        System.out.println("║ Worker 1:      " + padRight(getWorker1Host() + ":" + getWorker1Port(), 33) + "║");
        System.out.println("║ Worker 2:      " + padRight(getWorker2Host() + ":" + getWorker2Port(), 33) + "║");
        System.out.println("║ Database:      " + padRight(getDbUrl(), 33) + "║");
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    private static String padRight(String s, int n) {
        if (s.length() > n) {
            return s.substring(0, n - 3) + "...";
        }
        return String.format("%-" + n + "s", s);
    }
}
