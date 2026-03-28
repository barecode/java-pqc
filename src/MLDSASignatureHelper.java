import java.security.*;
import java.util.Base64;

/**
 * Helper class for ML-DSA signature operations.
 * Provides methods for signing data and verifying signatures using ML-DSA algorithms.
 */
public class MLDSASignatureHelper {
    
    private static final String DEFAULT_ALGORITHM = "ML-DSA-65";
    
    /**
     * Sign data using an ML-DSA private key.
     * 
     * @param data Data to sign
     * @param privateKey ML-DSA private key
     * @param algorithm ML-DSA algorithm variant
     * @return Signature bytes
     * @throws Exception if signing fails
     */
    public static byte[] sign(byte[] data, PrivateKey privateKey, String algorithm) throws Exception {
        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }
    
    /**
     * Sign data using an ML-DSA private key with default algorithm (ML-DSA-65).
     * 
     * @param data Data to sign
     * @param privateKey ML-DSA private key
     * @return Signature bytes
     * @throws Exception if signing fails
     */
    public static byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
        return sign(data, privateKey, DEFAULT_ALGORITHM);
    }
    
    /**
     * Sign a string using an ML-DSA private key.
     * 
     * @param message Message to sign
     * @param privateKey ML-DSA private key
     * @param algorithm ML-DSA algorithm variant
     * @return Base64-encoded signature
     * @throws Exception if signing fails
     */
    public static String signString(String message, PrivateKey privateKey, String algorithm) throws Exception {
        byte[] signatureBytes = sign(message.getBytes("UTF-8"), privateKey, algorithm);
        return Base64.getEncoder().encodeToString(signatureBytes);
    }
    
    /**
     * Sign a string using an ML-DSA private key with default algorithm.
     * 
     * @param message Message to sign
     * @param privateKey ML-DSA private key
     * @return Base64-encoded signature
     * @throws Exception if signing fails
     */
    public static String signString(String message, PrivateKey privateKey) throws Exception {
        return signString(message, privateKey, DEFAULT_ALGORITHM);
    }
    
    /**
     * Verify a signature using an ML-DSA public key.
     * 
     * @param data Original data
     * @param signatureBytes Signature to verify
     * @param publicKey ML-DSA public key
     * @param algorithm ML-DSA algorithm variant
     * @return true if signature is valid, false otherwise
     * @throws Exception if verification fails
     */
    public static boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey, String algorithm) throws Exception {
        Signature signature = Signature.getInstance(algorithm);
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }
    
    /**
     * Verify a signature using an ML-DSA public key with default algorithm.
     * 
     * @param data Original data
     * @param signatureBytes Signature to verify
     * @param publicKey ML-DSA public key
     * @return true if signature is valid, false otherwise
     * @throws Exception if verification fails
     */
    public static boolean verify(byte[] data, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        return verify(data, signatureBytes, publicKey, DEFAULT_ALGORITHM);
    }
    
    /**
     * Verify a Base64-encoded signature for a string message.
     * 
     * @param message Original message
     * @param signatureBase64 Base64-encoded signature
     * @param publicKey ML-DSA public key
     * @param algorithm ML-DSA algorithm variant
     * @return true if signature is valid, false otherwise
     * @throws Exception if verification fails
     */
    public static boolean verifyString(String message, String signatureBase64, PublicKey publicKey, String algorithm) throws Exception {
        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
        return verify(message.getBytes("UTF-8"), signatureBytes, publicKey, algorithm);
    }
    
    /**
     * Verify a Base64-encoded signature for a string message with default algorithm.
     * 
     * @param message Original message
     * @param signatureBase64 Base64-encoded signature
     * @param publicKey ML-DSA public key
     * @return true if signature is valid, false otherwise
     * @throws Exception if verification fails
     */
    public static boolean verifyString(String message, String signatureBase64, PublicKey publicKey) throws Exception {
        return verifyString(message, signatureBase64, publicKey, DEFAULT_ALGORITHM);
    }
    
    /**
     * Get signature size for an ML-DSA algorithm.
     * 
     * @param algorithm ML-DSA algorithm variant
     * @return Approximate signature size in bytes
     */
    public static int getSignatureSize(String algorithm) {
        switch (algorithm) {
            case "ML-DSA-44":
                return 2420;  // Approximate size for ML-DSA-44
            case "ML-DSA-65":
                return 3309;  // Approximate size for ML-DSA-65
            case "ML-DSA-87":
                return 4627;  // Approximate size for ML-DSA-87
            default:
                return 3309;  // Default to ML-DSA-65 size
        }
    }
    
    /**
     * Demo method showing ML-DSA signature operations.
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== ML-DSA Signature Demo ===\n");
            
            // Generate key pair
            System.out.println("1. Generating ML-DSA-65 key pair...");
            KeyPair keyPair = KeyStoreManager.generateMLDSAKeyPair();
            System.out.println();
            
            // Sign a message
            String message = "Hello, Post-Quantum World!";
            System.out.println("2. Signing message: \"" + message + "\"");
            String signature = signString(message, keyPair.getPrivate());
            System.out.println("   Signature (Base64): " + signature.substring(0, Math.min(64, signature.length())) + "...");
            System.out.println("   Signature size: " + Base64.getDecoder().decode(signature).length + " bytes");
            System.out.println();
            
            // Verify signature
            System.out.println("3. Verifying signature...");
            boolean isValid = verifyString(message, signature, keyPair.getPublic());
            System.out.println("   Signature valid: " + isValid);
            System.out.println();
            
            // Try with tampered message
            System.out.println("4. Verifying with tampered message...");
            String tamperedMessage = "Hello, Post-Quantum World?";
            boolean isTamperedValid = verifyString(tamperedMessage, signature, keyPair.getPublic());
            System.out.println("   Tampered signature valid: " + isTamperedValid);
            System.out.println();
            
            System.out.println("✓ ML-DSA signature demo complete!");
            
        } catch (Exception e) {
            System.err.println("✗ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

// Made with Bob
