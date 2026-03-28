/**
 * Source: https://codefarm0.medium.com/post-quantum-cryptography-in-java-with-code-examples-74326adb0d3c 
 * Min requirements: JDK 24 - https://openjdk.org/projects/jdk/24/
 * Related reading: 
 * - https://openjdk.org/jeps/497 "JEP 497: Quantum-Resistant Module-Lattice-Based Digital Signature Algorithm"
     - In order of increasing security strength and decreasing performance, they are named "ML-DSA-44", "ML-DSA-65" (DEFAULT), and "ML-DSA-87".
 * - https://openjdk.org/jeps/496 "JEP 496: Quantum-Resistant Module-Lattice-Based Key Encapsulation Mechanism"
 * - https://openjdk.org/jeps/470 "JEP 470: PEM Encodings of Cryptographic Objects (Preview)""
 * - https://openjdk.org/jeps/524 "JEP 524: PEM Encodings of Cryptographic Objects (Second Preview)"
 *
 * Related reading: https://openquantumsafe.org/
 */

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class DilithiumSignatureDemo {
    // Default algorithm: ML-DSA-65 (balanced security and performance)
    private static final String DEFAULT_ALGORITHM = "ML-DSA";
    
    public static void main(String[] args) throws Exception {
        // Parse command line argument for algorithm, or use default
        String algorithm = DEFAULT_ALGORITHM;
        if (args.length > 0) {
            algorithm = args[0];
            System.out.println("Using algorithm from command line: " + algorithm);
        } else {
            System.out.println("Using default algorithm: " + algorithm);
            System.out.println("(You can specify a different algorithm as a command line argument)");
            System.out.println("Available options: ML-DSA, ML-DSA-44, ML-DSA-65, ML-DSA-87\n");
        }
        
        System.out.println("=== Post-Quantum Cryptography Demo ===");
        System.out.println("Algorithm: " + algorithm + " (Module-Lattice-Based Digital Signature)\n");
        
        // Key Generation
        System.out.println("[1] Generating quantum-resistant key pair...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        KeyPair kp = kpg.generateKeyPair();
        System.out.println("    ✓ Key pair generated successfully");
        System.out.println("    - Public Key Algorithm: " + kp.getPublic().getAlgorithm());
        System.out.println("    - Private Key Algorithm: " + kp.getPrivate().getAlgorithm());
        
        // Export public key in X.509 format
        byte[] publicKeyEncoded = kp.getPublic().getEncoded();
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyEncoded);
        System.out.println("\n    Public Key (X.509 encoded):");
        System.out.println("    - Encoded length: " + x509KeySpec.getEncoded().length + " bytes");
        System.out.println("    - Format: " + kp.getPublic().getFormat());
        System.out.println("    - Base64 encoded:");
        System.out.println("-----BEGIN PUBLIC KEY-----");
        System.out.println(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(x509KeySpec.getEncoded()));
        System.out.println("-----END PUBLIC KEY-----");
        
        // Export private key in PKCS #8 format
        byte[] privateKeyEncoded = kp.getPrivate().getEncoded();
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privateKeyEncoded);
        System.out.println("\n    Private Key (PKCS #8 encoded):");
        System.out.println("    - Encoded length: " + pkcs8KeySpec.getEncoded().length + " bytes");
        System.out.println("    - Format: " + kp.getPrivate().getFormat());
        System.out.println("    - Base64 encoded:");
        System.out.println("-----BEGIN PRIVATE KEY-----");
        System.out.println(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pkcs8KeySpec.getEncoded()));
        System.out.println("-----END PRIVATE KEY-----");
        
        // Message Setup
        String message = "Post-Quantum Java is here!";
        byte[] messageBytes = message.getBytes();
        System.out.println("\n[2] Message to sign:");
        System.out.println("    \"" + message + "\"");
        
        // Signing
        System.out.println("\n[3] Signing message with private key...");
        Signature sig = Signature.getInstance(algorithm);
        sig.initSign(kp.getPrivate());
        sig.update(messageBytes);
        byte[] signature = sig.sign();
        System.out.println("    ✓ Signature generated");
        System.out.println("    - Signature length: " + signature.length + " bytes");
        System.out.println("    - Signature (hex): " + bytesToHex(signature));
        
        // Verification
        System.out.println("\n[4] Verifying signature with public key...");
        sig.initVerify(kp.getPublic());
        sig.update(messageBytes);
        boolean isVerified = sig.verify(signature);
        System.out.println("    ✓ Verification complete");
        System.out.println("\n[RESULT] Signature Verified: " + isVerified);
        
        if (isVerified) {
            System.out.println("\n✓ SUCCESS: The signature is valid and the message is authentic!");
        } else {
            System.out.println("\n✗ FAILURE: The signature verification failed!");
        }
    }
    
    // Helper method to convert bytes to hexadecimal string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, 64); i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        if (bytes.length > 64) {
            hexString.append("... (truncated, showing first 64 bytes)");
        }
        return hexString.toString();
    }
}