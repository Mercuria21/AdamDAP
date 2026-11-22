# ==============================================================================
# AdamDAP Build & Packaging Script
# Creates a standalone, portable application package with embedded runtimes.
# ==============================================================================

param(
    [switch]$CreateInstaller
)

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "       AdamDAP App Packaging Script      " -ForegroundColor Cyan
Write-Host "=========================================`n" -ForegroundColor Cyan

function Find-JavaTool([string]$ToolName) {
    # 1. Check if tool is on PATH
    $cmd = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($cmd -and (Test-Path $cmd.Source)) { 
        return $cmd.Source 
    }
    # 2. Check JAVA_HOME
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\$ToolName.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    # 3. Check well-known JDK directories in Program Files
    $jdkSearchPaths = @("C:\Program Files\Java\jdk*", "C:\Program Files\Eclipse Adoptium\jdk*", "C:\Program Files\Amazon Corretto\jdk*")
    foreach ($searchPath in $jdkSearchPaths) {
        $jdkDirs = Get-ChildItem $searchPath -ErrorAction SilentlyContinue | Sort-Object Name -Descending
        foreach ($dir in $jdkDirs) {
            $candidate = Join-Path $dir.FullName "bin\$ToolName.exe"
            if (Test-Path $candidate) { return $candidate }
        }
    }
    return $null
}

# 1. Locate Java Tools
$Javac = Find-JavaTool "javac"
$JarTool = Find-JavaTool "jar"
$JPackage = Find-JavaTool "jpackage"

if (-not $Javac) {
    Write-Error "javac.exe could not be found. Please ensure a JDK (e.g. JDK 17 or 21) is installed."
}
if (-not $JarTool) {
    Write-Error "jar.exe could not be found. Please ensure a JDK is installed."
}

Write-Host "Using Java tools:" -ForegroundColor Gray
Write-Host "  javac:    $Javac" -ForegroundColor Gray
Write-Host "  jar:      $JarTool" -ForegroundColor Gray
if ($JPackage) {
    Write-Host "  jpackage: $JPackage" -ForegroundColor Gray
}

# 2. Locate Python in .venv or system
$Python = ".\.venv\Scripts\python.exe"
if (-not (Test-Path $Python)) {
    $Python = (Get-Command python -ErrorAction SilentlyContinue).Source
    if (-not $Python) {
        Write-Error "Python executable could not be found."
    }
}
Write-Host "  python:   $Python`n" -ForegroundColor Gray

# 3. Ensure PyInstaller is installed
Write-Host "[1/4] Ensuring PyInstaller is installed..." -ForegroundColor Yellow
& $Python -m pip install pyinstaller --quiet

# 4. Build backend_cli.exe using PyInstaller
Write-Host "`n[2/4] Freezing Python backend into backend_cli.exe..." -ForegroundColor Yellow
$BackCliPath = "back/backend_cli.py"
& $Python -m PyInstaller `
    --clean `
    --onefile `
    --console `
    --name backend_cli `
    --distpath "dist_backend" `
    --workpath "build_pyinstaller" `
    --specpath "build_pyinstaller" `
    $BackCliPath

if (-not (Test-Path "dist_backend/backend_cli.exe")) {
    Write-Error "PyInstaller failed to build backend_cli.exe"
}

# 5. Compile Java GUI and build executable JAR
Write-Host "`n[3/4] Compiling Java GUI into AdamDAP.jar..." -ForegroundColor Yellow
$BuildDir = "build_jar"
$ClassesDir = "$BuildDir/classes"

if (Test-Path $BuildDir) { Remove-Item -Recurse -Force $BuildDir }
New-Item -ItemType Directory -Force -Path $ClassesDir | Out-Null

& $Javac -d $ClassesDir front/VideoDownloaderGUI.java

$Manifest = "$BuildDir/MANIFEST.MF"
"Main-Class: VideoDownloaderGUI`n" | Set-Content -Path $Manifest -NoNewline

& $JarTool cfm "$BuildDir/AdamDAP.jar" $Manifest -C $ClassesDir .

# 6. Assemble Release Package
Write-Host "`n[4/4] Assembling release folder..." -ForegroundColor Yellow
$ReleaseDir = "release/AdamDAP-portable"
if (Test-Path $ReleaseDir) { Remove-Item -Recurse -Force $ReleaseDir }
New-Item -ItemType Directory -Force -Path $ReleaseDir | Out-Null

Copy-Item "$BuildDir/AdamDAP.jar" -Destination "$ReleaseDir/AdamDAP.jar"
Copy-Item "dist_backend/backend_cli.exe" -Destination "$ReleaseDir/backend_cli.exe"

# Bundle FFmpeg if found on system
$FfmpegExe = & $Python -c "import sys; sys.path.insert(0, 'back'); import ffmpeg_utils; print(ffmpeg_utils.get_ffmpeg_path() or '')"
if ($FfmpegExe -and (Test-Path $FfmpegExe)) {
    Write-Host "Bundling FFmpeg executable from: $FfmpegExe" -ForegroundColor Green
    Copy-Item $FfmpegExe -Destination "$ReleaseDir/ffmpeg.exe"
}

# Create a clean one-click launcher batch script for the portable edition
@"
@echo off
start javaw -jar "%~dp0AdamDAP.jar"
"@ | Set-Content -Path "$ReleaseDir/AdamDAP.bat"

# Optional: Run jpackage if available to bundle a zero-dependency native app
if ($JPackage) {
    Write-Host "`n[Bonus] Building standalone native Windows app via jpackage..." -ForegroundColor Cyan
    $JpackageInputDir = "release/jpackage_input"
    if (Test-Path $JpackageInputDir) { Remove-Item -Recurse -Force $JpackageInputDir }
    New-Item -ItemType Directory -Force -Path $JpackageInputDir | Out-Null

    Copy-Item "$BuildDir/AdamDAP.jar" -Destination "$JpackageInputDir/AdamDAP.jar"

    $TargetType = if ($CreateInstaller) { "exe" } else { "app-image" }
    $JpackageDest = "release/standalone"
    $TargetAppFolder = "$JpackageDest/AdamDAP"
    if (Test-Path $TargetAppFolder) { Remove-Item -Recurse -Force $TargetAppFolder }
    New-Item -ItemType Directory -Force -Path $JpackageDest | Out-Null

    & $JPackage `
        --type $TargetType `
        --name "AdamDAP" `
        --input $JpackageInputDir `
        --main-jar "AdamDAP.jar" `
        --main-class "VideoDownloaderGUI" `
        --dest $JpackageDest

    # If app-image, copy backend_cli.exe and ffmpeg into the bundled folder
    if (Test-Path $TargetAppFolder) {
        Copy-Item "$ReleaseDir/backend_cli.exe" -Destination "$TargetAppFolder/backend_cli.exe"
        if (Test-Path "$ReleaseDir/ffmpeg.exe") {
            Copy-Item "$ReleaseDir/ffmpeg.exe" -Destination "$TargetAppFolder/ffmpeg.exe"
        }
        Write-Host "Native zero-dependency app created at: $TargetAppFolder/AdamDAP.exe" -ForegroundColor Green
    }
}

Write-Host "`n=========================================" -ForegroundColor Green
Write-Host " Packaging Complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host "Portable package: $ReleaseDir"
if (Test-Path "release/standalone/AdamDAP") {
    Write-Host "Native standalone app: release/standalone/AdamDAP"
}
