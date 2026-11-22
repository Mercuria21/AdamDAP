import os
import subprocess
from ffmpeg_utils import get_ffmpeg_path


def convert_file(input_file, output_format):
    if not os.path.exists(input_file):
        raise FileNotFoundError(f"Input file not found: {input_file}")

    # Normalize output format (remove leading dot and whitespace)
    output_format = output_format.strip().lstrip('.').lower()

    # If the input file already has the desired extension, skip conversion
    current_ext = os.path.splitext(input_file)[1].lstrip('.').lower()
    if current_ext == output_format:
        return os.path.abspath(input_file)

    ffmpeg_bin = get_ffmpeg_path()
    if not ffmpeg_bin:
        raise RuntimeError(
            "ffmpeg is required for file conversion but was not found on PATH or common installation directories. "
            "Please install ffmpeg or add it to PATH."
        )

    base = os.path.splitext(input_file)[0]
    output_file = f"{base}.{output_format}"

    # -y forces overwrite without hanging on confirmation
    command = [ffmpeg_bin, '-y', '-i', input_file, output_file]

    result = subprocess.run(command, capture_output=True, text=True)
    if result.returncode != 0:
        err_msg = result.stderr.strip() if result.stderr else f"ffmpeg exited with code {result.returncode}"
        raise RuntimeError(f"FFmpeg conversion failed: {err_msg}")

    return os.path.abspath(output_file)
