# ==============================================================================
# AdamDAP Build & Packaging Script
# Creates a standalone, portable application package with embedded runtimes.
# Automatically cleans up all intermediate build artifacts, keeping only release/.
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
    $jdkSearchPaths = @(
        "C:\Program Files\Java\jdk*", 
        "C:\Program Files\Eclipse Adoptium\jdk*", 
        "C:\Program Files\Amazon Corretto\jdk*"
    )
    foreach ($searchPath in $jdkSearchPaths) {
        $jdkDirs = Get-ChildItem $searchPath -ErrorAction SilentlyContinue | Sort-Object Name -Descending
        foreach ($dir in $jdkDirs) {
            $candidate = Join-Path $dir.FullName "bin\$ToolName.exe"
            if (Test-Path $candidate) { return $candidate }
        }
    }
    return $null
}

function Safe-RemovePath([string]$PathToRemove) {
    if (Test-Path $PathToRemove) {
        Get-ChildItem -Path $PathToRemove -Recurse -Force -ErrorAction SilentlyContinue | ForEach-Object {
            if ($_.IsReadOnly) { $_.IsReadOnly = $false }
        }
        Remove-Item -Recurse -Force $PathToRemove -ErrorAction SilentlyContinue
    }
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

# Define paths for intermediate build artifacts
$BuildJarDir = "build_jar"
$BuildPyinstallerDir = "build_pyinstaller"
$DistBackendDir = "dist_backend"
$JpackageInputDir = "release/jpackage_input"

try {
    # 3. Ensure PyInstaller is installed
    Write-Host "[1/5] Ensuring PyInstaller is installed..." -ForegroundColor Yellow
    & $Python -m pip install pyinstaller --quiet

    # 4. Build backend_cli.exe using PyInstaller
    Write-Host "`n[2/5] Freezing Python backend into backend_cli.exe..." -ForegroundColor Yellow
    $BackCliPath = "back/backend_cli.py"
    & $Python -m PyInstaller `
        --clean `
        --onefile `
        --console `
        --name backend_cli `
        --distpath $DistBackendDir `
        --workpath $BuildPyinstallerDir `
        --specpath $BuildPyinstallerDir `
        $BackCliPath

    if (-not (Test-Path "$DistBackendDir/backend_cli.exe")) {
        Write-Error "PyInstaller failed to build backend_cli.exe"
    }

    # 5. Compile Java GUI and build executable JAR
    Write-Host "`n[3/5] Compiling Java GUI into AdamDAP.jar..." -ForegroundColor Yellow
    $ClassesDir = "$BuildJarDir/classes"

    Safe-RemovePath $BuildJarDir
    New-Item -ItemType Directory -Force -Path $ClassesDir | Out-Null

    & $Javac -d $ClassesDir front/VideoDownloaderGUI.java

    $Manifest = "$BuildJarDir/MANIFEST.MF"
    "Main-Class: VideoDownloaderGUI`n" | Set-Content -Path $Manifest -NoNewline

    & $JarTool cfm "$BuildJarDir/AdamDAP.jar" $Manifest -C $ClassesDir .

    # 6. Assemble Portable Package
    Write-Host "`n[4/5] Assembling release packages..." -ForegroundColor Yellow
    $PortableDir = "release/AdamDAP-portable"
    Safe-RemovePath $PortableDir
    New-Item -ItemType Directory -Force -Path $PortableDir | Out-Null

    Copy-Item "$BuildJarDir/AdamDAP.jar" -Destination "$PortableDir/AdamDAP.jar" -Force
    Copy-Item "$DistBackendDir/backend_cli.exe" -Destination "$PortableDir/backend_cli.exe" -Force

    # Bundle FFmpeg if found on system
    $FfmpegExe = & $Python -c "import sys; sys.path.insert(0, 'back'); import ffmpeg_utils; print(ffmpeg_utils.get_ffmpeg_path() or '')"
    if ($FfmpegExe -and (Test-Path $FfmpegExe)) {
        Write-Host "Bundling FFmpeg executable from: $FfmpegExe" -ForegroundColor Green
        Copy-Item $FfmpegExe -Destination "$PortableDir/ffmpeg.exe" -Force
    }

    # Create a clean one-click launcher batch script for the portable edition
    @"
@echo off
start javaw -jar "%~dp0AdamDAP.jar"
"@ | Set-Content -Path "$PortableDir/AdamDAP.bat"

    # 7. Build zero-dependency native Windows app via jpackage
    if ($JPackage) {
        Write-Host "`nBuilding standalone native Windows app via jpackage..." -ForegroundColor Cyan
        Safe-RemovePath $JpackageInputDir
        New-Item -ItemType Directory -Force -Path $JpackageInputDir | Out-Null

        Copy-Item "$BuildJarDir/AdamDAP.jar" -Destination "$JpackageInputDir/AdamDAP.jar" -Force

        $TargetType = if ($CreateInstaller) { "exe" } else { "app-image" }
        $JpackageDest = "release/standalone"
        $TargetAppFolder = "$JpackageDest/AdamDAP"
        Safe-RemovePath $TargetAppFolder
        New-Item -ItemType Directory -Force -Path $JpackageDest | Out-Null

        & $JPackage `
            --type $TargetType `
            --name "AdamDAP" `
            --input $JpackageInputDir `
            --main-jar "AdamDAP.jar" `
            --main-class "VideoDownloaderGUI" `
            --dest $JpackageDest

        if (Test-Path $TargetAppFolder) {
            Copy-Item "$DistBackendDir/backend_cli.exe" -Destination "$TargetAppFolder/backend_cli.exe" -Force
            if ($FfmpegExe -and (Test-Path $FfmpegExe)) {
                Copy-Item $FfmpegExe -Destination "$TargetAppFolder/ffmpeg.exe" -Force
            }
            Write-Host "Native zero-dependency app created at: $TargetAppFolder/AdamDAP.exe" -ForegroundColor Green
        }
    }
}
finally {
    # 8. Cleanup Intermediate Build Artifacts (Keep ONLY the release/ folder)
    Write-Host "`n[5/5] Cleaning up intermediate build artifacts..." -ForegroundColor Yellow

    Safe-RemovePath $BuildJarDir
    Safe-RemovePath $BuildPyinstallerDir
    Safe-RemovePath $DistBackendDir
    Safe-RemovePath $JpackageInputDir
    Safe-RemovePath "build"
    Safe-RemovePath "back/__pycache__"

    Get-ChildItem -Path . -Filter "*.spec" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
    Get-ChildItem -Path "front" -Filter "*.class" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
}

Write-Host "`n=========================================" -ForegroundColor Green
Write-Host " Packaging & Cleanup Complete!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host "Outputs kept in release/:"
if (Test-Path "release/standalone/AdamDAP") {
    Write-Host "  -> Standalone App: release/standalone/AdamDAP/AdamDAP.exe" -ForegroundColor Green
}
if (Test-Path "release/AdamDAP-portable") {
    Write-Host "  -> Portable App:   release/AdamDAP-portable/" -ForegroundColor Green
}
