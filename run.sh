#!/bin/bash
set -e
if [ ! -d build/classes ]; then
  ./build.sh
fi
echo "=== Running Ice & Spice demo ==="
CP="build/classes"
if ls lib/*.jar >/dev/null 2>&1; then CP="$CP:$(ls lib/*.jar | tr '\n' ':')"; fi
# UTF-8 output so the rupee sign and check marks print on any locale
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "$CP" restaurant.recommendation.Main
