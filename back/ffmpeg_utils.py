import os
import shutil
import sys


def get_ffmpeg_path():
    """
    Finds the ffmpeg executable in:
    1. Bundled directory (if packaged with PyInstaller or placed in app folder)
    2. System PATH
    3. FFMPEG_PATH / FFMPEG_LOCATION environment variables
    4. Common Windows installation locations (Jellyfin, Haruna, Scoop, Chocolatey, WinGet)
    Adds its parent directory to os.environ['PATH'] so child processes can invoke it.
    Returns the absolute path to ffmpeg executable if found, else None.
    """
    # 1. Check application / bundled directories
    app_dirs = []
    if getattr(sys, 'frozen', False):
        if hasattr(sys, '_MEIPASS'):
            app_dirs.append(sys._MEIPASS)
        app_dirs.append(os.path.dirname(sys.executable))
    app_dirs.append(os.path.dirname(os.path.abspath(__file__)))
    app_dirs.append(os.path.abspath('.'))
    app_dirs.append(os.path.abspath('..'))

    for d in app_dirs:
        for name in ('ffmpeg.exe', 'ffmpeg'):
            candidate = os.path.join(d, name)
            if os.path.isfile(candidate):
                _add_to_path(candidate)
                return os.path.abspath(candidate)

    # 2. Check if already on PATH
    found = shutil.which('ffmpeg')
    if found:
        return os.path.abspath(found)

    # 3. Check environment variables
    for env_var in ('FFMPEG_PATH', 'FFMPEG_LOCATION', 'FFMPEG_DIR'):
        val = os.environ.get(env_var)
        if val:
            candidate = val
            if os.path.isdir(candidate):
                candidate = os.path.join(candidate, 'ffmpeg.exe' if sys.platform == 'win32' else 'ffmpeg')
            if os.path.isfile(candidate):
                _add_to_path(candidate)
                return os.path.abspath(candidate)

    # 4. Check well-known locations on Windows
    if sys.platform == 'win32':
        candidates = [
            r'C:\Program Files\Jellyfin\Server\ffmpeg.exe',
            r'C:\Program Files\Haruna\bin\ffmpeg.exe',
            r'C:\ffmpeg\bin\ffmpeg.exe',
            r'C:\ProgramData\chocolatey\bin\ffmpeg.exe',
            os.path.expanduser(r'~\scoop\shims\ffmpeg.exe'),
            os.path.expanduser(r'~\AppData\Local\Microsoft\WinGet\Links\ffmpeg.exe'),
        ]
        for candidate in candidates:
            if os.path.isfile(candidate):
                _add_to_path(candidate)
                return os.path.abspath(candidate)

    return None


def _add_to_path(ffmpeg_exe_path):
    bin_dir = os.path.dirname(os.path.abspath(ffmpeg_exe_path))
    current_path = os.environ.get('PATH', '')
    if bin_dir.lower() not in current_path.lower():
        os.environ['PATH'] = bin_dir + os.pathsep + current_path
