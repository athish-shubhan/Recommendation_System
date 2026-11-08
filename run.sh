#!/bin/bash
set -e
if [ ! -d build/classes ]; then
  ./build.sh
fi
echo "=== Running Ice & Spice demo ==="
CP="build/classes"
if ls lib/*.jar >/dev/null 2>&1; then CP="$CP:$(ls lib/*.jar | tr '\n' ':')"; fi
java -cp "$CP" restaurant.recommendation.Main
