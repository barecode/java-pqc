/**
 * Source: https://codefarm0.medium.com/post-quantum-cryptography-in-java-with-code-examples-74326adb0d3c 
 * Min requirements: JDK 24 - https://openjdk.org/projects/jdk/24/
 * Related reading: 
 * - https://openjdk.org/jeps/497 "JEP 497: Quantum-Resistant Module-Lattice-Based Digital Signature Algorithm"
 * - https://openjdk.org/jeps/496 "JEP 496: Quantum-Resistant Module-Lattice-Based Key Encapsulation Mechanism"
 */

import java.security.*;
public class DilithiumSignatureDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Post-Quantum Cryptography Demo ===");
        System.out.println("Algorithm: ML-DSA (Module-Lattice-Based Digital Signature)\n");
        
        // Key Generation
        System.out.println("[1] Generating quantum-resistant key pair...");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA");
        KeyPair kp = kpg.generateKeyPair();
        System.out.println("    ✓ Key pair generated successfully");
        System.out.println("    - Public Key Algorithm: " + kp.getPublic().getAlgorithm());
        System.out.println("    - Private Key Algorithm: " + kp.getPrivate().getAlgorithm());
        
        // Message Setup
        String message = "Post-Quantum Java is here!";
        byte[] messageBytes = message.getBytes();
        System.out.println("\n[2] Message to sign:");
        System.out.println("    \"" + message + "\"");
        
        // Signing
        System.out.println("\n[3] Signing message with private key...");
        Signature sig = Signature.getInstance("ML-DSA");
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