#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
find src -name '*.java' | sort > build/sources.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
jar --create --file build/Life-Game-timeline.jar --main-class com.holub.life.Life -C build/classes .
printf 'Built: build/Life-Game-timeline.jar\n'
