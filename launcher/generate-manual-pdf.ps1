$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")
$sourceHtml = Join-Path $repoRoot "src\main\resources\docs\manual-source.html"
$outputPdf = Join-Path $repoRoot "src\main\resources\docs\manual.pdf"
$tempPdf = Join-Path $repoRoot "build\manual-temp.pdf"
$browserProfile = Join-Path $repoRoot "build\browser-profile-manual"

if (-not (Test-Path $sourceHtml)) {
    throw "Manual source not found: $sourceHtml"
}

$browserCandidates = @(
    "C:\Program Files\Google\Chrome\Application\chrome.exe",
    "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    "C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
)

$browser = $browserCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $browser) {
    throw "Chrome or Edge was not found. Install one of them to generate the PDF manual."
}

$null = New-Item -ItemType Directory -Force -Path $browserProfile
if (Test-Path $tempPdf) {
    Remove-Item $tempPdf -Force
}

$resolvedHtml = (Resolve-Path $sourceHtml).Path
$fileUrl = "file:///" + ($resolvedHtml -replace "\\", "/")

& $browser `
    --headless=new `
    --disable-gpu `
    --disable-crash-reporter `
    --no-first-run `
    --no-default-browser-check `
    --no-sandbox `
    "--user-data-dir=$browserProfile" `
    --no-pdf-header-footer `
    "--print-to-pdf=$tempPdf" `
    $fileUrl

for ($i = 0; $i -lt 20; $i++) {
    if ((Test-Path $tempPdf) -and ((Get-Item $tempPdf).Length -gt 0)) {
        break
    }
    Start-Sleep -Milliseconds 250
}

if (-not (Test-Path $tempPdf)) {
    throw "Expected PDF was not created: $tempPdf"
}

Copy-Item $tempPdf -Destination $outputPdf -Force

Write-Host "PDF manual generated:"
Write-Host "  $outputPdf"
