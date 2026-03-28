#!/bin/bash

# End-to-end test for PQC HTTPS client-server connection
# Uses the bundled JDK 25.0.2 with post-quantum cryptography support

JAVA_HOME="./jdk-25.0.2+10/Contents/Home"
JAVA="$JAVA_HOME/bin/java"

# Test configuration
TEST_PORT=8443
TEST_URL="https://localhost:$TEST_PORT/ssl-info"
SERVER_STARTUP_DELAY=5

echo "=== PQC HTTPS Connection Test ==="
echo ""
echo "Configuration:"
echo "  Port: $TEST_PORT"
echo "  URL: $TEST_URL"
echo ""

# Check if files are compiled
if [ ! -f "src/PQCHttpsServer.class" ] || [ ! -f "src/PQCHttpsClient.class" ]; then
    echo "✗ Classes not found. Running compilation..."
    ./compile.sh
    if [ $? -ne 0 ]; then
        echo "✗ Compilation failed"
        exit 1
    fi
    echo ""
fi

# Start the server in background
echo "[1/4] Starting PQC HTTPS Server on port $TEST_PORT..."
"$JAVA" -cp src PQCHttpsServer "$TEST_PORT" > server.log 2>&1 &
SERVER_PID=$!

echo "      Server PID: $SERVER_PID"
echo "      Waiting $SERVER_STARTUP_DELAY seconds for server to start..."
sleep $SERVER_STARTUP_DELAY

# Check if server is still running
if ! ps -p $SERVER_PID > /dev/null; then
    echo "✗ Server failed to start. Check server.log for details."
    cat server.log
    exit 1
fi

echo "      ✓ Server started successfully"
echo ""

# Test server is listening
echo "[2/4] Checking if server is listening..."
if command -v nc &> /dev/null; then
    if nc -z localhost $TEST_PORT 2>/dev/null; then
        echo "      ✓ Server is listening on port $TEST_PORT"
    else
        echo "      ✗ Server is not listening on port $TEST_PORT"
        kill $SERVER_PID 2>/dev/null
        exit 1
    fi
else
    echo "      (nc not available, skipping port check)"
fi
echo ""

# Run the client
echo "[3/4] Running PQC HTTPS Client..."
echo "      Connecting to: $TEST_URL"
echo ""
"$JAVA" -cp src PQCHttpsClient "$TEST_URL"
CLIENT_EXIT_CODE=$?
echo ""

# Stop the server
echo "[4/4] Stopping server..."
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null
echo "      ✓ Server stopped"
echo ""

# Check results
if [ $CLIENT_EXIT_CODE -eq 0 ]; then
    echo "=============================="
    echo "✓ CONNECTION TEST SUCCESSFUL!"
    echo "=============================="
    echo ""
    echo "The PQC HTTPS client successfully connected to the server"
    echo "and retrieved SSL/TLS session information."
    echo ""
    echo "Server log saved to: server.log"
    exit 0
else
    echo "=============================="
    echo "✗ CONNECTION TEST FAILED"
    echo "=============================="
    echo ""
    echo "The client failed to connect to the server."
    echo "Check server.log for details."
    echo ""
    cat server.log
    exit 1
fi

# Made with Bob