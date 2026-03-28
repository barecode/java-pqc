# Quick Start Guide (5 Minutes)

Get up and running with hybrid post-quantum cryptography in Java.

## What This Demonstrates

This project shows a **hybrid approach** to post-quantum cryptography:

```
┌─────────────────────────────────────────────────────────┐
│              Hybrid PQC Architecture                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Layer 1: TLS Transport (ECDSA)                         │
│  ┌────────────────────────────────────────────────┐    │
│  │  ✓ Works with all clients today               │    │
│  │  ✓ Standard HTTPS/TLS 1.3                      │    │
│  │  ⚠ Vulnerable to future quantum computers     │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
│  Layer 2: Application Security (ML-DSA-65)              │
│  ┌────────────────────────────────────────────────┐    │
│  │  ✓ Quantum-resistant signatures                │    │
│  │  ✓ NIST FIPS 204 standard                      │    │
│  │  ✓ 3,309 byte signatures                       │    │
│  └────────────────────────────────────────────────┘    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**Why Hybrid?** Pure ML-DSA certificates don't work with current TLS implementations. This approach provides both compatibility and quantum resistance.

## Prerequisites

- macOS (bundled JDK included)
- Bash shell
- 5 minutes of your time

## Three Ways to Run

### Option 1: Automated Test (Recommended)

Run everything with one command:

```bash
./test-connection.sh
```

**What happens:**
1. ✓ Compiles all code
2. ✓ Starts HTTPS server (ECDSA certificate)
3. ✓ Runs client test
4. ✓ Demonstrates ML-DSA signing
5. ✓ Verifies signatures
6. ✓ Stops server

**Expected output:**
```
✓ TLS connection successful with ECDSA certificate!
✓ ML-DSA signature received
  - Signature size: 3309 bytes
✓ Signature valid: true
✓ Tampered message rejected: false
```

### Option 2: Manual Testing

**Terminal 1 - Start Server:**
```bash
./compile.sh
./start-server.sh
```

**Terminal 2 - Run Client:**
```bash
./start-client.sh
```

### Option 3: Just the Signature Demo

Test ML-DSA signatures without HTTPS:

```bash
./compile.sh
./run.sh
```

## What You Just Saw

### 1. TLS Handshake (ECDSA)
- Client connects to server over HTTPS
- Server presents ECDSA certificate
- TLS 1.3 handshake succeeds
- **Compatible with all clients today**

### 2. ML-DSA Signing
- Client sends message to `/pqc-sign` endpoint
- Server signs with ML-DSA-65 private key
- Returns 3,309 byte quantum-resistant signature
- **Secure against quantum computers**

### 3. ML-DSA Verification
- Client sends message + signature to `/pqc-verify`
- Server verifies using ML-DSA-65 public key
- Returns validation result
- **Detects any tampering**

## Try It Yourself

### Sign a Custom Message

```bash
curl -k -X POST https://localhost:8443/pqc-sign \
  -d "Your message here"
```

### Verify a Signature

```bash
curl -k -X POST https://localhost:8443/pqc-verify \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Your message here",
    "signature": "base64-signature-here"
  }'
```

### Get TLS Session Info

```bash
curl -k https://localhost:8443/ssl-info
```

## Understanding the Output

### TLS Session Information
```json
{
  "certificate": {
    "algorithm": "EC",              // ECDSA for TLS
    "signatureAlgorithm": "SHA384withECDSA"
  }
}
```
This shows the **traditional** ECDSA certificate used for TLS.

### ML-DSA Signature
```json
{
  "algorithm": "ML-DSA-65",         // Post-quantum!
  "signatureSize": 3309,            // Larger than ECDSA
  "signature": "bS8c+w2o..."        // Quantum-resistant
}
```
This shows the **quantum-resistant** ML-DSA signature.

## Key Concepts

### Why Two Layers?

1. **TLS Layer (ECDSA)**
   - Needed because ML-DSA not in TLS protocol yet
   - Provides immediate compatibility
   - Will be upgraded when TLS 1.3 PQC arrives

2. **Application Layer (ML-DSA)**
   - Provides quantum resistance today
   - Protects your data long-term
   - Ready for quantum computers

### Security Model

- **Today**: Both layers secure against classical computers
- **Future**: TLS vulnerable to quantum, application layer still secure
- **Migration**: Upgrade TLS to PQC when standards arrive

## Next Steps

### Learn More
- **[HYBRID_APPROACH.md](HYBRID_APPROACH.md)** - Complete architecture explanation
- **[SSL_HANDSHAKE_ANALYSIS.md](SSL_HANDSHAKE_ANALYSIS.md)** - Why pure ML-DSA doesn't work
- **[README.md](README.md)** - Full project documentation

### Explore the Code
- **Server**: `src/PQCHttpsServer.java` - HTTPS server with ML-DSA endpoints
- **Client**: `src/PQCHttpsClient.java` - Client demonstrating hybrid approach
- **Crypto**: `src/MLDSASignatureHelper.java` - ML-DSA operations
- **Keys**: `src/KeyStoreManager.java` - Certificate and key management

### Customize
1. Change ML-DSA variant (44, 65, or 87)
2. Add your own endpoints
3. Integrate into your application

## Troubleshooting

### Port Already in Use
```bash
./start-server.sh 9443  # Use different port
```

### Certificates Not Found
```bash
rm -rf certs/           # Delete old certificates
./start-server.sh       # Regenerate
```

### Compilation Errors
```bash
./compile.sh            # Recompile everything
```

## What Makes This Special?

✅ **Works Today** - No waiting for TLS standards
✅ **Quantum-Resistant** - Application data protected
✅ **Real Implementation** - Not just theory
✅ **Easy to Test** - One command to see it work
✅ **Well Documented** - Understand how and why

## Questions?

- **Why hybrid?** ML-DSA not in TLS protocol yet
- **Is it secure?** Yes, against both classical and quantum
- **Can I use this?** Yes, for learning and prototyping
- **Production ready?** Demonstrates concept, needs hardening

## Summary

You just saw:
1. ✓ HTTPS connection with ECDSA (compatible)
2. ✓ ML-DSA signature generation (quantum-resistant)
3. ✓ Signature verification (tamper-proof)
4. ✓ Hybrid approach (best of both worlds)

**Time to quantum computers**: ~10-15 years (estimated)
**Time to implement PQC**: Today! (with this approach)

Ready to dive deeper? Check out [HYBRID_APPROACH.md](HYBRID_APPROACH.md)!