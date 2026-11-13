#!/bin/bash
# Compiles the Java sources into build/classes (JDK 11+ required).
# src/ml/MLPredictionService.java needs the Jackson databind jars; put them in lib/
# to include it. Without them it is skipped, and the demo (Main) does not use it.
set -e
echo "=== Building Ice & Spice ==="
mkdir -p build/classes
SOURCES="$(mktemp)"
trap 'rm -f "$SOURCES"' EXIT
if ls lib/jackson-databind*.jar >/dev/null 2>&1; then
  CP="$(ls lib/*.jar | tr '\n' ':')"
  find src -name "*.java" -type f > "$SOURCES"
else
  CP="."
  find src -name "*.java" -type f ! -path "src/ml/*" > "$SOURCES"
  echo "lib/ has no Jackson jars: skipping src/ml (Python ML bridge client)"
fi
javac -encoding UTF-8 -cp "$CP" -d build/classes @"$SOURCES"
echo "Java compiled to build/classes"
echo "Build completed"
