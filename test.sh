#!/bin/sh
set -eu
cd "$(dirname "$0")"
sh build.sh
mkdir -p build/test-classes build/test-results
JUNIT_JAR=lib/junit-platform-console-standalone-1.11.4.jar
find tests -name '*.java' | sort > build/test-sources.txt
javac --release 17 -encoding UTF-8 -cp "build/classes:$JUNIT_JAR" -d build/test-classes @build/test-sources.txt
java -ea -Djava.awt.headless=true -jar "$JUNIT_JAR" execute --class-path build/classes:build/test-classes --scan-class-path --reports-dir build/test-results --disable-ansi-colors
