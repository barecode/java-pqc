#!/bin/bash

# Compile all Java files
# Uses the bundled JDK 25.0.2 with post-quantum cryptography support

JAVA_HOME="./jdk-25.0.2+10/Contents/Home"
JAVAC="$JAVA_HOME/bin/javac"

echo "Compiling Java files..."
echo "======================="

# Create src directory if it doesn't exist
mkdir -p src

# Compile original demo
echo ""
echo "[1/5] Compiling DilithiumSignatureDemo.java..."
"$JAVAC" DilithiumSignatureDemo.java

if [ $? -ne 0 ]; then
    echo "✗ Compilation failed"
    exit 1
fi

# Compile new PQC HTTPS components
echo ""
echo "[2/5] Compiling CertificateGenerator.java..."
"$JAVAC" src/CertificateGenerator.java

if [ $? -ne 0 ]; then
    echo "✗ Compilation failed"
    exit 1
fi

echo ""
echo "[3/5] Compiling KeyStoreManager.java..."
"$JAVAC" src/KeyStoreManager.java

if [ $? -ne 0 ]; then
    echo "✗ Compilation failed"
    exit 1
fi

echo ""
echo "[4/5] Compiling PQCHttpsServer.java..."
"$JAVAC" -cp src src/PQCHttpsServer.java

if [ $? -ne 0 ]; then
    echo "✗ Compilation failed"
    exit 1
fi

echo ""
echo "[5/5] Compiling PQCHttpsClient.java..."
"$JAVAC" -cp src src/PQCHttpsClient.java

if [ $? -ne 0 ]; then
    echo "✗ Compilation failed"
    exit 1
fi

echo ""
echo "✓ All files compiled successfully!"
echo ""
echo "Available commands:"
echo "  ./run.sh                  - Run the original Dilithium signature demo"
echo "  ./start-server.sh [port]  - Start the PQC HTTPS server (default port: 8443)"
echo "  ./start-client.sh [url]   - Start the PQC HTTPS client (default: https://localhost:8443/ssl-info)"
echo "  ./test-connection.sh      - Run end-to-end connectivity test"

# Made with Bob
