#!/usr/bin/env bash
set -euo pipefail

# Gera o par de chaves RSA usado para assinar os JWT.
# As chaves ficam fora do versionamento (ver .gitignore).

DIR="src/main/resources/keys"
mkdir -p "$DIR"

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$DIR/private_key.pem"
openssl rsa -pubout -in "$DIR/private_key.pem" -out "$DIR/public_key.pem"

echo "Chaves geradas em $DIR/"
