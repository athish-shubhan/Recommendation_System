#!/bin/bash
# Compiles and runs the JUnit 5 tests in test/. The JUnit console launcher is
# downloaded once into build/test-lib/ (git-ignored); nothing else is needed.
set -e
JUNIT_VERSION=1.11.4
JUNIT_JAR="build/test-lib/junit-platform-console-standalone-$JUNIT_VERSION.jar"
./build.sh
if [ ! -f "$JUNIT_JAR" ]; then
  mkdir -p build/test-lib
  curl -fsSL -o "$JUNIT_JAR" \
    "https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/junit-platform-console-standalone-$JUNIT_VERSION.jar"
fi
mkdir -p build/test-classes
SOURCES="$(mktemp)"
trap 'rm -f "$SOURCES"' EXIT
find test -name "*.java" -type f > "$SOURCES"
javac -encoding UTF-8 -cp "build/classes:$JUNIT_JAR" -d build/test-classes @"$SOURCES"
java -Dfile.encoding=UTF-8 -jar "$JUNIT_JAR" execute \
  --class-path "build/classes:build/test-classes" --scan-class-path build/test-classes \
  --fail-if-no-tests --disable-banner --details=summary
