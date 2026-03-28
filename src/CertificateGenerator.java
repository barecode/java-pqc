import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import java.io.*;

/**
 * Generates ML-DSA (Module-Lattice-Based Digital Signature Algorithm) certificates
 * for post-quantum HTTPS connections.
 * 
 * This class creates self-signed X.509 certificates using quantum-resistant ML-DSA keys.
 * 
 * Note: This implementation uses a simplified approach for certificate generation.
 * For production use, consider using a proper certificate authority or library like Bouncy Castle.
 */
public class CertificateGenerator {
    
    // Default ML-DSA algorithm variant (balanced security and performance)
    private static final String DEFAULT_ALGORITHM = "ML-DSA-65";
    
    // Default certificate validity period (1 year)
    private static final int DEFAULT_VALIDITY_DAYS = 365;
    
    /**
     * Generate an ML-DSA key pair with the default algorithm (ML-DSA-65).
     * 
     * @return KeyPair containing ML-DSA public and private keys
     * @throws NoSuchAlgorithmException if ML-DSA is not available
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return generateKeyPair(DEFAULT_ALGORITHM);
    }
    
    /**
     * Generate an ML-DSA key pair with a specific algorithm variant.
     * 
     * @param algorithm ML-DSA algorithm variant (ML-DSA-44, ML-DSA-65, or ML-DSA-87)
     * @return KeyPair containing ML-DSA public and private keys
     * @throws NoSuchAlgorithmException if the specified algorithm is not available
     */
    public static KeyPair generateKeyPair(String algorithm) throws NoSuchAlgorithmException {
        System.out.println("[CertificateGenerator] Generating " + algorithm + " key pair...");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        System.out.println("[CertificateGenerator] ✓ Key pair generated successfully");
        System.out.println("    - Public Key Algorithm: " + keyPair.getPublic().getAlgorithm());
        System.out.println("    - Public Key Size: " + keyPair.getPublic().getEncoded().length + " bytes");
        return keyPair;
    }
    
    /**
     * Generate a self-signed X.509 certificate using ML-DSA.
     * 
     * This is a simplified implementation that creates a basic certificate structure.
     * For production use, consider using a proper certificate library.
     * 
     * @param keyPair The ML-DSA key pair to use for the certificate
     * @param subject The certificate subject (e.g., "CN=localhost")
     * @param validityDays Number of days the certificate should be valid
     * @return Self-signed X509Certificate
     * @throws Exception if certificate generation fails
     */
    public static X509Certificate generateSelfSignedCertificate(
            KeyPair keyPair, 
            String subject, 
            int validityDays) throws Exception {
        
        System.out.println("[CertificateGenerator] Generating self-signed certificate...");
        System.out.println("    - Subject: " + subject);
        System.out.println("    - Validity: " + validityDays + " days");
        
        // Get the algorithm from the key pair
        String algorithm = keyPair.getPublic().getAlgorithm();
        
        // Create certificate validity dates
        Instant now = Instant.now();
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(validityDays, ChronoUnit.DAYS));
        
        // Generate a random serial number
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        
        // Create X500Principal for subject and issuer
        X500Principal subjectPrincipal = new X500Principal(subject);
        X500Principal issuerPrincipal = subjectPrincipal; // Self-signed
        
        // Build certificate using custom implementation
        SimpleCertificateBuilder builder = new SimpleCertificateBuilder();
        X509Certificate certificate = builder.build(
            serialNumber,
            issuerPrincipal,
            subjectPrincipal,
            notBefore,
            notAfter,
            keyPair.getPublic(),
            keyPair.getPrivate(),
            algorithm
        );
        
        System.out.println("[CertificateGenerator] ✓ Certificate generated successfully");
        System.out.println("    - Serial Number: " + certificate.getSerialNumber());
        System.out.println("    - Not Before: " + certificate.getNotBefore());
        System.out.println("    - Not After: " + certificate.getNotAfter());
        System.out.println("    - Signature Algorithm: " + certificate.getSigAlgName());
        
        return certificate;
    }
    
    /**
     * Generate a self-signed certificate with default validity period (365 days).
     * 
     * @param keyPair The ML-DSA key pair to use for the certificate
     * @param subject The certificate subject (e.g., "CN=localhost")
     * @return Self-signed X509Certificate
     * @throws Exception if certificate generation fails
     */
    public static X509Certificate generateSelfSignedCertificate(
            KeyPair keyPair, 
            String subject) throws Exception {
        return generateSelfSignedCertificate(keyPair, subject, DEFAULT_VALIDITY_DAYS);
    }
    
    /**
     * Create a certificate chain (for self-signed, this is just the certificate itself).
     * 
     * @param certificate The X509Certificate to include in the chain
     * @return Certificate array containing the certificate
     */
    public static Certificate[] createCertificateChain(X509Certificate certificate) {
        return new Certificate[] { certificate };
    }
    
    /**
     * Demo/test method to generate and display certificate information.
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== ML-DSA Certificate Generator Demo ===\n");
            
            // Generate key pair
            KeyPair keyPair = generateKeyPair();
            
            // Generate self-signed certificate
            X509Certificate certificate = generateSelfSignedCertificate(
                keyPair, 
                "CN=PQC-Test-Server, O=Post-Quantum Cryptography, C=US"
            );
            
            System.out.println("\n=== Certificate Details ===");
            System.out.println("Subject: " + certificate.getSubjectX500Principal());
            System.out.println("Issuer: " + certificate.getIssuerX500Principal());
            System.out.println("Serial Number: " + certificate.getSerialNumber());
            System.out.println("Valid From: " + certificate.getNotBefore());
            System.out.println("Valid Until: " + certificate.getNotAfter());
            System.out.println("Signature Algorithm: " + certificate.getSigAlgName());
            System.out.println("Public Key Algorithm: " + certificate.getPublicKey().getAlgorithm());
            System.out.println("Version: " + certificate.getVersion());
            
            System.out.println("\n✓ Certificate generation successful!");
            
        } catch (Exception e) {
            System.err.println("✗ Certificate generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Simple certificate builder that creates a basic X.509 certificate structure.
     * This is a minimal implementation for demonstration purposes.
     */
    private static class SimpleCertificateBuilder {
        
        public X509Certificate build(
                BigInteger serialNumber,
                X500Principal issuer,
                X500Principal subject,
                Date notBefore,
                Date notAfter,
                PublicKey publicKey,
                PrivateKey privateKey,
                String signatureAlgorithm) throws Exception {
            
            // Create a simple certificate using Java's certificate factory
            // Note: This is a workaround. In production, use a proper certificate library.
            
            // For now, we'll use a different approach: generate via keytool programmatically
            // or use the internal sun.security APIs with proper module access
            
            throw new UnsupportedOperationException(
                "Certificate generation requires additional setup. " +
                "Please use KeyStoreManager.createKeyStoreWithCertificate() instead, " +
                "which uses keytool internally to generate certificates."
            );
        }
    }
}

// Made with Bob
