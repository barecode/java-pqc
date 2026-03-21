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
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA");
        KeyPair kp = kpg.generateKeyPair();
        Signature sig = Signature.getInstance("ML-DSA");
        String message = "Post-Quantum Java is here!";
        byte[] messageBytes = message.getBytes();
        // Signing
        sig.initSign(kp.getPrivate());
        sig.update(messageBytes);
        byte[] signature = sig.sign();
        // Verification
        sig.initVerify(kp.getPublic());
        sig.update(messageBytes);
        boolean isVerified = sig.verify(signature);
        System.out.println("Signature Verified: " + isVerified);
    }
}