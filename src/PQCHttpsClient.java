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
    
    private String trustStorePath;
    private String trustStorePassword;
    
    /**
     * Create a new PQC HTTPS Client.
     * 
     * @param trustStorePath Path to truststore containing trusted certificates
     * @param trustStorePassword Truststore password
     */
    public PQCHttpsClient(String trustStorePath, String trustStorePassword) {
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
    }
    
    /**
     * Make an HTTPS GET request to the specified URL.
     * 
     * @param urlString URL to connect to
     * @return Response body as string
     * @throws Exception if request fails
     */
    public String get(String urlString) throws Exception {
        System.out.println("[PQCHttpsClient] Connecting to: " + urlString);
        
        // Load truststore
        System.out.println("[PQCHttpsClient] Loading truststore: " + trustStorePath);
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(trustStorePath)) {
            trustStore.load(fis, trustStorePassword.toCharArray());
        }
        
        // Initialize trust manager factory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        
        // Create SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
        
        // Create hostname verifier (for localhost testing)
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                // For testing purposes, accept localhost
                return hostname.equals("localhost") || hostname.equals("127.0.0.1");
            }
        };
        
        // Open connection
        URL url = new URL(urlString);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setHostnameVerifier(hostnameVerifier);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(10000); // 10 seconds
        
        System.out.println("[PQCHttpsClient] Establishing HTTPS connection...");
        
        // Connect and get response
        int responseCode = connection.getResponseCode();
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
                SSLSession session = ((HttpsURLConnection) connection).getSSLSession().orElse(null);
                if (session != null) {
                    System.out.println("    - Cipher Suite: " + session.getCipherSuite());
                    System.out.println("    - Protocol: " + session.getProtocol());
                }
            } catch (Exception e) {
                // SSLSession info not available, continue anyway
                System.out.println("    - SSL session info not available");
            }
            
            return response.toString();
        } else {
            throw new IOException("HTTP request failed with response code: " + responseCode);
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
