/**
 * Placeholder class for ML-DSA certificate generation.
 *
 * Note: Certificate generation is handled by KeyStoreManager using keytool.
 * This class is kept for potential future direct certificate generation functionality.
 */
public class CertificateGenerator {
    
    /**
     * Private constructor to prevent instantiation.
     * Use KeyStoreManager.createKeyStoreWithCertificate() for certificate generation.
     */
    private CertificateGenerator() {
        throw new UnsupportedOperationException(
            "Certificate generation is handled by KeyStoreManager. " +
            "Use KeyStoreManager.createKeyStoreWithCertificate() instead."
        );
    }
}

// Made with Bob
