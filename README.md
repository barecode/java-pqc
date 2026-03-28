# Post-Quantum Cryptography in Java (Hybrid Approach)

This project demonstrates a **hybrid approach** to post-quantum cryptography using ML-DSA (Module-Lattice-Based Digital Signature Algorithm) in Java. It combines traditional ECDSA for TLS compatibility with ML-DSA-65 for quantum-resistant application-level security.

## Features

✅ **Hybrid PQC Architecture** - ECDSA for TLS, ML-DSA for application layer
✅ **ML-DSA Digital Signatures** - Quantum-resistant signature generation and verification
✅ **HTTPS Server** - REST server with ECDSA certificates and ML-DSA signing endpoints
✅ **HTTPS Client** - Client demonstrating both TLS and ML-DSA operations
✅ **SSL/TLS Info Endpoint** - Returns comprehensive session details as JSON
✅ **PQC Sign/Verify Endpoints** - Application-level ML-DSA-65 operations
✅ **Easy Testing** - Simple scripts for server, client, and end-to-end testing

## Overview

- **TLS Layer**: ECDSA (secp384r1) - Compatible with all clients
- **Application Layer**: ML-DSA-65 - Quantum-resistant signatures
- **JDK Version**: 25.0.2 (bundled)
- **Standard**: NIST FIPS 204 (ML-DSA)
- **Transport**: HTTPS with TLSv1.3

## Why Hybrid?

Pure ML-DSA certificates don't work with current TLS implementations because ML-DSA is not yet integrated into the TLS handshake protocol. Our hybrid approach:
- ✓ Uses ECDSA for TLS (works today)
- ✓ Uses ML-DSA for application security (quantum-resistant)
- ✓ Provides migration path for future TLS 1.3 PQC support

See [HYBRID_APPROACH.md](HYBRID_APPROACH.md) and [SSL_HANDSHAKE_ANALYSIS.md](SSL_HANDSHAKE_ANALYSIS.md) for details.

## What is ML-DSA?

ML-DSA is a quantum-resistant digital signature algorithm based on lattice cryptography. It's designed to remain secure even against attacks from quantum computers, which could break traditional RSA and ECDSA signatures.

## Quick Start

### 1. Make scripts executable
```bash
chmod +x *.sh
```

### 2. Compile all components
```bash
./compile.sh
```

### 3. Run the signature demo
```bash
./run.sh
```

### 4. Test the HTTPS client-server
```bash
# Run end-to-end test (recommended)
./test-connection.sh

# Or manually:
# Terminal 1 - Start server
./start-server.sh

# Terminal 2 - Run client
./start-client.sh
```

## REST/JSON HTTPS Client-Server (Hybrid Architecture)

### Architecture

The project implements a hybrid post-quantum cryptography approach:

**TLS Layer (ECDSA)**:
- HTTPS connection using ECDSA certificates
- Compatible with all modern clients
- Provides immediate deployment capability

**Application Layer (ML-DSA-65)**:
- Post-quantum signature operations
- Quantum-resistant security
- Demonstrates PQC capabilities

**Endpoints**:
- `GET /` - Server information
- `GET /ssl-info` - SSL/TLS session details (shows ECDSA certificate)
- `POST /pqc-sign` - Sign message with ML-DSA-65
- `POST /pqc-verify` - Verify ML-DSA-65 signature

### Starting the Server

```bash
# Default port (8443)
./start-server.sh

# Custom port
./start-server.sh 9443
```

The server will:
1. Generate ECDSA certificates if they don't exist (for TLS)
2. Generate ML-DSA-65 key pair if it doesn't exist (for application signing)
3. Create keystore and truststore
4. Start HTTPS server on the specified port
5. Expose endpoints:
   - `GET /` - Server information
   - `GET /ssl-info` - SSL/TLS session details
   - `POST /pqc-sign` - Sign message with ML-DSA-65
   - `POST /pqc-verify` - Verify ML-DSA-65 signature

### Running the Client

```bash
# Run full hybrid demonstration
./start-client.sh
```

The client will:
1. Establish TLS connection using ECDSA certificate
2. Retrieve SSL/TLS session information
3. Test ML-DSA-65 signing (send message to server)
4. Test ML-DSA-65 verification (verify signature)
5. Test with tampered message (should fail verification)

### API Examples

#### GET /ssl-info - TLS Session Information
Returns ECDSA certificate details:
```json
{
  "timestamp": "2026-03-28T11:06:15Z",
  "server": {
    "port": 8443,
    "protocol": "TLSv1.3"
  },
  "cipher": {
    "suite": "TLS_AES_256_GCM_SHA384",
    "protocol": "TLSv1.3"
  },
  "certificate": {
    "algorithm": "EC",
    "subject": "CN=localhost, O=PQC-HTTPS-Server, C=US",
    "issuer": "CN=localhost, O=PQC-HTTPS-Server, C=US",
    "signatureAlgorithm": "SHA384withECDSA",
    "publicKeySize": 120,
    "version": 3
  }
}
```

#### POST /pqc-sign - ML-DSA Signing
Request:
```bash
curl -k -X POST https://localhost:8443/pqc-sign \
  -d "Hello, Post-Quantum World!"
```

Response:
```json
{
  "message": "Hello, Post-Quantum World!",
  "signature": "bS8c+w2oq5tzvOwV2t8Q...",
  "algorithm": "ML-DSA-65",
  "signatureSize": 3309,
  "publicKey": "MIIHsjALBglghkgBZQME..."
}
```

#### POST /pqc-verify - ML-DSA Verification
Request:
```bash
curl -k -X POST https://localhost:8443/pqc-verify \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello","signature":"..."}'
```

Response:
```json
{
  "message": "Hello, Post-Quantum World!",
  "valid": true,
  "algorithm": "ML-DSA-65"
}
```

### End-to-End Testing

Run the automated test script:

```bash
./test-connection.sh
```

This will:
1. Compile all components (if needed)
2. Start the server in the background
3. Run the client to connect and retrieve SSL info
4. Stop the server

## Signature Demo

### How It Works

The signature demo performs the following steps:

1. **Key Generation**: Creates a quantum-resistant key pair using ML-DSA
2. **Signing**: Signs the message "Post-Quantum Java is here!" with the private key
3. **Verification**: Verifies the signature using the public key
4. **Result**: Displays whether the signature is valid

5. Report success or failure

## How It Works

The demo performs the following steps:

1. **Key Generation**: Creates a quantum-resistant key pair using ML-DSA
2. **Signing**: Signs the message "Post-Quantum Java is here!" with the private key
3. **Verification**: Verifies the signature using the public key
4. **Result**: Displays whether the signature is valid

## Algorithm Variants

ML-DSA supports three security levels (from JEP 497):

| Algorithm | Security Level | Signature Size | Performance |
|-----------|---------------|----------------|-------------|
| ML-DSA-44 | ~128-bit | ~2420 bytes | Fastest |
| ML-DSA-65 | ~192-bit | ~3309 bytes | Balanced (Default) |
| ML-DSA-87 | ~256-bit | ~4627 bytes | Most Secure |

You can specify the algorithm variant as a command-line argument:
```bash
./jdk-25.0.2+10/Contents/Home/bin/java DilithiumSignatureDemo ML-DSA-87
```

## Technical Details

- **Source**: Based on [Post-Quantum Cryptography in Java](https://codefarm0.medium.com/post-quantum-cryptography-in-java-with-code-examples-74326adb0d3c)
- **JEP 497**: [Quantum-Resistant Module-Lattice-Based Digital Signature Algorithm](https://openjdk.org/jeps/497)
- **JEP 496**: [Quantum-Resistant Module-Lattice-Based Key Encapsulation Mechanism](https://openjdk.org/jeps/496)

## Requirements

- macOS (bundled JDK is for macOS)
- Bash shell
- No additional dependencies required (JDK is bundled)

## Project Structure

```
.
├── DilithiumSignatureDemo.java  # Main demo code
├── compile.sh                    # Compilation script
├── run.sh                        # Execution script
├── README.md                     # This file
└── jdk-25.0.2+10/               # Bundled JDK with PQC support
```

## Why Post-Quantum Cryptography?

Quantum computers pose a significant threat to current cryptographic systems. ML-DSA and other post-quantum algorithms are designed to be secure against both classical and quantum attacks, ensuring long-term security for digital signatures.


## Project Structure

```
.
├── src/                          # Source code
│   ├── CertificateGenerator.java # ML-DSA certificate generation
│   ├── KeyStoreManager.java      # Keystore/truststore management
│   ├── PQCHttpsServer.java       # HTTPS REST server
│   └── PQCHttpsClient.java       # HTTPS REST client
├── certs/                        # Generated certificates (gitignored)
│   ├── server-keystore.jks       # Server private key + certificate
│   └── client-truststore.jks     # Client trusted certificates
├── docs/                         # Documentation
│   ├── IMPLEMENTATION_PLAN.md    # Detailed implementation plan
│   ├── ARCHITECTURE.md           # Architecture diagrams
│   └── PLAN_SUMMARY.md           # Plan summary
├── DilithiumSignatureDemo.java   # Original signature demo
├── compile.sh                    # Build all components
├── run.sh                        # Run signature demo
├── start-server.sh               # Start HTTPS server
├── start-client.sh               # Start HTTPS client
├── test-connection.sh            # End-to-end test
├── README.md                     # This file
└── jdk-25.0.2+10/               # Bundled JDK with PQC support
```

## Components

### Certificate Infrastructure

- **CertificateGenerator**: Generates ML-DSA key pairs (simplified, uses KeyStoreManager)
- **KeyStoreManager**: Creates keystores and truststores using `keytool`
  - Generates self-signed ML-DSA certificates
  - Manages keystore (server private key + certificate)
  - Exports certificates to truststore (for client)

### HTTPS Server

- **PQCHttpsServer**: Embedded HTTPS server using `com.sun.net.httpserver.HttpsServer`
  - Configurable port (default: 8443)
  - Automatic certificate generation on first run
  - Endpoints:
    - `GET /` - Server information
    - `GET /ssl-info` - Comprehensive SSL/TLS session details

### HTTPS Client

- **PQCHttpsClient**: HTTPS client using `HttpsURLConnection`
  - Trusts ML-DSA certificates via custom truststore
  - Configurable URL (default: https://localhost:8443/ssl-info)
  - Pretty-prints JSON responses

## Troubleshooting

### Server won't start
- **Port already in use**: Try a different port with `./start-server.sh 9443`
- **Certificate generation failed**: Check that `keytool` is available in the bundled JDK

### Client connection fails
- **Server not running**: Start the server first with `./start-server.sh`
- **Truststore not found**: The server creates it automatically on first run
- **Certificate mismatch**: Delete `certs/` directory and restart the server

### Compilation errors
- **Classes not found**: Ensure you're using the bundled JDK 25.0.2
- **Module access errors**: The code uses standard Java APIs only

## Documentation

For detailed information, see:
- [HYBRID_APPROACH.md](HYBRID_APPROACH.md) - **Complete hybrid architecture explanation**
- [SSL_HANDSHAKE_ANALYSIS.md](SSL_HANDSHAKE_ANALYSIS.md) - **Why pure ML-DSA doesn't work**
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) - Implementation guide
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture
- [PLAN_SUMMARY.md](PLAN_SUMMARY.md) - Executive summary

## Security Model

### TLS Layer (ECDSA)
- **Algorithm**: ECDSA with secp384r1 curve
- **Security**: Strong against classical computers
- **Quantum Threat**: Vulnerable to Shor's algorithm (future threat)
- **Purpose**: Provides immediate compatibility

### Application Layer (ML-DSA-65)
- **Algorithm**: ML-DSA-65 (NIST FIPS 204)
- **Security**: Resistant to quantum attacks
- **Signature Size**: 3,309 bytes
- **Purpose**: Provides long-term quantum resistance

### Combined Security
- ✓ Works with existing infrastructure today
- ✓ Application data protected by quantum-resistant signatures
- ✓ Clear migration path to full PQC when TLS standards arrive
