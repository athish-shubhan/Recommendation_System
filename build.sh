#!/bin/bash
set -e
echo "=== Building Ice & Spice ==="
mkdir -p build/classes
find src -name "*.java" -type f > sources.txt
javac -d build/classes @sources.txt
rm sources.txt
echo "✓ Java compiled to build/classes"
echo "Setting up Python (optional)..."
cd python && python3 -m pip install -r requirements.txt || true
echo "✓ Build completed"
