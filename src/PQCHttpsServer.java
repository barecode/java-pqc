import com.sun.net.httpserver.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Post-Quantum HTTPS REST Server with ML-DSA certificate support.
 * 
 * This server provides an endpoint that returns comprehensive SSL/TLS session information
 * including cipher suite details, certificate information, and session metadata.
 */
public class PQCHttpsServer {
    
    private static final int DEFAULT_PORT = 8443;
    private static final String DEFAULT_KEYSTORE = "certs/server-keystore.p12";
    private static final String DEFAULT_TRUSTSTORE = "certs/client-truststore.p12";
    
    private HttpsServer server;
    private int port;
    
    /**
     * Create a new PQC HTTPS Server.
     * 
     * @param port Port to listen on
     * @param keystorePath Path to keystore containing server certificate
     * @param keystorePassword Keystore password
     * @throws Exception if server creation fails
     */
    public PQCHttpsServer(int port, String keystorePath, String keystorePassword) throws Exception {
        this.port = port;
        
        System.out.println("[PQCHttpsServer] Initializing server on port " + port + "...");
        
        // Load keystore
        System.out.println("[PQCHttpsServer] Loading keystore: " + keystorePath);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        
        // Initialize key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());
        
        // Initialize trust manager factory (optional, for mutual TLS)
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        
        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        
        // Create HTTPS server
        server = HttpsServer.create(new InetSocketAddress(port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                try {
                    SSLContext context = getSSLContext();
                    SSLEngine engine = context.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());
                    
                    SSLParameters sslParams = context.getDefaultSSLParameters();
                    params.setSSLParameters(sslParams);
                } catch (Exception e) {
                    System.err.println("Failed to configure HTTPS: " + e.getMessage());
                }
            }
        });
        
        // Register endpoints
        server.createContext("/ssl-info", new SSLInfoHandler());
        server.createContext("/", new RootHandler());
        
        // Set executor (null = default executor)
        server.setExecutor(null);
        
        System.out.println("[PQCHttpsServer] ✓ Server initialized successfully");
    }
    
    /**
     * Start the server.
     */
    public void start() {
        server.start();
        System.out.println("[PQCHttpsServer] ✓ Server started on port " + port);
        System.out.println("[PQCHttpsServer] Endpoints:");
        System.out.println("    - https://localhost:" + port + "/");
        System.out.println("    - https://localhost:" + port + "/ssl-info");
    }
    
    /**
     * Stop the server.
     * 
     * @param delay Delay in seconds before stopping
     */
    public void stop(int delay) {
        server.stop(delay);
        System.out.println("[PQCHttpsServer] Server stopped");
    }
    
    /**
     * Handler for the root endpoint.
     */
    private static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\n" +
                "  \"message\": \"PQC HTTPS Server\",\n" +
                "  \"endpoints\": [\n" +
                "    \"/ssl-info - Get SSL/TLS session information\"\n" +
                "  ]\n" +
                "}";
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Handler for the /ssl-info endpoint.
     * Returns comprehensive SSL/TLS session information as JSON.
     */
    private static class SSLInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!(exchange instanceof HttpsExchange)) {
                String error = "{\"error\": \"This endpoint requires HTTPS\"}";
                exchange.sendResponseHeaders(400, error.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
                return;
            }
            
            HttpsExchange httpsExchange = (HttpsExchange) exchange;
            SSLSession sslSession = httpsExchange.getSSLSession();
            
            try {
                String jsonResponse = buildSSLInfoJSON(sslSession, exchange);
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
            } catch (Exception e) {
                String error = "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}";
                exchange.sendResponseHeaders(500, error.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        }
        
        /**
         * Build JSON response with SSL session information.
         */
        private String buildSSLInfoJSON(SSLSession session, HttpExchange exchange) throws Exception {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            
            // Timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            json.append("  \"timestamp\": \"").append(sdf.format(new Date())).append("\",\n");
            
            // Server information
            json.append("  \"server\": {\n");
            json.append("    \"port\": ").append(exchange.getLocalAddress().getPort()).append(",\n");
            json.append("    \"protocol\": \"").append(session.getProtocol()).append("\"\n");
            json.append("  },\n");
            
            // Cipher information
            json.append("  \"cipher\": {\n");
            json.append("    \"suite\": \"").append(session.getCipherSuite()).append("\",\n");
            json.append("    \"protocol\": \"").append(session.getProtocol()).append("\"\n");
            json.append("  },\n");
            
            // Certificate information
            json.append("  \"certificate\": ");
            try {
                java.security.cert.Certificate[] certs = session.getLocalCertificates();
                if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) certs[0];
                    json.append("{\n");
                    json.append("    \"algorithm\": \"").append(cert.getPublicKey().getAlgorithm()).append("\",\n");
                    json.append("    \"subject\": \"").append(escapeJson(cert.getSubjectX500Principal().toString())).append("\",\n");
                    json.append("    \"issuer\": \"").append(escapeJson(cert.getIssuerX500Principal().toString())).append("\",\n");
                    json.append("    \"serialNumber\": \"").append(cert.getSerialNumber().toString()).append("\",\n");
                    json.append("    \"notBefore\": \"").append(sdf.format(cert.getNotBefore())).append("\",\n");
                    json.append("    \"notAfter\": \"").append(sdf.format(cert.getNotAfter())).append("\",\n");
                    json.append("    \"signatureAlgorithm\": \"").append(cert.getSigAlgName()).append("\",\n");
                    json.append("    \"publicKeySize\": ").append(cert.getPublicKey().getEncoded().length).append(",\n");
                    json.append("    \"version\": ").append(cert.getVersion()).append("\n");
                    json.append("  },\n");
                } else {
                    json.append("null,\n");
                }
            } catch (Exception e) {
                json.append("{\"error\": \"").append(escapeJson(e.getMessage())).append("\"},\n");
            }
            
            // Session information
            json.append("  \"session\": {\n");
            json.append("    \"id\": \"").append(bytesToHex(session.getId())).append("\",\n");
            json.append("    \"creationTime\": ").append(session.getCreationTime()).append(",\n");
            json.append("    \"peerHost\": \"").append(session.getPeerHost()).append("\",\n");
            json.append("    \"peerPort\": ").append(session.getPeerPort()).append("\n");
            json.append("  }\n");
            
            json.append("}");
            
            return json.toString();
        }
        
        /**
         * Convert byte array to hex string.
         */
        private String bytesToHex(byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return "";
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        
        /**
         * Escape special characters for JSON.
         */
        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
        }
    }
    
    /**
     * Main method to start the server.
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== PQC HTTPS Server ===\n");
            
            // Parse command-line arguments
            int port = DEFAULT_PORT;
            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                    System.out.println("Using port from command line: " + port);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[0]);
                    System.err.println("Using default port: " + DEFAULT_PORT);
                }
            } else {
                System.out.println("Using default port: " + DEFAULT_PORT);
                System.out.println("(You can specify a different port as a command line argument)\n");
            }
            
            // Determine Java home
            String javaHome = System.getProperty("java.home");
            System.out.println("Java Home: " + javaHome + "\n");
            
            // Create certs directory
            new File("certs").mkdirs();
            
            // Check if keystore exists, create if not
            File keystoreFile = new File(DEFAULT_KEYSTORE);
            if (!keystoreFile.exists()) {
                System.out.println("[PQCHttpsServer] Keystore not found, creating new one...");
                KeyStoreManager.createKeyStoreWithCertificate(DEFAULT_KEYSTORE, javaHome);
                System.out.println();
            }
            
            // Check if truststore exists, create if not
            File truststoreFile = new File(DEFAULT_TRUSTSTORE);
            if (!truststoreFile.exists()) {
                System.out.println("[PQCHttpsServer] Truststore not found, creating new one...");
                KeyStoreManager.exportCertificateToTrustStore(DEFAULT_KEYSTORE, DEFAULT_TRUSTSTORE, javaHome);
                System.out.println();
            }
            
            // Create and start server
            PQCHttpsServer server = new PQCHttpsServer(
                port,
                DEFAULT_KEYSTORE,
                KeyStoreManager.getDefaultPassword()
            );
            
            server.start();
            
            System.out.println("\n✓ Server is running. Press Ctrl+C to stop.");
            
            // Keep the server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("✗ Server failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// Made with Bob
