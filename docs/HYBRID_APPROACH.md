# Hybrid Post-Quantum Cryptography Approach

## Overview

This project demonstrates a **hybrid approach** to post-quantum cryptography (PQC) that combines:
- **Traditional ECDSA** for TLS/SSL handshakes (ensuring compatibility)
- **ML-DSA-65** (post-quantum) for application-level signing (ensuring quantum resistance)

This approach provides both **immediate compatibility** with existing infrastructure and **future-proof security** against quantum computers.

## Why Hybrid?

### The Problem with Pure PQC

When we initially tried to use ML-DSA-65 certificates for TLS:
- ✗ SSL handshake failed with `handshake_failure` alert
- ✗ Client doesn't advertise ML-DSA support in TLS handshake
- ✗ ML-DSA not integrated into TLS 1.3 protocol (as of JDK 25)

**Root Cause**: While ML-DSA algorithms are available in JDK 25 for signing/verification, they are **not yet integrated into the TLS handshake protocol**. The TLS implementation doesn't include ML-DSA in the list of supported signature schemes for certificate verification.

### The Hybrid Solution

Instead of waiting for TLS 1.3 PQC extensions:
1. ✓ Use **ECDSA certificates** for TLS handshake (works with all clients)
2. ✓ Use **ML-DSA-65** for application-level signing (quantum-resistant)
3. ✓ Demonstrate both traditional and post-quantum security

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTPS Connection                          │
│                                                              │
│  ┌──────────────┐                    ┌──────────────┐      │
│  │   Client     │◄──── TLS 1.3 ────►│   Server     │      │
│  │              │   (ECDSA Cert)     │              │      │
│  └──────────────┘                    └──────────────┘      │
│         │                                     │              │
│         │                                     │              │
│         ▼                                     ▼              │
│  ┌──────────────┐                    ┌──────────────┐      │
│  │ Application  │◄── ML-DSA-65 ────►│ Application  │      │
│  │   Layer      │    Signatures      │   Layer      │      │
│  └──────────────┘                    └──────────────┘      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Layer 1: TLS Transport (ECDSA)
- **Purpose**: Establish secure HTTPS connection
- **Algorithm**: ECDSA (secp384r1) with SHA384
- **Certificate**: Self-signed ECDSA certificate
- **Compatibility**: Works with all modern browsers and clients

### Layer 2: Application Security (ML-DSA-65)
- **Purpose**: Demonstrate post-quantum cryptography
- **Algorithm**: ML-DSA-65 (NIST FIPS 204)
- **Key Size**: 
  - Public key: 1,974 bytes
  - Private key: 4,060 bytes
  - Signature: 3,309 bytes
- **Security**: Resistant to quantum computer attacks

## Implementation Details

### Server Components

#### 1. Certificate Generation (ECDSA)
```java
// KeyStoreManager.java - Line 97
"EC"  // Use ECDSA for TLS compatibility
```

The server generates an ECDSA certificate using `keytool`:
- Algorithm: EC (Elliptic Curve)
- Curve: secp384r1 (384-bit)
- Signature: SHA384withECDSA
- Validity: 365 days

#### 2. ML-DSA Key Pair Generation
```java
// KeyStoreManager.java - generateMLDSAKeyPair()
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ML-DSA-65");
KeyPair keyPair = keyGen.generateKeyPair();
```

The server generates a separate ML-DSA-65 key pair:
- Stored in: `certs/mldsa-public.key` and `certs/mldsa-private.key`
- Used for: Application-level signing operations
- Generated once on first startup

#### 3. Server Endpoints

**`/ssl-info`** - Get TLS session information
- Shows ECDSA certificate details
- Displays TLS 1.3 cipher suite
- Returns JSON with session metadata

**`/pqc-sign`** - Sign a message with ML-DSA-65
- Method: POST
- Input: Plain text message
- Output: JSON with signature and public key
- Example:
  ```json
  {
    "message": "Hello, Post-Quantum World!",
    "signature": "bS8c+w2oq5tzvOwV2t8Q...",
    "algorithm": "ML-DSA-65",
    "signatureSize": 3309,
    "publicKey": "MIIHsjALBglghkgBZQME..."
  }
  ```

**`/pqc-verify`** - Verify an ML-DSA-65 signature
- Method: POST
- Input: JSON with message and signature
- Output: JSON with verification result
- Example:
  ```json
  {
    "message": "Hello, Post-Quantum World!",
    "valid": true,
    "algorithm": "ML-DSA-65"
  }
  ```

### Client Components

#### 1. TLS Connection (ECDSA)
The client establishes a standard HTTPS connection:
- Loads truststore with server's ECDSA certificate
- Performs TLS 1.3 handshake
- Verifies server certificate using ECDSA

#### 2. ML-DSA Operations
After successful TLS connection, the client:
1. Sends a message to `/pqc-sign` endpoint
2. Receives ML-DSA-65 signature
3. Verifies signature using `/pqc-verify` endpoint
4. Tests with tampered message to demonstrate security

## Security Analysis

### TLS Layer (ECDSA)
- **Current Security**: Strong against classical computers
- **Quantum Threat**: Vulnerable to Shor's algorithm
- **Timeline**: Safe for next 10-15 years (estimated)
- **Purpose**: Provides immediate compatibility

### Application Layer (ML-DSA-65)
- **Current Security**: Strong against classical computers
- **Quantum Threat**: Resistant to known quantum algorithms
- **Timeline**: Safe for foreseeable future
- **Purpose**: Provides long-term quantum resistance

### Combined Security
The hybrid approach provides:
- ✓ **Immediate deployment** with existing infrastructure
- ✓ **Quantum-resistant** application-level security
- ✓ **Migration path** for when TLS 1.3 PQC support arrives
- ✓ **Demonstrable** post-quantum capabilities

## Performance Characteristics

### ECDSA (secp384r1)
- Key generation: ~10ms
- Signing: ~1ms
- Verification: ~2ms
- Signature size: 96-104 bytes

### ML-DSA-65
- Key generation: ~50ms
- Signing: ~5ms
- Verification: ~3ms
- Signature size: 3,309 bytes

### Trade-offs
- **Larger signatures**: ML-DSA signatures are ~30x larger than ECDSA
- **Slightly slower**: ML-DSA operations are ~3-5x slower
- **Quantum-resistant**: Worth the overhead for long-term security

## Migration Path

### Phase 1: Current (Hybrid Approach)
- TLS: ECDSA
- Application: ML-DSA-65
- Status: ✓ Implemented

### Phase 2: TLS 1.3 PQC Extensions
- TLS: Hybrid ECDSA + ML-DSA
- Application: ML-DSA-65
- Status: ⏳ Waiting for standards

### Phase 3: Full PQC
- TLS: ML-DSA or ML-KEM
- Application: ML-DSA-65
- Status: ⏳ Future

## Testing the Implementation

### 1. Start the Server
```bash
./start-server.sh
```

Expected output:
- ECDSA keystore created
- ML-DSA keys generated
- Server listening on port 8443

### 2. Run the Client
```bash
./start-client.sh
```

Expected output:
- ✓ TLS handshake successful (ECDSA)
- ✓ ML-DSA signature generated
- ✓ ML-DSA signature verified (valid: true)
- ✓ Tampered message rejected (valid: false)

### 3. Manual Testing
```bash
# Test ML-DSA signing
curl -k -X POST https://localhost:8443/pqc-sign \
  -d "Hello, Post-Quantum World!"

# Test ML-DSA verification
curl -k -X POST https://localhost:8443/pqc-verify \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello","signature":"..."}'
```

## Key Files

### Certificates (ECDSA)
- `certs/server-keystore.p12` - Server ECDSA certificate and private key
- `certs/client-truststore.p12` - Client truststore with server's ECDSA certificate

### ML-DSA Keys
- `certs/mldsa-public.key` - ML-DSA-65 public key (1,974 bytes)
- `certs/mldsa-private.key` - ML-DSA-65 private key (4,060 bytes)

### Source Code
- `src/PQCHttpsServer.java` - HTTPS server with ML-DSA endpoints
- `src/PQCHttpsClient.java` - HTTPS client with ML-DSA testing
- `src/KeyStoreManager.java` - Certificate and key management
- `src/MLDSASignatureHelper.java` - ML-DSA signing/verification utilities

## Advantages of This Approach

1. **Immediate Deployment**
   - Works with existing TLS infrastructure
   - No need to wait for TLS 1.3 PQC standards
   - Compatible with all modern clients

2. **Quantum Resistance**
   - Application data protected by ML-DSA-65
   - Future-proof against quantum computers
   - Demonstrates PQC capabilities

3. **Flexibility**
   - Can upgrade TLS layer when standards arrive
   - Application layer already quantum-resistant
   - Easy to add more PQC algorithms

4. **Educational Value**
   - Shows both traditional and PQC approaches
   - Demonstrates real-world integration
   - Provides working code examples

## Limitations

1. **TLS Layer Not Quantum-Resistant**
   - ECDSA vulnerable to quantum computers
   - Acceptable for near-term deployment
   - Will need upgrade when quantum computers arrive

2. **Larger Signatures**
   - ML-DSA signatures are significantly larger
   - May impact bandwidth-constrained applications
   - Trade-off for quantum resistance

3. **Not a Standard**
   - Custom application-level protocol
   - Not interoperable with other implementations
   - Demonstrates concept, not production-ready

## Conclusion

This hybrid approach provides a practical solution for integrating post-quantum cryptography today:
- ✓ Works with existing infrastructure
- ✓ Provides quantum-resistant application security
- ✓ Demonstrates real-world PQC implementation
- ✓ Offers clear migration path to full PQC

The implementation shows that post-quantum cryptography is ready for application-level use, even while we wait for TLS protocol updates.