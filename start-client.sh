#!/bin/bash

# Start the PQC HTTPS Client
# Uses the bundled JDK 25.0.2 with post-quantum cryptography support

JAVA_HOME="./jdk-25.0.2+10/Contents/Home"
JAVA="$JAVA_HOME/bin/java"

# Default URL
DEFAULT_URL="https://localhost:8443/ssl-info"

# Get URL from command line argument or use default
URL=${1:-$DEFAULT_URL}

echo "Starting PQC HTTPS Client..."
echo "=============================="
echo "URL: $URL"
echo ""

# Run the client
"$JAVA" -cp src PQCHttpsClient "$URL"

# Made with Bob