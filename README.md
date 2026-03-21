# Post-Quantum Cryptography Demo in Java

This project demonstrates quantum-resistant digital signatures using the ML-DSA (Module-Lattice-Based Digital Signature Algorithm) in Java.

## Overview

- **Algorithm**: ML-DSA (formerly Dilithium)
- **JDK Version**: 25.0.2 (bundled)
- **Standard**: NIST Post-Quantum Cryptography standardization

## What is ML-DSA?

ML-DSA is a quantum-resistant digital signature algorithm based on lattice cryptography. It's designed to remain secure even against attacks from quantum computers, which could break traditional RSA and ECDSA signatures.

## Quick Start

### 1. Make scripts executable
```bash
chmod +x compile.sh run.sh
```

### 2. Compile the demo
```bash
./compile.sh
```

### 3. Run the demo

Run with default algorithm (ML-DSA):
```bash
./run.sh
```

Or specify a specific algorithm variant:
```bash
./jdk-25.0.2+10/Contents/Home/bin/java DilithiumSignatureDemo ML-DSA-44
./jdk-25.0.2+10/Contents/Home/bin/java DilithiumSignatureDemo ML-DSA-65
./jdk-25.0.2+10/Contents/Home/bin/java DilithiumSignatureDemo ML-DSA-87
```

## Expected Output

```
Running Post-Quantum Cryptography Demo...
==========================================
Signature Verified: true
```

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