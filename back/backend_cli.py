"""
Simple line-based bridge between the Java GUI and the existing Python
download/convert logic. Designed to be called as a subprocess:

    python3 -u backend_cli.py info <url>
    python3 -u backend_cli.py process <url> <format>

Output protocol (one message per line, always flushed immediately):

  info command:
    TITLE:<title>
    DURATION:<seconds>
    UPLOADER:<uploader>
    BITRATE:<kbps>
    DONE
    (or) ERROR:<message>

  process command:
    STATUS:<human readable progress message>   (zero or more)
    FILE:<absolute path to final file>
    (or) ERROR:<message>
"""

import sys
import os

from metadata import get_info
from downloader import download_video
from converter import convert_file


def cmd_info(url):
    try:
        info = get_info(url)
        print(f"TITLE:{info.get('title', 'Unknown Title')}", flush=True)
        print(f"DURATION:{info.get('duration', 0)}", flush=True)
        print(f"UPLOADER:{info.get('uploader', 'Unknown Uploader')}", flush=True)
        bitrate = info.get('bitrate') or 0
        print(f"BITRATE:{bitrate}", flush=True)
        print("DONE", flush=True)
    except Exception as e:
        print(f"ERROR:{e}", flush=True)


def cmd_process(url, output_format):
    try:
        print("STATUS:Downloading video...", flush=True)
        downloaded_file = download_video(url, output_format)

        download_ext = os.path.splitext(downloaded_file)[1].lstrip('.').lower()
        requested = output_format.strip().lower()

        if download_ext == requested:
            print("STATUS:Downloaded file already in requested format; skipping conversion.", flush=True)
            output_file = downloaded_file
        else:
            print("STATUS:Converting file...", flush=True)
            output_file = convert_file(downloaded_file, output_format)

        print(f"FILE:{os.path.abspath(output_file)}", flush=True)
    except Exception as e:
        print(f"ERROR:{e}", flush=True)


def main():
    if len(sys.argv) < 2:
        print("ERROR:No command given", flush=True)
        return

    command = sys.argv[1]

    if command == 'info':
        if len(sys.argv) < 3:
            print("ERROR:Missing URL", flush=True)
            return
        cmd_info(sys.argv[2])
    elif command == 'process':
        if len(sys.argv) < 4:
            print("ERROR:Missing URL or format", flush=True)
            return
        cmd_process(sys.argv[2], sys.argv[3])
    else:
        print(f"ERROR:Unknown command '{command}'", flush=True)


if __name__ == '__main__':
    main()
