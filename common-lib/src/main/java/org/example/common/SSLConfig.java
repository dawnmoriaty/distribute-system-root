package org.example.common;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * SSL/TLS utility class for secure socket communication.
 *
 * Supports both server-side (SSLServerSocket) and client-side (SSLSocket) connections.
 * Uses Java KeyStore (JKS) for certificates.
 *
 * To generate self-signed certificate:
 * keytool -genkeypair -alias distributed-system -keyalg RSA -keysize 2048
 *         -validity 365 -keystore keystore.jks -storepass changeit
 */
public class SSLConfig {

    private static final String KEYSTORE_FILE = "keystore.jks";
    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String KEY_PASSWORD = "changeit";

    private static volatile SSLContext sslContext;
    private static volatile boolean sslEnabled = false;

    static {
        initializeSSL();
    }

    /**
     * Initializes SSL context from keystore file.
     */
    private static void initializeSSL() {
        try {
            // Check if SSL is enabled via config
            String sslEnabledStr = NetworkConfig.get("SSL_ENABLED", "false");
            sslEnabled = Boolean.parseBoolean(sslEnabledStr);

            if (!sslEnabled) {
                System.out.println("[SSL] SSL is disabled. Set SSL_ENABLED=true in config to enable.");
                return;
            }

            String keystorePath = NetworkConfig.get("SSL_KEYSTORE_PATH", KEYSTORE_FILE);
            String keystorePassword = NetworkConfig.get("SSL_KEYSTORE_PASSWORD", KEYSTORE_PASSWORD);
            String keyPassword = NetworkConfig.get("SSL_KEY_PASSWORD", KEY_PASSWORD);

            // Load keystore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (InputStream is = new FileInputStream(keystorePath)) {
                keyStore.load(is, keystorePassword.toCharArray());
            }

            // Initialize KeyManagerFactory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keyPassword.toCharArray());

            // Initialize TrustManagerFactory (for client to trust server cert)
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // Create SSL context
            sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

            System.out.println("[SSL] ✅ SSL/TLS initialized successfully with TLSv1.3");
            sslEnabled = true;

        } catch (Exception e) {
            System.err.println("[SSL] ⚠️ Failed to initialize SSL: " + e.getMessage());
            System.err.println("[SSL] Falling back to plain sockets.");
            sslEnabled = false;
        }
    }

    /**
     * Checks if SSL is enabled and properly configured.
     */
    public static boolean isSSLEnabled() {
        return sslEnabled && sslContext != null;
    }

    /**
     * Creates an SSL server socket factory.
     */
    public static SSLServerSocketFactory getServerSocketFactory() {
        if (!isSSLEnabled()) {
            throw new IllegalStateException("SSL is not enabled or not properly configured");
        }
        return sslContext.getServerSocketFactory();
    }

    /**
     * Creates an SSL socket factory for clients.
     */
    public static SSLSocketFactory getSocketFactory() {
        if (!isSSLEnabled()) {
            throw new IllegalStateException("SSL is not enabled or not properly configured");
        }
        return sslContext.getSocketFactory();
    }

    /**
     * Creates an SSL socket to connect to a server.
     */
    public static Socket createClientSocket(String host, int port) throws IOException {
        if (isSSLEnabled()) {
            SSLSocket socket = (SSLSocket) getSocketFactory().createSocket(host, port);
            socket.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
            socket.startHandshake();
            return socket;
        } else {
            return new Socket(host, port);
        }
    }

    /**
     * Creates a server socket (SSL or plain based on config).
     */
    public static java.net.ServerSocket createServerSocket(int port) throws IOException {
        if (isSSLEnabled()) {
            SSLServerSocket serverSocket = (SSLServerSocket) getServerSocketFactory().createServerSocket(port);
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
            serverSocket.setNeedClientAuth(false); // Don't require client cert
            return serverSocket;
        } else {
            return new java.net.ServerSocket(port);
        }
    }

    /**
     * Wraps an existing socket with SSL (for upgrading connections).
     */
    public static SSLSocket wrapSocket(Socket socket, String host, int port, boolean clientMode) throws IOException {
        if (!isSSLEnabled()) {
            throw new IllegalStateException("SSL is not enabled");
        }

        SSLSocket sslSocket = (SSLSocket) getSocketFactory().createSocket(
                socket, host, port, true);
        sslSocket.setUseClientMode(clientMode);
        sslSocket.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
        sslSocket.startHandshake();
        return sslSocket;
    }

    /**
     * Gets SSL status message for display.
     */
    public static String getSSLStatus() {
        if (isSSLEnabled()) {
            return "🔒 SSL: Enabled (TLSv1.3)";
        } else {
            return "🔓 SSL: Disabled";
        }
    }
}
