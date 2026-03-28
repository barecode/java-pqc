#!/bin/bash

# Run the Dilithium Signature Demo
# Uses the bundled JDK 25.0.2 with post-quantum cryptography support

JAVA_HOME="./jdk-25.0.2+10/Contents/Home"
JAVA="$JAVA_HOME/bin/java"

echo "Running Post-Quantum Cryptography Demo..."
echo "=========================================="
"$JAVA" DilithiumSignatureDemo

# Made with Bob
