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
            
            // Make request
            String response = client.get(url);
            
            // Display response
            prettyPrintJSON(response);
            
            System.out.println("\n✓ Client request successful!");
            
        } catch (Exception e) {
            System.err.println("\n✗ Client request failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

// Made with Bob
