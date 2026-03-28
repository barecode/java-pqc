import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.*;

/**
 * Post-Quantum HTTPS REST Client with ML-DSA certificate support.
 * 
 * This client connects to a PQC HTTPS server and retrieves SSL/TLS session information.
 */
public class PQCHttpsClient {
    
    private static final String DEFAULT_URL = "https://localhost:8443/ssl-info";
    private static final String DEFAULT_TRUSTSTORE = "certs/client-truststore.p12";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 10000;
    
    // Hostname verifier for localhost testing
    private static final HostnameVerifier LOCALHOST_VERIFIER = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            // For testing purposes, accept localhost
            return hostname.equals("localhost") || hostname.equals("127.0.0.1");
        }
    };
    
    private String trustStorePath;
    private String trustStorePassword;
    private SSLContext sslContext;
    
    /**
     * Create a new PQC HTTPS Client.
     *
     * @param trustStorePath Path to truststore containing trusted certificates
     * @param trustStorePassword Truststore password
     * @throws Exception if SSL context initialization fails
     */
    public PQCHttpsClient(String trustStorePath, String trustStorePassword) throws Exception {
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
        this.sslContext = initializeSSLContext();
    }
    
    /**
     * Initialize SSL context with the configured truststore.
     *
     * @return Configured SSL context
     * @throws Exception if initialization fails
     */
    private SSLContext initializeSSLContext() throws Exception {
        System.out.println("[PQCHttpsClient] Initializing SSL context...");
        System.out.println("[PQCHttpsClient] Truststore path: " + trustStorePath);
        
        try {
            // Load truststore
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            System.out.println("[PQCHttpsClient] KeyStore type: PKCS12");
            
            try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                trustStore.load(fis, trustStorePassword.toCharArray());
                System.out.println("[PQCHttpsClient] ✓ Truststore loaded successfully");
                System.out.println("[PQCHttpsClient] Truststore contains " + trustStore.size() + " entries");
                
                // List trusted certificates for debugging
                java.util.Enumeration<String> aliases = trustStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    System.out.println("[PQCHttpsClient]   - Certificate alias: " + alias);
                    if (trustStore.isCertificateEntry(alias)) {
                        java.security.cert.Certificate cert = trustStore.getCertificate(alias);
                        if (cert instanceof java.security.cert.X509Certificate) {
                            java.security.cert.X509Certificate x509 = (java.security.cert.X509Certificate) cert;
                            System.out.println("[PQCHttpsClient]     Subject: " + x509.getSubjectX500Principal());
                            System.out.println("[PQCHttpsClient]     Issuer: " + x509.getIssuerX500Principal());
                            System.out.println("[PQCHttpsClient]     Valid from: " + x509.getNotBefore());
                            System.out.println("[PQCHttpsClient]     Valid until: " + x509.getNotAfter());
                        }
                    }
                }
            }
            
            // Initialize trust manager factory
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            System.out.println("[PQCHttpsClient] TrustManagerFactory algorithm: " + tmfAlgorithm);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(trustStore);
            System.out.println("[PQCHttpsClient] ✓ TrustManagerFactory initialized");
            
            // Create and initialize SSL context
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), new SecureRandom());
            System.out.println("[PQCHttpsClient] ✓ SSLContext initialized with TLS protocol");
            
            return context;
        } catch (Exception e) {
            System.err.println("[PQCHttpsClient] ✗ SSL context initialization failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Make an HTTPS GET request to the specified URL.
     * 
     * @param urlString URL to connect to
     * @return Response body as string
     * @throws Exception if request fails
     */
    public String get(String urlString) throws Exception {
        System.out.println("\n[PQCHttpsClient] ========== Starting HTTPS Request ==========");
        System.out.println("[PQCHttpsClient] Target URL: " + urlString);
        
        // Open connection
        URL url = new URL(urlString);
        HttpsURLConnection connection = null;
        
        try {
            connection = (HttpsURLConnection) url.openConnection();
            System.out.println("[PQCHttpsClient] Connection created");
            
            // Configure SSL
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            System.out.println("[PQCHttpsClient] SSL socket factory configured");
            
            connection.setHostnameVerifier(LOCALHOST_VERIFIER);
            System.out.println("[PQCHttpsClient] Hostname verifier set (accepts localhost/127.0.0.1)");
            
            // Configure request
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            System.out.println("[PQCHttpsClient] Request configured: GET, timeout=" + CONNECT_TIMEOUT_MS + "ms");
            
            System.out.println("[PQCHttpsClient] Initiating SSL handshake...");
            
            // Connect and get response
            int responseCode = connection.getResponseCode();
            System.out.println("[PQCHttpsClient] ✓ SSL handshake successful");
            System.out.println("[PQCHttpsClient] Response Code: " + responseCode);
            
            if (responseCode == 200) {
                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                }
                
                // Get SSL session info
                System.out.println("[PQCHttpsClient] ✓ Connection successful");
                try {
                    SSLSession session = connection.getSSLSession().orElse(null);
                    if (session != null) {
                        System.out.println("[PQCHttpsClient] SSL Session Details:");
                        System.out.println("    - Cipher Suite: " + session.getCipherSuite());
                        System.out.println("    - Protocol: " + session.getProtocol());
                        System.out.println("    - Peer Host: " + session.getPeerHost());
                        System.out.println("    - Peer Port: " + session.getPeerPort());
                        
                        // Display peer certificates
                        try {
                            java.security.cert.Certificate[] peerCerts = session.getPeerCertificates();
                            System.out.println("    - Peer certificates: " + peerCerts.length);
                            for (int i = 0; i < peerCerts.length; i++) {
                                if (peerCerts[i] instanceof java.security.cert.X509Certificate) {
                                    java.security.cert.X509Certificate x509 = (java.security.cert.X509Certificate) peerCerts[i];
                                    System.out.println("      [" + i + "] Subject: " + x509.getSubjectX500Principal());
                                    System.out.println("      [" + i + "] Issuer: " + x509.getIssuerX500Principal());
                                }
                            }
                        } catch (Exception certEx) {
                            System.out.println("    - Could not retrieve peer certificates: " + certEx.getMessage());
                        }
                    }
                } catch (Exception e) {
                    // SSLSession info not available, continue anyway
                    System.out.println("    - SSL session info not available: " + e.getMessage());
                }
                
                return response.toString();
            } else {
                System.err.println("[PQCHttpsClient] ✗ HTTP request failed with code: " + responseCode);
                
                // Try to read error stream for more details
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()))) {
                    System.err.println("[PQCHttpsClient] Error response:");
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.err.println("    " + line);
                    }
                } catch (Exception e) {
                    System.err.println("[PQCHttpsClient] Could not read error stream: " + e.getMessage());
                }
                
                throw new IOException("HTTP request failed with response code: " + responseCode);
            }
        } catch (javax.net.ssl.SSLHandshakeException e) {
            System.err.println("\n[PQCHttpsClient] ✗✗✗ SSL HANDSHAKE FAILED ✗✗✗");
            System.err.println("[PQCHttpsClient] Error: " + e.getMessage());
            System.err.println("[PQCHttpsClient] Possible causes:");
            System.err.println("    1. Server certificate not trusted (not in truststore)");
            System.err.println("    2. Certificate expired or not yet valid");
            System.err.println("    3. Hostname mismatch");
            System.err.println("    4. Incompatible cipher suites");
            System.err.println("    5. TLS protocol version mismatch");
            System.err.println("\n[PQCHttpsClient] Stack trace:");
            e.printStackTrace();
            throw e;
        } catch (javax.net.ssl.SSLException e) {
            System.err.println("\n[PQCHttpsClient] ✗✗✗ SSL ERROR ✗✗✗");
            System.err.println("[PQCHttpsClient] Error: " + e.getMessage());
            System.err.println("[PQCHttpsClient] Stack trace:");
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("\n[PQCHttpsClient] ✗✗✗ REQUEST FAILED ✗✗✗");
            System.err.println("[PQCHttpsClient] Error type: " + e.getClass().getName());
            System.err.println("[PQCHttpsClient] Error message: " + e.getMessage());
            System.err.println("[PQCHttpsClient] Stack trace:");
            e.printStackTrace();
            throw e;
        } finally {
            // Ensure connection is closed
            if (connection != null) {
                connection.disconnect();
                System.out.println("[PQCHttpsClient] Connection closed");
            }
            System.out.println("[PQCHttpsClient] ========== Request Complete ==========\n");
        }
    }
    
    /**
     * Pretty print JSON response.
     * 
     * @param json JSON string to format
     */
    private static void prettyPrintJSON(String json) {
        System.out.println("\n=== SSL/TLS Session Information ===");
        System.out.println(json);
    }
    /**
     * Test ML-DSA signing by sending a message to the server.
     * 
     * @param message Message to sign
     * @return JSON response with signature
     * @throws Exception if request fails
     */
    public String testMLDSASign(String message) throws Exception {
        System.out.println("\n[PQCHttpsClient] ========== Testing ML-DSA Signing ==========");
        System.out.println("[PQCHttpsClient] Message: \"" + message + "\"");
        
        URL url = new URL("https://localhost:8443/pqc-sign");
        HttpsURLConnection connection = null;
        
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setHostnameVerifier(LOCALHOST_VERIFIER);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "text/plain");
            
            // Send message
            try (OutputStream os = connection.getOutputStream()) {
                os.write(message.getBytes("UTF-8"));
            }
            
            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                }
                
                System.out.println("[PQCHttpsClient] ✓ ML-DSA signature received");
                return response.toString();
            } else {
                throw new IOException("ML-DSA sign request failed with code: " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Test ML-DSA verification by sending a message and signature to the server.
     * 
     * @param message Original message
     * @param signature Base64-encoded signature
     * @return JSON response with verification result
     * @throws Exception if request fails
     */
    public String testMLDSAVerify(String message, String signature) throws Exception {
        System.out.println("\n[PQCHttpsClient] ========== Testing ML-DSA Verification ==========");
        System.out.println("[PQCHttpsClient] Verifying signature...");
        
        URL url = new URL("https://localhost:8443/pqc-verify");
        HttpsURLConnection connection = null;
        
        try {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setHostnameVerifier(LOCALHOST_VERIFIER);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Content-Type", "application/json");
            
            // Build JSON request
            String jsonRequest = "{\n" +
                "  \"message\": \"" + message.replace("\"", "\\\"") + "\",\n" +
                "  \"signature\": \"" + signature + "\"\n" +
                "}";
            
            // Send request
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonRequest.getBytes("UTF-8"));
            }
            
            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                }
                
                System.out.println("[PQCHttpsClient] ✓ ML-DSA verification complete");
                return response.toString();
            } else {
                throw new IOException("ML-DSA verify request failed with code: " + responseCode);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Extract a JSON value from a simple JSON string.
     */
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        // Check if value is a boolean
        String afterColon = json.substring(colonIndex + 1).trim();
        if (afterColon.startsWith("true") || afterColon.startsWith("false")) {
            return afterColon.startsWith("true") ? "true" : "false";
        }
        
        int startQuote = json.indexOf("\"", colonIndex);
        if (startQuote == -1) return null;
        
        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return null;
        
        return json.substring(startQuote + 1, endQuote);
    }
    
    
    /**
     * Main method to run the client.
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== PQC HTTPS Client ===\n");
            
            // Parse command-line arguments
            String url = DEFAULT_URL;
            if (args.length > 0) {
                url = args[0];
                System.out.println("Using URL from command line: " + url);
            } else {
                System.out.println("Using default URL: " + DEFAULT_URL);
                System.out.println("(You can specify a different URL as a command line argument)\n");
            }
            
            // Check if truststore exists
            File truststoreFile = new File(DEFAULT_TRUSTSTORE);
            if (!truststoreFile.exists()) {
                System.err.println("✗ Truststore not found: " + DEFAULT_TRUSTSTORE);
                System.err.println("Please start the server first to generate certificates.");
                System.exit(1);
            }
            
            // Create client
            PQCHttpsClient client = new PQCHttpsClient(
                DEFAULT_TRUSTSTORE,
                KeyStoreManager.getDefaultPassword()
            );
            
            // Make request to get SSL info
            String response = client.get(url);
            
            // Display response
            prettyPrintJSON(response);
            
            System.out.println("\n✓ TLS connection successful with ECDSA certificate!");
            
            // Now demonstrate ML-DSA signing at application level
            System.out.println("\n=== Demonstrating Post-Quantum Cryptography ===");
            System.out.println("Now testing ML-DSA-65 signatures at application level...\n");
            
            // Test ML-DSA signing
            String testMessage = "Hello, Post-Quantum World!";
            String signResponse = client.testMLDSASign(testMessage);
            
            // Extract signature from response
            String signature = extractJsonValue(signResponse, "signature");
            String signatureSize = extractJsonValue(signResponse, "signatureSize");
            
            System.out.println("\nML-DSA Signature Details:");
            System.out.println("  - Algorithm: ML-DSA-65");
            System.out.println("  - Signature size: " + signatureSize + " bytes");
            System.out.println("  - Signature (first 64 chars): " + signature.substring(0, Math.min(64, signature.length())) + "...");
            
            // Test ML-DSA verification with correct signature
            String verifyResponse = client.testMLDSAVerify(testMessage, signature);
            String isValid = extractJsonValue(verifyResponse, "valid");
            
            System.out.println("\nML-DSA Verification:");
            System.out.println("  - Original message: \"" + testMessage + "\"");
            System.out.println("  - Signature valid: " + isValid);
            
            // Test with tampered message
            String tamperedMessage = "Hello, Post-Quantum World?";
            String tamperedVerifyResponse = client.testMLDSAVerify(tamperedMessage, signature);
            String isTamperedValid = extractJsonValue(tamperedVerifyResponse, "valid");
            
            System.out.println("\nML-DSA Verification (Tampered Message):");
            System.out.println("  - Tampered message: \"" + tamperedMessage + "\"");
            System.out.println("  - Signature valid: " + isTamperedValid);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("✓ Hybrid PQC demonstration complete!");
            System.out.println("  - TLS: ECDSA (traditional, compatible)");
            System.out.println("  - Application: ML-DSA-65 (post-quantum secure)");
            System.out.println("=".repeat(60));
            
        } catch (Exception e) {
            System.err.println("\n✗ Client request failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// Made with Bob
