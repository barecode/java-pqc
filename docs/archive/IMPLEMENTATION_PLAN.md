# REST/JSON Client-Server Implementation Plan
## Post-Quantum HTTPS with ML-DSA Certificates

## Overview
This plan outlines the implementation of a REST/JSON client-server application using pure Java ML-DSA (Module-Lattice-Based Digital Signature Algorithm) certificates for HTTPS connectivity testing.

## Architecture

```mermaid
graph TB
    subgraph "Server Side"
        A[start-server.sh] -->|port arg| B[PQCHttpsServer]
        B --> C[CertificateGenerator]
        C -->|ML-DSA cert| D[KeyStoreManager]
        D -->|keystore| E[HttpsServer]
        E --> F[/ssl-info endpoint]
    end
    
    subgraph "Client Side"
        G[start-client.sh] -->|URL arg| H[PQCHttpsClient]
        H --> I[KeyStoreManager]
        I -->|truststore| J[HttpsURLConnection]
    end
    
    J -->|HTTPS GET| F
    F -->|JSON response| J
    
    style C fill:#e1f5ff
    style D fill:#e1f5ff
    style F fill:#ffe1e1
    style I fill:#e1f5ff
```

## Component Design

### 1. CertificateGenerator.java
**Purpose**: Generate ML-DSA self-signed certificates for HTTPS

**Key Methods**:
- `generateKeyPair(String algorithm)` - Generate ML-DSA key pair (ML-DSA-44/65/87)
- `generateSelfSignedCertificate(KeyPair, String subject, int validityDays)` - Create X.509 certificate
- `saveCertificateChain(Certificate[], String filename)` - Export certificate chain

**ML-DSA Integration**:
- Uses `KeyPairGenerator.getInstance("ML-DSA-65")` for quantum-resistant keys
- Creates X.509 certificates with ML-DSA public keys
- Supports all three security levels: ML-DSA-44, ML-DSA-65, ML-DSA-87

### 2. KeyStoreManager.java
**Purpose**: Manage keystores and truststores for ML-DSA certificates

**Key Methods**:
- `createKeyStore(KeyPair, Certificate[], String alias, char[] password)` - Create keystore with ML-DSA cert
- `createTrustStore(Certificate cert, String alias)` - Create truststore for client
- `saveKeyStore(KeyStore, String filename, char[] password)` - Persist keystore to disk
- `loadKeyStore(String filename, char[] password)` - Load existing keystore

**Storage Format**:
- Uses JKS (Java KeyStore) format
- Stores ML-DSA private keys and certificate chains
- Separate truststore for client certificate validation

### 3. PQCHttpsServer.java
**Purpose**: HTTPS REST server with ML-DSA certificate support

**Key Features**:
- Embedded `com.sun.net.httpserver.HttpsServer`
- Configurable port via command-line argument (default: 8443)
- Automatic ML-DSA certificate generation on startup
- Single endpoint: `/ssl-info`

**Endpoint: GET /ssl-info**
Returns comprehensive SSL/TLS session information:
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

**Implementation Details**:
- Uses `HttpsServer.create()` with custom `SSLContext`
- Configures `SSLContext` with ML-DSA keystore
- Extracts SSL session info from `HttpsExchange.getSSLSession()`
- Returns JSON using simple string formatting or lightweight JSON library

### 4. PQCHttpsClient.java
**Purpose**: HTTPS REST client that trusts ML-DSA certificates

**Key Features**:
- Configurable server URL via command-line argument
- Custom `SSLContext` that trusts ML-DSA certificates
- HTTP GET request to `/ssl-info` endpoint
- Pretty-print JSON response

**Implementation Details**:
- Uses `HttpsURLConnection` with custom `SSLSocketFactory`
- Loads truststore containing server's ML-DSA certificate
- Disables hostname verification for localhost testing (configurable)
- Parses and displays JSON response

## File Structure

```
java-pqc/
├── src/
│   ├── CertificateGenerator.java
│   ├── KeyStoreManager.java
│   ├── PQCHttpsServer.java
│   └── PQCHttpsClient.java
├── certs/                          # Generated at runtime
│   ├── server-keystore.jks
│   └── client-truststore.jks
├── start-server.sh                 # Server startup script
├── start-client.sh                 # Client startup script
├── compile.sh                      # Updated compilation script
├── test-connection.sh              # End-to-end test script
├── DilithiumSignatureDemo.java     # Existing demo
└── README.md                       # Updated documentation
```

## Implementation Steps

### Phase 1: Certificate Infrastructure
1. Create `CertificateGenerator.java` with ML-DSA key pair generation
2. Implement self-signed certificate creation using ML-DSA
3. Create `KeyStoreManager.java` for keystore/truststore management
4. Test certificate generation and storage

### Phase 2: HTTPS Server
1. Implement `PQCHttpsServer.java` with embedded HttpsServer
2. Configure SSLContext with ML-DSA keystore
3. Create `/ssl-info` endpoint with SSL session extraction
4. Implement JSON response formatting
5. Add port configuration via command-line argument

### Phase 3: HTTPS Client
1. Implement `PQCHttpsClient.java` with HttpsURLConnection
2. Configure custom SSLContext with truststore
3. Implement GET request to `/ssl-info` endpoint
4. Add JSON response parsing and display
5. Add URL configuration via command-line argument

### Phase 4: Scripts and Testing
1. Create `start-server.sh` with port argument
2. Create `start-client.sh` with URL argument
3. Update `compile.sh` to build all new classes
4. Create `test-connection.sh` for automated testing
5. Test end-to-end connectivity

### Phase 5: Documentation
1. Update README.md with new architecture section
2. Add usage examples for server and client
3. Document JSON response structure
4. Add troubleshooting guide

## Technical Considerations

### ML-DSA Certificate Challenges
- **JDK Support**: Requires JDK 24+ with JEP 497 support (already bundled)
- **Certificate Format**: X.509 certificates with ML-DSA public keys
- **Signature Algorithm**: ML-DSA-65 (default) for certificate signing
- **Compatibility**: Pure Java implementation, no external libraries needed

### SSL/TLS Configuration
- **Protocol**: TLSv1.3 (latest, most secure)
- **Cipher Suites**: Let Java negotiate best available
- **Certificate Validation**: Custom truststore for ML-DSA certificates
- **Hostname Verification**: Disabled for localhost testing (can be enabled for production)

### Security Notes
- Self-signed certificates for testing/demo purposes
- In production, use proper CA-signed ML-DSA certificates when available
- Keystore passwords should be configurable (not hardcoded)
- Consider certificate rotation and expiration handling

## Testing Strategy

### Unit Tests
- Certificate generation with different ML-DSA variants
- Keystore creation and loading
- JSON response formatting

### Integration Tests
1. Start server on port 8443
2. Verify server is listening
3. Run client to connect and retrieve SSL info
4. Validate JSON response structure
5. Verify ML-DSA certificate details

### End-to-End Test Script
```bash
#!/bin/bash
# test-connection.sh

echo "Starting PQC HTTPS Server on port 8443..."
./start-server.sh 8443 &
SERVER_PID=$!
sleep 3

echo "Testing client connection..."
./start-client.sh https://localhost:8443/ssl-info

echo "Stopping server..."
kill $SERVER_PID
```

## Success Criteria

- [ ] Server starts successfully with ML-DSA certificate
- [ ] Client connects via HTTPS using ML-DSA certificate
- [ ] JSON response contains comprehensive SSL session details
- [ ] Port is configurable via command-line argument
- [ ] All components compile without errors
- [ ] End-to-end test passes successfully
- [ ] Documentation is complete and accurate

## Future Enhancements

1. **Multiple Endpoints**: Add more REST endpoints for testing
2. **Certificate Rotation**: Implement automatic certificate renewal
3. **Mutual TLS**: Add client certificate authentication
4. **Performance Metrics**: Add timing and throughput measurements
5. **Configuration File**: Support external configuration for advanced settings
6. **Docker Support**: Containerize for easy deployment
7. **Monitoring**: Add health check endpoint and metrics

## References

- [JEP 497: Quantum-Resistant Module-Lattice-Based Digital Signature Algorithm](https://openjdk.org/jeps/497)
- [Java HttpsServer Documentation](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpsServer.html)
- [Java SSLContext Documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/net/ssl/SSLContext.html)
- [X.509 Certificate Format](https://datatracker.ietf.org/doc/html/rfc5280)