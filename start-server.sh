#!/bin/bash

# Start the PQC HTTPS Server
# Uses the bundled JDK 25.0.2 with post-quantum cryptography support

JAVA_HOME="./jdk-25.0.2+10/Contents/Home"
JAVA="$JAVA_HOME/bin/java"

# Default port
DEFAULT_PORT=8443

# Get port from command line argument or use default
PORT=${1:-$DEFAULT_PORT}

echo "Starting PQC HTTPS Server..."
echo "=============================="
echo "Port: $PORT"
echo ""

# Run the server
"$JAVA" -cp src PQCHttpsServer "$PORT"

# Made with Bob