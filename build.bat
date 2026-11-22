@echo off
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
(for /r src %%f in (*.java) do @echo "%%f") > build\sources.txt
javac --release 17 -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1
jar --create --file build\Life-Game-timeline.jar --main-class com.holub.life.Life -C build\classes .
