$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")
$stageDir = Join-Path $repoRoot "build\jpackage-input"
$mavenRepoDir = Join-Path $repoRoot "build\.m2\repository"
$distDir = Join-Path $repoRoot "dist"
$appDir = Join-Path $distDir "ODME"
$pomPath = Join-Path $repoRoot "pom.xml"

function Resolve-MavenCommand {
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    if ($env:MAVEN_HOME) {
        $fromMavenHome = Join-Path $env:MAVEN_HOME "bin\mvn.cmd"
        if (Test-Path $fromMavenHome) {
            return $fromMavenHome
        }
    }

    $knownPaths = @(
        "C:\Tools\apache-maven-3.9.14\bin\mvn.cmd",
        "C:\apache-maven-3.9.14\bin\mvn.cmd"
    )

    foreach ($path in $knownPaths) {
        if (Test-Path $path) {
            return $path
        }
    }

    throw "Could not find mvn.cmd. Install Maven or add it to PATH/MAVEN_HOME."
}

[xml]$pom = Get-Content $pomPath
$projectVersion = $pom.project.version
$appVersion = ($projectVersion -replace "-SNAPSHOT$", "")
$mavenCommand = Resolve-MavenCommand

Write-Host "Building application jar..."
if (-not (Test-Path $mavenRepoDir)) {
    New-Item -ItemType Directory -Path $mavenRepoDir -Force | Out-Null
}
Push-Location $repoRoot
try {
    & $mavenCommand -q "-Dmaven.repo.local=$mavenRepoDir" "-Dmaven.test.skip=true" package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

if (-not (Test-Path (Join-Path $repoRoot "target"))) {
    throw "Expected Maven target directory was not created."
}

$targetJar = Get-ChildItem (Join-Path $repoRoot "target\SESEditor-*.jar") |
    Where-Object { $_.Name -notlike "original-*" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not (Test-Path $targetJar)) {
    throw "Expected packaged jar not found under target\SESEditor-*.jar"
}

Write-Host "Preparing jpackage input..."
if (Test-Path $stageDir) {
    Remove-Item $stageDir -Recurse -Force
}
New-Item -ItemType Directory -Path $stageDir | Out-Null
Copy-Item $targetJar.FullName -Destination $stageDir

if (Test-Path $appDir) {
    Remove-Item $appDir -Recurse -Force
}
if (-not (Test-Path $distDir)) {
    New-Item -ItemType Directory -Path $distDir | Out-Null
}

Write-Host "Creating Windows app image..."
& jpackage `
    --type app-image `
    --name ODME `
    --app-version $appVersion `
    --input $stageDir `
    --main-jar $targetJar.Name `
    --main-class odme.odmeeditor.Main `
    --dest $distDir `
    --vendor "DLR SES" `
    --description "Operation Domain Modeling Environment"
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE."
}

$exePath = Join-Path $appDir "ODME.exe"
if (-not (Test-Path $exePath)) {
    throw "Executable was not created: $exePath"
}

Write-Host ""
Write-Host "Executable created:"
Write-Host "  $exePath"
Write-Host ""
Write-Host "Launch it by double-clicking ODME.exe. Keep the whole ODME folder together."
