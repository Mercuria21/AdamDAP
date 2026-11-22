import os
import sys

# Ensure stdout and stderr use UTF-8 on Windows
if sys.platform == 'win32':
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    if hasattr(sys.stderr, 'reconfigure'):
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')

from downloader import download_video
from converter import convert_file
from metadata import get_info


def format_duration(seconds):
    try:
        sec = int(seconds)
        if sec <= 0:
            return "Unknown / Live"
        h, rem = divmod(sec, 3600)
        m, s = divmod(rem, 60)
        if h > 0:
            return f"{h}:{m:02d}:{s:02d} ({sec} seconds)"
        return f"{m}:{s:02d} ({sec} seconds)"
    except (ValueError, TypeError):
        return "Unknown"


def run_once():
    try:
        url = input("Enter the video URL: ").strip()
    except (KeyboardInterrupt, EOFError):
        print("\nExiting.")
        sys.exit(0)

    if not url:
        print("URL cannot be empty.")
        return False

    try:
        output_format = input("Convert to (mp3, mp4, webm, mkv, wav... [default: mp4]): ").strip().lower()
    except (KeyboardInterrupt, EOFError):
        print("\nExiting.")
        sys.exit(0)

    if not output_format:
        output_format = 'mp4'

    output_format = output_format.lstrip('.')

    print("Fetching video info...")
    try:
        info = get_info(url)
    except Exception as e:
        print(f"Error retrieving info: {e}")
        return False

    print(f"Title: {info['title']}")
    print(f"Duration: {format_duration(info['duration'])}")
    print(f"Uploader: {info['uploader']}")
    print(f"Bitrate: {info['bitrate']} kbps")
    print(f"Requested output format: {output_format}")

    # Ask user to confirm before proceeding
    while True:
        try:
            choice = input("Continue? (y/n): ").strip().lower()
        except (KeyboardInterrupt, EOFError):
            print("\nCancelled.")
            return False
        if choice == 'y':
            break
        elif choice == 'n':
            print("Cancelled by user.")
            return False
        else:
            print("Please enter 'y' or 'n'.")

    print("Downloading media...")
    try:
        downloaded_file = download_video(url, output_format)
    except Exception as e:
        print(f"Download failed: {e}")
        return False

    download_ext = os.path.splitext(downloaded_file)[1].lstrip('.').lower()
    requested = output_format.strip().lower()

    if download_ext == requested:
        print("Downloaded file already in requested format; skipping conversion.")
        output_file = downloaded_file
    else:
        print(f"Converting file from {download_ext} to {requested}...")
        try:
            output_file = convert_file(downloaded_file, output_format)
        except Exception as e:
            print(f"Conversion failed: {e}")
            return False

    print(f"File ready! Saved as: {output_file}")
    return True


if __name__ == '__main__':
    while True:
        completed = run_once()
        if not completed:
            continue

        while True:
            try:
                choice = input("Do you wish to download another video? (y/n): ").strip().lower()
            except (KeyboardInterrupt, EOFError):
                print("\nSee you next time")
                sys.exit(0)
            if choice == 'y':
                break
            elif choice == 'n':
                print("See you next time")
                sys.exit(0)
            else:
                print("Please enter 'y' or 'n'.")
