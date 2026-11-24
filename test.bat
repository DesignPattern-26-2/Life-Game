@echo off
setlocal DisableDelayedExpansion
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
if not exist build\test-classes mkdir build\test-classes
> build\test-sources.txt (
    for /r tests %%f in (*.java) do (
        set "sourceFile=%%f"
        setlocal EnableDelayedExpansion
        echo "!sourceFile:\=/!"
        endlocal
    )
)
set "JUNIT_JAR=lib\junit-platform-console-standalone-1.11.4.jar"
javac --release 17 -encoding UTF-8 -cp "build\classes;%JUNIT_JAR%" -d build\test-classes @build\test-sources.txt
if errorlevel 1 exit /b 1
java -ea -Djava.awt.headless=true -jar "%JUNIT_JAR%" execute --class-path "build\classes;build\test-classes" --scan-class-path --reports-dir build\test-results --disable-ansi-colors
