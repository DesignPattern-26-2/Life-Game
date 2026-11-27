@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
set "JAVAC=javac"
set "JAVA=java"
if defined JAVA_HOME (
    set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    set "JAVA=%JAVA_HOME%\bin\java.exe"
)
"%JAVAC%" -encoding UTF-8 -d bin -sourcepath src src\com\holub\life\Life.java
if errorlevel 1 (
    pause
    exit /b 1
)
"%JAVA%" -cp bin com.holub.life.Life
