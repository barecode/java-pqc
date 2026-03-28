# Post-Quantum Cryptography in Java

This project demonstrates quantum-resistant cryptography using ML-DSA (Module-Lattice-Based Digital Signature Algorithm) in Java, featuring both a signature demo and a REST/JSON HTTPS client-server for connectivity testing.

## Features

✅ **ML-DSA Digital Signatures** - Quantum-resistant signature generation and verification
✅ **PQC HTTPS Server** - REST server with ML-DSA certificates
✅ **PQC HTTPS Client** - Client that trusts ML-DSA certificates
✅ **SSL/TLS Info Endpoint** - Returns comprehensive session details as JSON
✅ **Easy Testing** - Simple scripts for server, client, and end-to-end testing

## Overview

- **Algorithm**: ML-DSA (formerly Dilithium)
- **JDK Version**: 25.0.2 (bundled)
- **Standard**: NIST Post-Quantum Cryptography standardization
- **Transport**: HTTPS with TLSv1.3

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

## REST/JSON HTTPS Client-Server

### Architecture

The project includes a complete HTTPS client-server implementation using ML-DSA certificates:

- **PQCHttpsServer**: HTTPS server with ML-DSA certificate
- **PQCHttpsClient**: Client that trusts ML-DSA certificates
- **Endpoint**: `GET /ssl-info` returns comprehensive SSL/TLS session information

### Starting the Server

```bash
# Default port (8443)
./start-server.sh

# Custom port
./start-server.sh 9443
```

The server will:
1. Generate ML-DSA certificates if they don't exist
2. Create keystore and truststore
3. Start HTTPS server on the specified port
4. Expose endpoints:
   - `GET /` - Server information
   - `GET /ssl-info` - SSL/TLS session details

### Running the Client

```bash
# Default URL (https://localhost:8443/ssl-info)
./start-client.sh

# Custom URL
./start-client.sh https://localhost:9443/ssl-info
```

### JSON Response Example

The `/ssl-info` endpoint returns comprehensive SSL/TLS session information:

```json
{
  "timestamp": "2026-03-28T02:45:00Z",
  "server": {
    "port": 8443,
    "protocol": "TLSv1.3"
  },
  "cipher": {
    "suite": "TLS_AES_256_GCM_SHA384",
    "protocol": "TLSv1.3"
  },
  "certificate": {
    "algorithm": "ML-DSA-65",
    "subject": "CN=localhost, O=PQC-HTTPS-Server, C=US",
    "issuer": "CN=localhost, O=PQC-HTTPS-Server, C=US",
    "serialNumber": "1234567890",
    "notBefore": "2026-03-28T00:00:00Z",
    "notAfter": "2027-03-28T00:00:00Z",
    "signatureAlgorithm": "ML-DSA-65",
    "publicKeySize": 1952,
    "version": 3
  },
  "session": {
    "id": "abc123...",
    "creationTime": 1234567890,
    "peerHost": "127.0.0.1",
    "peerPort": 54321
  }
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

For detailed implementation information, see:
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) - Detailed implementation guide
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture and design decisions
- [PLAN_SUMMARY.md](PLAN_SUMMARY.md) - Executive summary
