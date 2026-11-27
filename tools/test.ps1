param(
    [Parameter(Mandatory=$true)][string]$JdkDirectory,
    [Parameter(Mandatory=$true)][string]$EclipsePlugins
)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
Push-Location -LiteralPath $projectDirectory
try {
    $jars = Get-ChildItem -LiteralPath $EclipsePlugins -File | Where-Object {
        $_.Name -match '^(junit-jupiter-(api|engine)|junit-platform-(commons|engine|launcher)|org\.opentest4j|opentest4j|org\.apiguardian\.api|apiguardian-api)[_\-].*\.jar$'
    }
    $classpath = 'build/classes;' + (($jars | ForEach-Object FullName) -join ';')
    New-Item -ItemType Directory -Force -Path build/classes,build/test-classes | Out-Null
    $sources = Get-ChildItem -LiteralPath src -Recurse -Filter '*.java' | ForEach-Object FullName
    & (Join-Path $JdkDirectory 'bin/javac.exe') -encoding UTF-8 --release 8 -Xlint:-options -d build/classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Source compilation failed' }
    $tests = Get-ChildItem -LiteralPath test -Recurse -Filter '*.java' | ForEach-Object FullName
    & (Join-Path $JdkDirectory 'bin/javac.exe') -encoding UTF-8 -cp $classpath -d build/test-classes $tests tools/TestLauncher.java
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
    & (Join-Path $JdkDirectory 'bin/java.exe') '-Djava.awt.headless=true' -cp ("build/test-classes;" + $classpath) TestLauncher
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
} finally { Pop-Location }
