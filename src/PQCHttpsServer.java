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
    private static final String MLDSA_PRIVATE_KEY_PATH = "certs/mldsa-private.key";
    private static final String MLDSA_PUBLIC_KEY_PATH = "certs/mldsa-public.key";
    
    private HttpsServer server;
    private int port;
    private static KeyPair mldsaKeyPair;
    
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
        server.createContext("/pqc-sign", new PQCSignHandler());
        server.createContext("/pqc-verify", new PQCVerifyHandler());
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
        System.out.println("    - https://localhost:" + port + "/pqc-sign");
        System.out.println("    - https://localhost:" + port + "/pqc-verify");
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
                "  \"message\": \"PQC HTTPS Server (Hybrid Approach)\",\n" +
                "  \"description\": \"Uses ECDSA for TLS, ML-DSA for application-level signing\",\n" +
                "  \"endpoints\": [\n" +
                "    \"/ssl-info - Get SSL/TLS session information\",\n" +
                "    \"/pqc-sign - Sign a message with ML-DSA-65\",\n" +
                "    \"/pqc-verify - Verify an ML-DSA-65 signature\"\n" +
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
        
        // Thread-safe date formatter for ISO 8601 timestamps
        private static final SimpleDateFormat ISO_DATE_FORMATTER = createISOFormatter();
        
        private static SimpleDateFormat createISOFormatter() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
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
            appendTimestamp(json);
            appendServerInfo(json, exchange, session);
            appendCipherInfo(json, session);
            appendCertificateInfo(json, session);
            appendSessionInfo(json, session);
            json.append("}");
            return json.toString();
        }
        
        /**
         * Append timestamp to JSON.
         */
        private void appendTimestamp(StringBuilder json) {
            synchronized (ISO_DATE_FORMATTER) {
                json.append("  \"timestamp\": \"")
                    .append(ISO_DATE_FORMATTER.format(new Date()))
                    .append("\",\n");
            }
        }
        
        /**
         * Append server information to JSON.
         */
        private void appendServerInfo(StringBuilder json, HttpExchange exchange, SSLSession session) {
            json.append("  \"server\": {\n");
            json.append("    \"port\": ").append(exchange.getLocalAddress().getPort()).append(",\n");
            json.append("    \"protocol\": \"").append(session.getProtocol()).append("\"\n");
            json.append("  },\n");
        }
        
        /**
         * Append cipher information to JSON.
         */
        private void appendCipherInfo(StringBuilder json, SSLSession session) {
            json.append("  \"cipher\": {\n");
            json.append("    \"suite\": \"").append(session.getCipherSuite()).append("\",\n");
            json.append("    \"protocol\": \"").append(session.getProtocol()).append("\"\n");
            json.append("  },\n");
        }
        
        /**
         * Append certificate information to JSON.
         */
        private void appendCertificateInfo(StringBuilder json, SSLSession session) {
            json.append("  \"certificate\": ");
            try {
                java.security.cert.Certificate[] certs = session.getLocalCertificates();
                if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) certs[0];
                    appendCertificateDetails(json, cert);
                } else {
                    json.append("null,\n");
                }
            } catch (Exception e) {
                json.append("{\"error\": \"").append(escapeJson(e.getMessage())).append("\"},\n");
            }
        }
        
        /**
         * Append detailed certificate information to JSON.
         */
        private void appendCertificateDetails(StringBuilder json, X509Certificate cert) {
            synchronized (ISO_DATE_FORMATTER) {
                json.append("{\n");
                json.append("    \"algorithm\": \"").append(cert.getPublicKey().getAlgorithm()).append("\",\n");
                json.append("    \"subject\": \"").append(escapeJson(cert.getSubjectX500Principal().toString())).append("\",\n");
                json.append("    \"issuer\": \"").append(escapeJson(cert.getIssuerX500Principal().toString())).append("\",\n");
                json.append("    \"serialNumber\": \"").append(cert.getSerialNumber().toString()).append("\",\n");
                json.append("    \"notBefore\": \"").append(ISO_DATE_FORMATTER.format(cert.getNotBefore())).append("\",\n");
                json.append("    \"notAfter\": \"").append(ISO_DATE_FORMATTER.format(cert.getNotAfter())).append("\",\n");
                json.append("    \"signatureAlgorithm\": \"").append(cert.getSigAlgName()).append("\",\n");
                json.append("    \"publicKeySize\": ").append(cert.getPublicKey().getEncoded().length).append(",\n");
                json.append("    \"version\": ").append(cert.getVersion()).append("\n");
                json.append("  },\n");
            }
        }
        
        /**
         * Append session information to JSON.
         */
        private void appendSessionInfo(StringBuilder json, SSLSession session) {
            json.append("  \"session\": {\n");
            json.append("    \"id\": \"").append(bytesToHex(session.getId())).append("\",\n");
            json.append("    \"creationTime\": ").append(session.getCreationTime()).append(",\n");
            json.append("    \"peerHost\": \"").append(session.getPeerHost()).append("\",\n");
            json.append("    \"peerPort\": ").append(session.getPeerPort()).append("\n");
            json.append("  }\n");
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
     * Handler for the /pqc-sign endpoint.
     * Signs a message using ML-DSA-65 and returns the signature.
     */
    private static class PQCSignHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                String error = "{\"error\": \"Method not allowed. Use POST.\"}";
                exchange.sendResponseHeaders(405, error.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
                return;
            }
            
            try {
                // Read message from request body
                String message;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    message = sb.toString();
                }
                
                if (message == null || message.trim().isEmpty()) {
                    String error = "{\"error\": \"Message body is required\"}";
                    exchange.sendResponseHeaders(400, error.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(error.getBytes());
                    }
                    return;
                }
                
                // Sign the message
                String signature = MLDSASignatureHelper.signString(message, mldsaKeyPair.getPrivate());
                
                // Build JSON response
                String response = "{\n" +
                    "  \"message\": \"" + escapeJson(message) + "\",\n" +
                    "  \"signature\": \"" + signature + "\",\n" +
                    "  \"algorithm\": \"ML-DSA-65\",\n" +
                    "  \"signatureSize\": " + java.util.Base64.getDecoder().decode(signature).length + ",\n" +
                    "  \"publicKey\": \"" + java.util.Base64.getEncoder().encodeToString(mldsaKeyPair.getPublic().getEncoded()) + "\"\n" +
                    "}";
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                
            } catch (Exception e) {
                String error = "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}";
                exchange.sendResponseHeaders(500, error.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        }
        
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
     * Handler for the /pqc-verify endpoint.
     * Verifies an ML-DSA-65 signature.
     */
    private static class PQCVerifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                String error = "{\"error\": \"Method not allowed. Use POST.\"}";
                exchange.sendResponseHeaders(405, error.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
                return;
            }
            
            try {
                // Read JSON from request body
                String requestBody;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    requestBody = sb.toString();
                }
                
                // Simple JSON parsing (extract message and signature)
                String message = extractJsonValue(requestBody, "message");
                String signature = extractJsonValue(requestBody, "signature");
                
                if (message == null || signature == null) {
                    String error = "{\"error\": \"Both 'message' and 'signature' fields are required\"}";
                    exchange.sendResponseHeaders(400, error.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(error.getBytes());
                    }
                    return;
                }
                
                // Verify the signature
                boolean isValid = MLDSASignatureHelper.verifyString(message, signature, mldsaKeyPair.getPublic());
                
                // Build JSON response
                String response = "{\n" +
                    "  \"message\": \"" + escapeJson(message) + "\",\n" +
                    "  \"valid\": " + isValid + ",\n" +
                    "  \"algorithm\": \"ML-DSA-65\"\n" +
                    "}";
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                
            } catch (Exception e) {
                String error = "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}";
                exchange.sendResponseHeaders(500, error.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        }
        
        private String extractJsonValue(String json, String key) {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return null;
            
            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) return null;
            
            int startQuote = json.indexOf("\"", colonIndex);
            if (startQuote == -1) return null;
            
            int endQuote = json.indexOf("\"", startQuote + 1);
            if (endQuote == -1) return null;
            
            return json.substring(startQuote + 1, endQuote);
        }
        
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
     * Parse port number from command-line arguments.
     *
     * @param args Command-line arguments
     * @return Port number to use
     */
    private static int parsePort(String[] args) {
        if (args.length == 0) {
            System.out.println("Using default port: " + DEFAULT_PORT);
            System.out.println("(You can specify a different port as a command line argument)\n");
            return DEFAULT_PORT;
        }
        
        try {
            int port = Integer.parseInt(args[0]);
            System.out.println("Using port from command line: " + port);
            return port;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            System.err.println("Using default port: " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
    
    /**
     * Ensure keystore and truststore exist, creating them if necessary.
     *
     * @param javaHome Java home directory path
     * @throws Exception if keystore creation fails
     */
    private static void ensureKeystoresExist(String javaHome) throws Exception {
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
    }
    /**
     * Ensure ML-DSA key pair exists, creating it if necessary.
     * 
     * @throws Exception if key generation or loading fails
     */
    private static void ensureMLDSAKeysExist() throws Exception {
        File publicKeyFile = new File(MLDSA_PUBLIC_KEY_PATH);
        File privateKeyFile = new File(MLDSA_PRIVATE_KEY_PATH);
        
        if (!publicKeyFile.exists() || !privateKeyFile.exists()) {
            System.out.println("[PQCHttpsServer] ML-DSA keys not found, generating new ones...");
            mldsaKeyPair = KeyStoreManager.generateMLDSAKeyPair();
            KeyStoreManager.saveMLDSAKeyPair(mldsaKeyPair, MLDSA_PUBLIC_KEY_PATH, MLDSA_PRIVATE_KEY_PATH);
            System.out.println();
        } else {
            System.out.println("[PQCHttpsServer] Loading existing ML-DSA keys...");
            PublicKey publicKey = KeyStoreManager.loadMLDSAPublicKey(MLDSA_PUBLIC_KEY_PATH, "ML-DSA-65");
            PrivateKey privateKey = KeyStoreManager.loadMLDSAPrivateKey(MLDSA_PRIVATE_KEY_PATH, "ML-DSA-65");
            mldsaKeyPair = new KeyPair(publicKey, privateKey);
            System.out.println("[PQCHttpsServer] ✓ ML-DSA keys loaded");
            System.out.println();
        }
    }
    
    
    /**
     * Setup shutdown hook for graceful server termination.
     *
     * @param server Server instance to shutdown
     */
    private static void setupShutdownHook(PQCHttpsServer server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[PQCHttpsServer] Shutting down gracefully...");
            server.stop(0);
        }));
    }
    
    /**
     * Keep the server running until interrupted.
     *
     * @throws InterruptedException if the wait is interrupted
     */
    private static void keepServerRunning() throws InterruptedException {
        System.out.println("\n✓ Server is running. Press Ctrl+C to stop.");
        // Use a monitor object for more robust thread management
        Object monitor = new Object();
        synchronized (monitor) {
            monitor.wait(); // Will be interrupted by shutdown hook
        }
    }
    
    /**
     * Main method to start the server.
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== PQC HTTPS Server ===\n");
            
            // Parse command-line arguments
            int port = parsePort(args);
            
            // Determine Java home
            String javaHome = System.getProperty("java.home");
            System.out.println("Java Home: " + javaHome + "\n");
            
            // Ensure keystores exist
            ensureKeystoresExist(javaHome);
            
            // Ensure ML-DSA keys exist
            ensureMLDSAKeysExist();
            
            // Create and start server
            PQCHttpsServer server = new PQCHttpsServer(
                port,
                DEFAULT_KEYSTORE,
                KeyStoreManager.getDefaultPassword()
            );
            
            server.start();
            
            // Setup graceful shutdown
            setupShutdownHook(server);
            
            // Keep the server running
            keepServerRunning();
            
        } catch (Exception e) {
            System.err.println("✗ Server failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// Made with Bob
