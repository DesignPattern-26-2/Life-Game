#!/usr/bin/env bash
# 소스(src)와 테스트(test)를 컴파일하고 JUnit 5 테스트를 창 없이(headless) 실행한다.
# 사용법: bash scripts/test.sh
# JDK는 JAVA_HOME이 있으면 그것을, 없으면 PATH의 java/javac를 쓴다.
set -euo pipefail
cd "$(dirname "$0")/.."

JUNIT_VERSION=1.10.2
JUNIT_JAR="lib/junit-platform-console-standalone-$JUNIT_VERSION.jar"
JUNIT_URL="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION/junit-platform-console-standalone-$JUNIT_VERSION.jar"

if [ -n "${JAVA_HOME:-}" ]; then
	JAVA="$JAVA_HOME/bin/java"
	JAVAC="$JAVA_HOME/bin/javac"
else
	JAVA=java
	JAVAC=javac
fi

# 클래스패스 구분자는 Windows(Git Bash)에서 ';', 그 밖에서는 ':'
case "$(uname -s)" in
	MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
	*)                    SEP=':' ;;
esac

# JUnit jar는 저장소에 올리지 않으므로, 없으면 Maven Central에서 내려받는다.
if [ ! -f "$JUNIT_JAR" ]; then
	mkdir -p lib
	echo "Downloading $JUNIT_URL"
	curl -fsSL -o "$JUNIT_JAR" "$JUNIT_URL"
fi

rm -rf out
mkdir -p out/main out/test
find src  -name '*.java' > out/main-sources.txt
find test -name '*.java' > out/test-sources.txt

"$JAVAC" -encoding UTF-8 -nowarn -d out/main @out/main-sources.txt
"$JAVAC" -encoding UTF-8 -d out/test -cp "out/main${SEP}${JUNIT_JAR}" @out/test-sources.txt

"$JAVA" -Djava.awt.headless=true -jar "$JUNIT_JAR" execute \
	--class-path "out/main${SEP}out/test" \
	--scan-class-path out/test \
	--details=tree \
	--disable-banner \
	--fail-if-no-tests
