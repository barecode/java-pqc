# SSL Handshake Failure Analysis

## Problem Summary

The SSL handshake between the PQC HTTPS client and server is failing with a `handshake_failure` alert.

## Root Cause

The issue occurs because:

1. **Server Certificate Uses ML-DSA-65**: The server's certificate is signed with the ML-DSA-65 post-quantum signature algorithm
2. **Client Doesn't Advertise ML-DSA Support**: During the TLS handshake, the client's `ClientHello` message includes a `signature_algorithms` extension that lists supported signature schemes. This list does NOT include ML-DSA algorithms:
   ```
   signature_algorithms: [ecdsa_secp256r1_sha256, ecdsa_secp384r1_sha384, 
                          rsa_pss_rsae_sha256, ed25519, ed448, ...]
   ```
3. **Server Cannot Prove Certificate**: The server receives the `ClientHello` and sees that the client doesn't support ML-DSA. Since the server's certificate is signed with ML-DSA-65, it cannot prove the certificate's authenticity using any algorithm the client supports
4. **Handshake Fails**: The server sends a `handshake_failure` alert and terminates the connection

## Technical Details

### What We Discovered

1. **ML-DSA Algorithms ARE Available in JDK 25**:
   ```
   ✓ ML-DSA-44, ML-DSA-65, ML-DSA-87 are available
   ✓ Provider: SUN v25
   ✓ Can be used for signing/verification operations
   ```

2. **ML-DSA is NOT Integrated into TLS Handshake**:
   - The TLS/SSL implementation in JDK 25 does not include ML-DSA in the list of supported signature schemes for handshakes
   - System properties like `jdk.tls.client.SignatureSchemes` do not accept ML-DSA values
   - The TLS 1.3 specification extensions for post-quantum algorithms are not yet implemented

### Debug Output Evidence

From the SSL debug logs:

```
ClientHello:
  "signature_algorithms (13)": {
    "signature schemes": [ecdsa_secp256r1_sha256, ecdsa_secp384r1_sha384, 
                          ecdsa_secp521r1_sha512, ed25519, ed448, 
                          rsa_pss_rsae_sha256, ...]
    // NO ML-DSA algorithms listed!
  }

ServerHello:
  "cipher suite": "TLS_AES_256_GCM_SHA384"
  // Server selected a cipher suite but...

Alert:
  "level": "fatal",
  "description": "handshake_failure"
  // Server cannot prove its ML-DSA certificate
```

## Why This Happens

The TLS handshake protocol requires:
1. Client advertises which signature algorithms it supports
2. Server must prove its certificate using one of those algorithms
3. If the server's certificate uses an algorithm the client doesn't support, the handshake fails

This is a **protocol-level limitation**, not a bug. The TLS 1.3 specification needs to be extended to support post-quantum signature algorithms in the handshake, and JDK implementations need to adopt these extensions.

## Current State of PQC in TLS

As of JDK 25 (Early Access):
- ✓ Post-quantum signature algorithms (ML-DSA) are available for general use
- ✓ Post-quantum key encapsulation (ML-KEM) may be available
- ✗ TLS handshake protocol does NOT support PQC signature algorithms
- ✗ Cannot use ML-DSA certificates in standard HTTPS connections

## Solutions

### Option 1: Use Hybrid Certificates (Recommended for Production)
Create certificates that use traditional signature algorithms (RSA, ECDSA) for the TLS handshake, but include ML-DSA public keys in certificate extensions for application-level verification.

### Option 2: Use Traditional Certificates for TLS
Use RSA or ECDSA certificates for the HTTPS connection, then demonstrate ML-DSA capabilities at the application level (signing/verifying data after the connection is established).

### Option 3: Wait for Full TLS 1.3 PQC Support
Wait for:
- IETF to finalize TLS 1.3 extensions for post-quantum algorithms
- JDK to implement these extensions
- Browsers and clients to adopt the new standards

### Option 4: Custom TLS Implementation
Implement a custom TLS handshake that supports ML-DSA (complex, not recommended for production).

## Recommended Fix

For this demonstration project, we should:

1. **Modify the certificate generation** to use ECDSA or RSA for the certificate signature (so TLS handshake works)
2. **Add ML-DSA demonstration** at the application level:
   - After successful HTTPS connection
   - Server signs a challenge with ML-DSA
   - Client verifies the ML-DSA signature
   - This proves PQC capabilities without breaking TLS

This approach:
- ✓ Allows HTTPS connection to succeed
- ✓ Demonstrates ML-DSA signature capabilities
- ✓ Shows how PQC can be integrated into existing systems
- ✓ Provides a migration path for when TLS 1.3 PQC support arrives

## Implementation Plan

1. Update `KeyStoreManager` to generate certificates with ECDSA signatures
2. Keep ML-DSA key pair generation for application-level signing
3. Add endpoint that demonstrates ML-DSA signing/verification
4. Document the hybrid approach

This will create a working demonstration that shows both:
- Traditional TLS security (ECDSA certificates)
- Post-quantum cryptography capabilities (ML-DSA signing)