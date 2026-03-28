# Java Post-Quantum Cryptography (Hybrid Approach)

Demonstrates quantum-resistant cryptography using **ECDSA for TLS** and **ML-DSA-65 for application signatures**.

```
┌─────────────────────────────────────────────────────────┐
│              Hybrid PQC Architecture                     │
├─────────────────────────────────────────────────────────┤
│  Layer 1: TLS (ECDSA) - Compatible with all clients    │
│  Layer 2: Application (ML-DSA-65) - Quantum-resistant   │
└─────────────────────────────────────────────────────────┘
```

## Quick Start

```bash
./scripts/test-connection.sh
```

That's it! You just:
- ✓ Established TLS connection with ECDSA certificate
- ✓ Created quantum-resistant ML-DSA-65 signature (3,309 bytes)
- ✓ Verified the signature successfully
- ✓ Detected tampering in modified messages

## Why Hybrid?

Pure ML-DSA certificates don't work with current TLS implementations because ML-DSA is not yet integrated into the TLS handshake protocol. Our hybrid approach:

- ✓ **Works Today** - Uses ECDSA for TLS (compatible with all clients)
- ✓ **Quantum-Resistant** - Uses ML-DSA-65 for application security
- ✓ **Migration Path** - Ready for future TLS 1.3 PQC support

## Features

- 🔐 **Hybrid Architecture** - ECDSA (TLS) + ML-DSA-65 (Application)
- 🚀 **Immediate Deployment** - Works with existing infrastructure
- 🔮 **Quantum-Resistant** - Application data protected against quantum computers
- 📡 **REST API** - Sign and verify endpoints
- 📚 **Complete Documentation** - Architecture, analysis, and guides

## What's Included

### Server (`src/PQCHttpsServer.java`)
- HTTPS server with ECDSA certificate (TLS)
- ML-DSA-65 signing endpoint (`POST /pqc-sign`)
- ML-DSA-65 verification endpoint (`POST /pqc-verify`)
- TLS session info endpoint (`GET /ssl-info`)

### Client (`src/PQCHttpsClient.java`)
- Connects via HTTPS (ECDSA)
- Tests ML-DSA-65 signing
- Verifies signatures
- Demonstrates tamper detection

### Cryptography
- **TLS Layer**: ECDSA (secp384r1, SHA384)
- **Application Layer**: ML-DSA-65 (NIST FIPS 204)
- **Key Management**: Automatic generation and storage

## Usage

### Automated Test
```bash
./scripts/test-connection.sh
```

### Manual Testing

**Terminal 1 - Start Server:**
```bash
./scripts/compile.sh
./scripts/start-server.sh
```

**Terminal 2 - Run Client:**
```bash
./scripts/start-client.sh
```

### API Examples

**Sign a message:**
```bash
curl -k -X POST https://localhost:8443/pqc-sign \
  -d "Hello, Post-Quantum World!"
```

**Verify a signature:**
```bash
curl -k -X POST https://localhost:8443/pqc-verify \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello","signature":"..."}'
```

## Documentation

- **[QUICKSTART.md](QUICKSTART.md)** - Get running in 5 minutes
- **[docs/HYBRID_APPROACH.md](docs/HYBRID_APPROACH.md)** - Complete architecture explanation
- **[docs/SSL_HANDSHAKE_ANALYSIS.md](docs/SSL_HANDSHAKE_ANALYSIS.md)** - Why pure ML-DSA doesn't work
- **[docs/API_REFERENCE.md](docs/API_REFERENCE.md)** - API documentation
- **[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)** - Common issues and solutions

## Project Structure

```
java-pqc/
├── QUICKSTART.md              # 5-minute getting started guide
├── README.md                  # This file
├── src/                       # Source code
│   ├── PQCHttpsServer.java   # HTTPS server
│   ├── PQCHttpsClient.java   # HTTPS client
│   ├── KeyStoreManager.java  # Certificate management
│   └── MLDSASignatureHelper.java  # ML-DSA operations
├── scripts/                   # Build and run scripts
│   ├── compile.sh            # Compile all code
│   ├── start-server.sh       # Start server
│   ├── start-client.sh       # Run client
│   └── test-connection.sh    # End-to-end test
├── docs/                      # Documentation
│   ├── HYBRID_APPROACH.md    # Architecture details
│   ├── SSL_HANDSHAKE_ANALYSIS.md  # Technical analysis
│   ├── API_REFERENCE.md      # API documentation
│   └── TROUBLESHOOTING.md    # Common issues
├── certs/                     # Generated certificates (gitignored)
└── jdk-25.0.2+10/            # Bundled JDK with PQC support
```

## Security Model

### TLS Layer (ECDSA)
- **Algorithm**: ECDSA with secp384r1 curve
- **Security**: Strong against classical computers
- **Quantum Threat**: Vulnerable to Shor's algorithm (future)
- **Purpose**: Provides immediate compatibility

### Application Layer (ML-DSA-65)
- **Algorithm**: ML-DSA-65 (NIST FIPS 204)
- **Security**: Resistant to quantum attacks
- **Signature Size**: 3,309 bytes
- **Purpose**: Provides long-term quantum resistance

### Combined Security
- ✓ Works with existing infrastructure today
- ✓ Application data protected by quantum-resistant signatures
- ✓ Clear migration path when TLS 1.3 PQC standards arrive

## Requirements

- macOS (bundled JDK included)
- Bash shell
- No additional dependencies

## Algorithm Variants

ML-DSA supports three security levels:

| Algorithm | Security Level | Signature Size | Performance |
|-----------|---------------|----------------|-------------|
| ML-DSA-44 | ~128-bit | ~2,420 bytes | Fastest |
| ML-DSA-65 | ~192-bit | ~3,309 bytes | Balanced (Default) |
| ML-DSA-87 | ~256-bit | ~4,627 bytes | Most Secure |

## Troubleshooting

### Port Already in Use
```bash
./scripts/start-server.sh 9443  # Use different port
```

### Certificates Not Found
```bash
rm -rf certs/                    # Delete old certificates
./scripts/start-server.sh        # Regenerate
```

### Compilation Errors
```bash
./scripts/compile.sh             # Recompile everything
```

For more help, see [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)

## What Makes This Special?

✅ **Works Today** - No waiting for TLS standards
✅ **Quantum-Resistant** - Application data protected
✅ **Real Implementation** - Not just theory
✅ **Easy to Test** - One command to see it work
✅ **Well Documented** - Understand how and why
✅ **Production-Ready Approach** - Demonstrates practical PQC integration

## Technical Details

- **Source**: Based on [Post-Quantum Cryptography in Java](https://codefarm0.medium.com/post-quantum-cryptography-in-java-with-code-examples-74326adb0d3c)
- **JEP 497**: [Quantum-Resistant Module-Lattice-Based Digital Signature Algorithm](https://openjdk.org/jeps/497)
- **NIST FIPS 204**: ML-DSA Standard
- **JDK Version**: 25.0.2 (bundled)

## License

[Your license here]

## Contributing

Contributions welcome! Please read the documentation first to understand the hybrid approach.

---

**Ready to get started?** Check out [QUICKSTART.md](QUICKSTART.md) for a 5-minute guide!
