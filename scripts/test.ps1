# Compiles src/ and both test folders (test/ and tests/), then runs the JUnit 5 tests headless (no window).
# There are two test folders because feature branches used either name and the merge kept both.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\test.ps1
# Uses the JDK in JAVA_HOME if it is set, otherwise java/javac from PATH.
# (Comments are ASCII on purpose: Windows PowerShell 5.1 misreads UTF-8 files without a BOM.)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

$junitVersion = '1.10.2'
$junitJar = "lib\junit-platform-console-standalone-$junitVersion.jar"
$junitUrl = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$junitVersion/junit-platform-console-standalone-$junitVersion.jar"

if ($env:JAVA_HOME) {
	$java  = Join-Path $env:JAVA_HOME 'bin\java.exe'
	$javac = Join-Path $env:JAVA_HOME 'bin\javac.exe'
} else {
	$java  = 'java'
	$javac = 'javac'
}

# The JUnit jar is not committed, so download it from Maven Central when missing.
if (-not (Test-Path $junitJar)) {
	New-Item -ItemType Directory -Force lib | Out-Null
	Write-Host "Downloading $junitUrl"
	Invoke-WebRequest -Uri $junitUrl -OutFile $junitJar -UseBasicParsing
}

if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Force out\main, out\test | Out-Null

$mainSources = Get-ChildItem src  -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$testDirs    = @('test', 'tests') | Where-Object { Test-Path $_ }
$testSources = Get-ChildItem $testDirs -Recurse -Filter *.java | ForEach-Object { $_.FullName }

& $javac -encoding UTF-8 -nowarn -d out\main $mainSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& $javac -encoding UTF-8 -d out\test -cp "out\main;$junitJar" $testSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# -ea: the tests under tests/ were written to run with assertions on (see test.sh / test.bat in the root).
& $java '-ea' '-Djava.awt.headless=true' -jar $junitJar execute `
	--class-path 'out\main;out\test' `
	--scan-class-path out\test `
	--details=tree `
	--disable-banner `
	--fail-if-no-tests
exit $LASTEXITCODE
