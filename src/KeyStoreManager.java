import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.nio.file.*;

/**
 * Manages keystores and truststores for ML-DSA certificates.
 * 
 * This class handles creation, loading, and management of industry standard KeyStores (PKCS12)
 * containing ML-DSA certificates for HTTPS connections.
 */
public class KeyStoreManager {
    
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "changeit";
    private static final String DEFAULT_KEY_ALIAS = "pqc-server";
    private static final String KEYTOOL_OUTPUT_PREFIX = "    [keytool] ";
    
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
        
        // Validate input parameters
        validateParameter(keystorePath, "keystorePath");
        validateParameter(alias, "alias");
        validateParameter(password, "password");
        validateParameter(dname, "dname");
        validateParameter(algorithm, "algorithm");
        validateParameter(javaHome, "javaHome");
        if (validityDays <= 0) {
            throw new IllegalArgumentException("validityDays must be positive, got: " + validityDays);
        }
        
        System.out.println("[KeyStoreManager] Creating keystore with ML-DSA certificate...");
        System.out.println("    - Keystore: " + keystorePath);
        System.out.println("    - Alias: " + alias);
        System.out.println("    - Algorithm: " + algorithm);
        System.out.println("    - DN: " + dname);
        System.out.println("    - Validity: " + validityDays + " days");
        
        // Delete existing keystore if it exists (using NIO)
        Path keystoreFilePath = Paths.get(keystorePath);
        if (Files.deleteIfExists(keystoreFilePath)) {
            System.out.println("    - Deleted existing keystore");
        }
        
        // Ensure parent directory exists (using NIO)
        Path parentDir = keystoreFilePath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        
        // Build and execute keytool command
        Path keytoolPath = Paths.get(javaHome).resolve("bin").resolve("keytool");
        ProcessBuilder pb = buildKeytoolGenKeyPairCommand(
            keytoolPath.toString(), alias, algorithm, keystorePath,
            password, dname, validityDays
        );
        
        executeKeytoolCommand(pb, "Keystore creation");
        
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
        
        // Validate input parameters
        validateParameter(sourceKeystorePath, "sourceKeystorePath");
        validateParameter(sourcePassword, "sourcePassword");
        validateParameter(sourceAlias, "sourceAlias");
        validateParameter(trustStorePath, "trustStorePath");
        validateParameter(trustStorePassword, "trustStorePassword");
        validateParameter(javaHome, "javaHome");
        
        System.out.println("[KeyStoreManager] Exporting certificate to truststore...");
        System.out.println("    - Source: " + sourceKeystorePath);
        System.out.println("    - Destination: " + trustStorePath);
        System.out.println("    - Alias: " + sourceAlias);
        
        // Delete existing truststore if it exists (using NIO)
        Path trustStoreFilePath = Paths.get(trustStorePath);
        if (Files.deleteIfExists(trustStoreFilePath)) {
            System.out.println("    - Deleted existing truststore");
        }
        
        // Ensure parent directory exists (using NIO)
        Path parentDir = trustStoreFilePath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        
        // Export certificate using keytool
        Path keytoolPath = Paths.get(javaHome).resolve("bin").resolve("keytool");
        String keytoolCommand = keytoolPath.toString();
        
        // First, export the certificate to a file
        Path certFilePath = Paths.get(trustStorePath + ".cert");
        ProcessBuilder exportPb = new ProcessBuilder(
            keytoolCommand,
            "-exportcert",
            "-alias", sourceAlias,
            "-keystore", sourceKeystorePath,
            "-storepass", sourcePassword,
            "-file", certFilePath.toString()
        );
        
        executeKeytoolCommand(exportPb, "Certificate export");
        
        // Then, import the certificate into the truststore
        ProcessBuilder importPb = new ProcessBuilder(
            keytoolCommand,
            "-importcert",
            "-alias", sourceAlias,
            "-file", certFilePath.toString(),
            "-keystore", trustStorePath,
            "-storepass", trustStorePassword,
            "-noprompt",
            "-storetype", KEYSTORE_TYPE
        );
        
        executeKeytoolCommand(importPb, "Certificate import");
        
        // Clean up temporary certificate file (using NIO)
        Files.deleteIfExists(certFilePath);
        
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
     * Validates that a parameter is not null or empty.
     *
     * @param value Parameter value to validate
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if validation fails
     */
    private static void validateParameter(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
    }
    
    /**
     * Builds a ProcessBuilder for keytool genkeypair command.
     *
     * @param keytoolPath Path to keytool executable
     * @param alias Key alias
     * @param algorithm Signature algorithm
     * @param keystorePath Path to keystore
     * @param password Keystore password
     * @param dname Distinguished name
     * @param validityDays Certificate validity in days
     * @return Configured ProcessBuilder
     */
    private static ProcessBuilder buildKeytoolGenKeyPairCommand(
            String keytoolPath, String alias, String algorithm,
            String keystorePath, String password, String dname, int validityDays) {
        return new ProcessBuilder(
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
    }
    
    /**
     * Executes a keytool command and captures output.
     *
     * @param pb ProcessBuilder configured with keytool command
     * @param operationName Name of operation for error messages
     * @throws Exception if command execution fails
     */
    private static void executeKeytoolCommand(ProcessBuilder pb, String operationName) throws Exception {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Capture output with proper resource management
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(KEYTOOL_OUTPUT_PREFIX + line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception(String.format(
                "%s failed with exit code: %d", operationName, exitCode));
        }
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
            String keystorePath = "certs/test-keystore.p12";
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
            String trustStorePath = "certs/test-truststore.p12";
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
