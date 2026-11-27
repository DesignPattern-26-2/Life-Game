#!/usr/bin/env bash
# 소스(src)와 테스트(test, tests)를 컴파일하고 JUnit 5 테스트를 전부 실행한다.
# 테스트 폴더가 둘인 이유: 기능 브랜치마다 test/ 또는 tests/를 썼고, 통합하면서 둘 다 그대로 두었다.
# headless 옵션은 주지 않는다. 실제 메뉴를 만들어 확인하는 테스트(SimulationMenuTest의 RealMenu)는
# 화면이 있어야 실행되기 때문이다. 화면이 없는 환경에서는 그 테스트들만 aborted로 끝난다.
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
: > out/test-sources.txt
for dir in test tests; do
	if [ -d "$dir" ]; then
		find "$dir" -name '*.java' >> out/test-sources.txt
	fi
done

"$JAVAC" -encoding UTF-8 -nowarn -d out/main @out/main-sources.txt
"$JAVAC" -encoding UTF-8 -d out/test -cp "out/main${SEP}${JUNIT_JAR}" @out/test-sources.txt

# -ea: 타임라인·통계 테스트(tests/)가 처음부터 쓰던 옵션. assert 문도 검사한다.
"$JAVA" -ea -jar "$JUNIT_JAR" execute \
	--class-path "out/main${SEP}out/test" \
	--scan-class-path out/test \
	--details=tree \
	--disable-banner \
	--fail-if-no-tests
