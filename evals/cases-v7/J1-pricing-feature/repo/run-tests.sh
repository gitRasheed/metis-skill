#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build
mkdir -p build/classes
find src/main/java src/test/java -name '*.java' > build/sources.txt
javac -d build/classes @build/sources.txt
java -cp build/classes com.acme.pricing.testing.TestRunner build/classes
