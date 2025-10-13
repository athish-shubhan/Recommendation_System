#!/bin/bash
set -e
if [ ! -d build/classes ]; then
  ./build.sh
fi
echo "=== Running Ice & Spice demo ==="
java -cp build/classes restaurant.recommendation.Main
