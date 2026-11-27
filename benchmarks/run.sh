#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
sh build.sh
mkdir -p build/benchmark-classes
javac --release 17 -cp build/classes -d build/benchmark-classes benchmarks/MemoryProbe.java benchmarks/IntegratedBoardProbe.java
printf 'Premain-Class: com.holub.life.MemoryProbe\n' > build/benchmark-manifest.txt
jar --create --file build/memory-probe.jar --manifest build/benchmark-manifest.txt -C build/benchmark-classes .
java -Xmx128m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC --add-opens java.base/java.util=ALL-UNNAMED -Djava.awt.headless=true -javaagent:build/memory-probe.jar -cp build/classes:build/benchmark-classes com.holub.life.MemoryProbe 100000
java -Xmx256m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC -Djava.awt.headless=true -cp build/classes:build/benchmark-classes com.holub.life.IntegratedBoardProbe build/integrated-preview.png build/integrated-cluster-preview.png
