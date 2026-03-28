import java.security.Security;
import java.security.Provider;

public class CheckSignatureAlgorithms {
    public static void main(String[] args) {
        System.out.println("=== Available Signature Algorithms ===\n");
        
        for (Provider provider : Security.getProviders()) {
            System.out.println("Provider: " + provider.getName() + " v" + provider.getVersionStr());
            
            for (Provider.Service service : provider.getServices()) {
                if (service.getType().equals("Signature")) {
                    String algo = service.getAlgorithm();
                    if (algo.contains("ML-DSA") || algo.contains("Dilithium") || 
                        algo.contains("MLDSA") || algo.toUpperCase().contains("DSA")) {
                        System.out.println("  - " + algo);
                    }
                }
            }
        }
        
        System.out.println("\n=== Checking ML-DSA Support ===");
        try {
            java.security.Signature sig = java.security.Signature.getInstance("ML-DSA-65");
            System.out.println("✓ ML-DSA-65 is available!");
            System.out.println("  Provider: " + sig.getProvider().getName());
        } catch (Exception e) {
            System.out.println("✗ ML-DSA-65 is NOT available: " + e.getMessage());
        }
    }
}

// Made with Bob
