import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.nio.file.*;

/**
 * Manages keystores and truststores for ML-DSA certificates.
 * 
 * This class handles creation, loading, and management of Java KeyStores (JKS)
 * containing ML-DSA certificates for HTTPS connections.
 */
public class KeyStoreManager {
    
    private static final String KEYSTORE_TYPE = "JKS";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";
    private static final String DEFAULT_KEY_ALIAS = "pqc-server";
    
    /**
     * Create a keystore with an ML-DSA certificate using keytool.
     * This method generates a self-signed certificate directly in the keystore.
     * 
     * @param keystorePath Path where the keystore will be created
     * @param alias Alias for the key entry
     * @param password Keystore password
     * @param dname Distinguished name (e.g., "CN=localhost, O=PQC, C=US")
     * @param algorithm ML-DSA algorithm variant (ML-DSA-44, ML-DSA-65, or ML-DSA-87)
     * @param validityDays Number of days the certificate should be valid
     * @param javaHome Path to Java home directory
     * @throws Exception if keystore creation fails
     */
    public static void createKeyStoreWithCertificate(
            String keystorePath,
            String alias,
            String password,
            String dname,
            String algorithm,
            int validityDays,
            String javaHome) throws Exception {
        
        System.out.println("[KeyStoreManager] Creating keystore with ML-DSA certificate...");
        System.out.println("    - Keystore: " + keystorePath);
        System.out.println("    - Alias: " + alias);
        System.out.println("    - Algorithm: " + algorithm);
        System.out.println("    - DN: " + dname);
        System.out.println("    - Validity: " + validityDays + " days");
        
        // Delete existing keystore if it exists
        File keystoreFile = new File(keystorePath);
        if (keystoreFile.exists()) {
            System.out.println("    - Deleting existing keystore...");
            keystoreFile.delete();
        }
        
        // Ensure parent directory exists
        File parentDir = keystoreFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // Build keytool command
        String keytoolPath = javaHome + "/bin/keytool";
        ProcessBuilder pb = new ProcessBuilder(
            keytoolPath,
            "-genkeypair",
            "-alias", alias,
            "-keyalg", algorithm,
            "-keystore", keystorePath,
            "-storepass", password,
            "-keypass", password,
            "-dname", dname,
            "-validity", String.valueOf(validityDays),
            "-storetype", KEYSTORE_TYPE
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Capture output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("    [keytool] " + line);
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("keytool failed with exit code: " + exitCode);
        }
        
        System.out.println("[KeyStoreManager] ✓ Keystore created successfully");
    }
    
    /**
     * Create a keystore with default settings.
     * 
     * @param keystorePath Path where the keystore will be created
     * @param javaHome Path to Java home directory
     * @throws Exception if keystore creation fails
     */
    public static void createKeyStoreWithCertificate(String keystorePath, String javaHome) throws Exception {
        createKeyStoreWithCertificate(
            keystorePath,
            DEFAULT_KEY_ALIAS,
            DEFAULT_KEYSTORE_PASSWORD,
            "CN=localhost, O=PQC-HTTPS-Server, C=US",
            "ML-DSA-65",
            365,
            javaHome
        );
    }
    
    /**
     * Load an existing keystore from a file.
     * 
     * @param keystorePath Path to the keystore file
     * @param password Keystore password
     * @return Loaded KeyStore object
     * @throws Exception if loading fails
     */
    public static KeyStore loadKeyStore(String keystorePath, String password) throws Exception {
        System.out.println("[KeyStoreManager] Loading keystore from: " + keystorePath);
        
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, password.toCharArray());
        }
        
        System.out.println("[KeyStoreManager] ✓ Keystore loaded successfully");
        System.out.println("    - Size: " + keyStore.size() + " entries");
        
        return keyStore;
    }
    
    /**
     * Load keystore with default password.
     * 
     * @param keystorePath Path to the keystore file
     * @return Loaded KeyStore object
     * @throws Exception if loading fails
     */
    public static KeyStore loadKeyStore(String keystorePath) throws Exception {
        return loadKeyStore(keystorePath, DEFAULT_KEYSTORE_PASSWORD);
    }
    
    /**
     * Export a certificate from a keystore to create a truststore.
     * 
     * @param sourceKeystorePath Path to source keystore
     * @param sourcePassword Source keystore password
     * @param sourceAlias Alias of the certificate to export
     * @param trustStorePath Path where truststore will be created
     * @param trustStorePassword Truststore password
     * @param javaHome Path to Java home directory
     * @throws Exception if export fails
     */
    public static void exportCertificateToTrustStore(
            String sourceKeystorePath,
            String sourcePassword,
            String sourceAlias,
            String trustStorePath,
            String trustStorePassword,
            String javaHome) throws Exception {
        
        System.out.println("[KeyStoreManager] Exporting certificate to truststore...");
        System.out.println("    - Source: " + sourceKeystorePath);
        System.out.println("    - Destination: " + trustStorePath);
        System.out.println("    - Alias: " + sourceAlias);
        
        // Delete existing truststore if it exists
        File trustStoreFile = new File(trustStorePath);
        if (trustStoreFile.exists()) {
            System.out.println("    - Deleting existing truststore...");
            trustStoreFile.delete();
        }
        
        // Ensure parent directory exists
        File parentDir = trustStoreFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // Export certificate using keytool
        String keytoolPath = javaHome + "/bin/keytool";
        
        // First, export the certificate to a file
        String certFile = trustStorePath + ".cert";
        ProcessBuilder exportPb = new ProcessBuilder(
            keytoolPath,
            "-exportcert",
            "-alias", sourceAlias,
            "-keystore", sourceKeystorePath,
            "-storepass", sourcePassword,
            "-file", certFile
        );
        
        exportPb.redirectErrorStream(true);
        Process exportProcess = exportPb.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(exportProcess.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("    [keytool export] " + line);
        }
        
        int exitCode = exportProcess.waitFor();
        if (exitCode != 0) {
            throw new Exception("Certificate export failed with exit code: " + exitCode);
        }
        
        // Then, import the certificate into the truststore
        ProcessBuilder importPb = new ProcessBuilder(
            keytoolPath,
            "-importcert",
            "-alias", sourceAlias,
            "-file", certFile,
            "-keystore", trustStorePath,
            "-storepass", trustStorePassword,
            "-noprompt",
            "-storetype", KEYSTORE_TYPE
        );
        
        importPb.redirectErrorStream(true);
        Process importProcess = importPb.start();
        
        reader = new BufferedReader(new InputStreamReader(importProcess.getInputStream()));
        while ((line = reader.readLine()) != null) {
            System.out.println("    [keytool import] " + line);
        }
        
        exitCode = importProcess.waitFor();
        if (exitCode != 0) {
            throw new Exception("Certificate import failed with exit code: " + exitCode);
        }
        
        // Clean up temporary certificate file
        new File(certFile).delete();
        
        System.out.println("[KeyStoreManager] ✓ Certificate exported to truststore successfully");
    }
    
    /**
     * Export certificate with default settings.
     * 
     * @param sourceKeystorePath Path to source keystore
     * @param trustStorePath Path where truststore will be created
     * @param javaHome Path to Java home directory
     * @throws Exception if export fails
     */
    public static void exportCertificateToTrustStore(
            String sourceKeystorePath,
            String trustStorePath,
            String javaHome) throws Exception {
        exportCertificateToTrustStore(
            sourceKeystorePath,
            DEFAULT_KEYSTORE_PASSWORD,
            DEFAULT_KEY_ALIAS,
            trustStorePath,
            DEFAULT_KEYSTORE_PASSWORD,
            javaHome
        );
    }
    
    /**
     * Get the default keystore password.
     * 
     * @return Default password
     */
    public static String getDefaultPassword() {
        return DEFAULT_KEYSTORE_PASSWORD;
    }
    
    /**
     * Get the default key alias.
     * 
     * @return Default alias
     */
    public static String getDefaultAlias() {
        return DEFAULT_KEY_ALIAS;
    }
    
    /**
     * Demo/test method to create and manage keystores.
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== KeyStore Manager Demo ===\n");
            
            // Determine Java home
            String javaHome = System.getProperty("java.home");
            System.out.println("Java Home: " + javaHome + "\n");
            
            // Create directories
            new File("certs").mkdirs();
            
            // Create keystore with certificate
            String keystorePath = "certs/test-keystore.jks";
            createKeyStoreWithCertificate(keystorePath, javaHome);
            
            // Load the keystore
            KeyStore keyStore = loadKeyStore(keystorePath);
            System.out.println("\nKeystore aliases:");
            var aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                System.out.println("  - " + alias);
                
                if (keyStore.isCertificateEntry(alias) || keyStore.isKeyEntry(alias)) {
                    Certificate cert = keyStore.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        X509Certificate x509 = (X509Certificate) cert;
                        System.out.println("    Subject: " + x509.getSubjectX500Principal());
                        System.out.println("    Algorithm: " + x509.getPublicKey().getAlgorithm());
                    }
                }
            }
            
            // Export to truststore
            String trustStorePath = "certs/test-truststore.jks";
            exportCertificateToTrustStore(keystorePath, trustStorePath, javaHome);
            
            // Load the truststore
            KeyStore trustStore = loadKeyStore(trustStorePath);
            System.out.println("\nTruststore aliases:");
            aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                System.out.println("  - " + aliases.nextElement());
            }
            
            System.out.println("\n✓ KeyStore management successful!");
            
        } catch (Exception e) {
            System.err.println("✗ KeyStore management failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

// Made with Bob
