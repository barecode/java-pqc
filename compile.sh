#!/bin/bash

# Compile the Dilithium Signature Demo
# Uses the bundled JDK 25.0.2 with post-quantum cryptography support

JAVA_HOME="./jdk-25.0.2+10/Contents/Home"
JAVAC="$JAVA_HOME/bin/javac"

echo "Compiling DilithiumSignatureDemo.java..."
"$JAVAC" DilithiumSignatureDemo.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful!"
    echo "Run './run.sh' to execute the demo"
else
    echo "✗ Compilation failed"
    exit 1
fi

# Made with Bob
