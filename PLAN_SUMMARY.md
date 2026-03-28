# REST/JSON Client-Server Implementation Plan Summary

## Executive Summary

This plan outlines the implementation of a REST/JSON client-server application using **pure Java ML-DSA (Module-Lattice-Based Digital Signature Algorithm)** certificates for HTTPS connectivity testing. The solution leverages the bundled JDK 25.0.2 with post-quantum cryptography support (JEP 497) to create a quantum-resistant HTTPS communication channel.

## Goals

1. **Create HTTPS REST Server**: Embedded Java HTTPS server with ML-DSA certificate support
2. **Create HTTPS REST Client**: Java client that trusts ML-DSA certificates
3. **SSL Information Endpoint**: Server endpoint that returns comprehensive SSL/TLS session details as JSON
4. **Configurable Deployment**: Port configuration via command-line arguments
5. **Easy Testing**: Simple scripts for starting server, client, and end-to-end testing

## Core Components

### 1. Certificate Infrastructure
- **CertificateGenerator.java**: Generates ML-DSA self-signed certificates
- **KeyStoreManager.java**: Manages keystores and truststores for ML-DSA certificates

### 2. Server Components
- **PQCHttpsServer.java**: Embedded HTTPS server with ML-DSA certificate
- **Endpoint**: `GET /ssl-info` returns comprehensive SSL session information

### 3. Client Components
- **PQCHttpsClient.java**: HTTPS client that trusts ML-DSA certificates
- **Functionality**: Connects to server and displays SSL information

### 4. Scripts
- **start-server.sh**: Launch server with configurable port (default: 8443)
- **start-client.sh**: Launch client with configurable URL (default: https://localhost:8443)
- **test-connection.sh**: Automated end-to-end testing
- **compile.sh**: Updated to build all components

## JSON Response Structure

The `/ssl-info` endpoint returns comprehensive SSL/TLS session details:

```json
{
  "timestamp": "2026-03-28T02:45:00Z",
  "server": {
    "port": 8443,
    "protocol": "TLSv1.3"
  },
  "cipher": {
    "suite": "TLS_AES_256_GCM_SHA384",
    "protocol": "TLSv1.3",
    "strength": 256
  },
  "certificate": {
    "algorithm": "ML-DSA-65",
    "subject": "CN=PQC-HTTPS-Server",
    "issuer": "CN=PQC-HTTPS-Server",
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

## Implementation Phases

### Phase 1: Certificate Infrastructure ✓ Planned
- Create CertificateGenerator with ML-DSA key pair generation
- Implement self-signed certificate creation
- Create KeyStoreManager for keystore/truststore management
- Test certificate generation and storage

### Phase 2: HTTPS Server ✓ Planned
- Implement PQCHttpsServer with embedded HttpsServer
- Configure SSLContext with ML-DSA keystore
- Create /ssl-info endpoint with SSL session extraction
- Implement JSON response formatting
- Add port configuration

### Phase 3: HTTPS Client ✓ Planned
- Implement PQCHttpsClient with HttpsURLConnection
- Configure custom SSLContext with truststore
- Implement GET request to /ssl-info endpoint
- Add JSON response parsing and display
- Add URL configuration

### Phase 4: Scripts and Testing ✓ Planned
- Create start-server.sh with port argument
- Create start-client.sh with URL argument
- Update compile.sh to build all classes
- Create test-connection.sh for automated testing
- Test end-to-end connectivity

### Phase 5: Documentation ✓ Planned
- Update README.md with new architecture
- Add usage examples
- Document JSON response structure
- Add troubleshooting guide

## Key Design Decisions

### ✅ Pure Java ML-DSA Implementation
- Uses only Java's built-in ML-DSA support (JEP 497)
- No external dependencies required
- Leverages bundled JDK 25.0.2

### ✅ Self-Signed Certificates
- Generated at runtime for demo/testing
- No CA infrastructure needed
- 365-day validity period

### ✅ Embedded HTTP Server
- Uses `com.sun.net.httpserver.HttpsServer`
- Lightweight and easy to configure
- Perfect for testing and demos

### ✅ Comprehensive SSL Information
- Returns detailed SSL/TLS session data
- Includes certificate details, cipher info, and session metadata
- Machine-readable JSON format

## Usage Examples

### Starting the Server
```bash
# Default port (8443)
./start-server.sh

# Custom port
./start-server.sh 9443
```

### Starting the Client
```bash
# Default URL (https://localhost:8443)
./start-client.sh

# Custom URL
./start-client.sh https://localhost:9443/ssl-info
```

### Running End-to-End Test
```bash
./test-connection.sh
```

### Compiling All Components
```bash
./compile.sh
```

## Project Structure

```
java-pqc/
├── src/
│   ├── CertificateGenerator.java       # ML-DSA certificate generation
│   ├── KeyStoreManager.java            # Keystore/truststore management
│   ├── PQCHttpsServer.java             # HTTPS REST server
│   └── PQCHttpsClient.java             # HTTPS REST client
├── certs/                               # Runtime-generated (gitignored)
│   ├── server-keystore.jks
│   └── client-truststore.jks
├── docs/
│   ├── IMPLEMENTATION_PLAN.md          # Detailed implementation plan
│   ├── ARCHITECTURE.md                 # Architecture diagrams
│   └── PLAN_SUMMARY.md                 # This file
├── start-server.sh                     # Server launcher
├── start-client.sh                     # Client launcher
├── test-connection.sh                  # End-to-end test
├── compile.sh                          # Build script (updated)
├── DilithiumSignatureDemo.java         # Original PQC demo
└── README.md                           # Project documentation (updated)
```

## Technical Highlights

### Post-Quantum Cryptography
- **Algorithm**: ML-DSA-65 (balanced security and performance)
- **Security Level**: ~192-bit quantum resistance
- **Key Size**: ~1952 bytes public key
- **Signature Size**: ~3309 bytes

### SSL/TLS Configuration
- **Protocol**: TLSv1.3 (latest and most secure)
- **Cipher Suites**: Negotiated by Java SSL implementation
- **Certificate Format**: X.509 with ML-DSA public keys
- **Keystore Format**: JKS (Java KeyStore)

### Performance Expectations
- **Server Startup**: ~1-2 seconds (includes cert generation)
- **TLS Handshake**: ~50-100ms
- **Request Latency**: <10ms for /ssl-info endpoint
- **Throughput**: Suitable for testing and demos

## Success Criteria

- [x] Plan created with clear implementation steps
- [x] Architecture documented with diagrams
- [x] JSON response structure defined
- [ ] All Java classes implemented and tested
- [ ] Scripts created and functional
- [ ] End-to-end test passes successfully
- [ ] Documentation complete and accurate

## Next Steps

### Ready for Implementation
The planning phase is complete. The next step is to switch to **Code mode** to implement the solution according to this plan.

### Implementation Order
1. Create certificate infrastructure (CertificateGenerator, KeyStoreManager)
2. Implement HTTPS server (PQCHttpsServer)
3. Implement HTTPS client (PQCHttpsClient)
4. Create startup and test scripts
5. Update documentation

### Estimated Timeline
- **Certificate Infrastructure**: 2-3 hours
- **HTTPS Server**: 2-3 hours
- **HTTPS Client**: 1-2 hours
- **Scripts and Testing**: 1-2 hours
- **Documentation**: 1 hour
- **Total**: 7-11 hours of development time

## Questions or Concerns?

Before proceeding to implementation, please review:
1. Is the JSON response structure comprehensive enough?
2. Are there any additional endpoints needed?
3. Should we support multiple ML-DSA variants (44/65/87)?
4. Any specific error handling requirements?
5. Any additional testing scenarios to consider?

## References

- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) - Detailed implementation guide
- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture diagrams and design decisions
- [JEP 497: ML-DSA](https://openjdk.org/jeps/497) - Java post-quantum cryptography
- [DilithiumSignatureDemo.java](DilithiumSignatureDemo.java) - Existing PQC demo

---

**Status**: ✅ Planning Complete - Ready for Implementation
**Next Action**: Switch to Code mode to begin implementation