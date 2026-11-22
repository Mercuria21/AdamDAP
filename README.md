# AdamDAP

A media downloader created by me, Mercuria21 for my friend ValetParkings (Adam Haziq). Happy Birthday!

## Requirements
- Python 3.10+
- Java 17+ (for GUI)
- FFmpeg (automatically detected if installed via PATH, Jellyfin, Scoop, Chocolatey, etc.)

## Setup
1. Create and activate a virtual environment:
   ```bash
   python -m venv .venv
   # Windows PowerShell:
   .\.venv\Scripts\Activate.ps1
   # Linux/macOS:
   source .venv/bin/activate
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Running

### Java GUI
```bash
javac front/VideoDownloaderGUI.java
java -cp front VideoDownloaderGUI
```

### CLI
```bash
python back/main.py
```
