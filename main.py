import sys
import os
from downloader import download_video
from converter import convert_file
from metadata import get_info


def run_once():
    url = input("Enter the video URL: ")
    output_format = input("Convert to (mp3, mp4, webm?): ")

    info = get_info(url)
    print(f"Title: {info['title']}")
    print(f"Duration: {info['duration']} seconds")
    print(f"Uploader: {info['uploader']}")
    print(f"Bitrate: {info['bitrate']} kbps")
    print(f"Requested output format: {output_format}")

    # Ask user to confirm before proceeding
    while True:
        choice = input("Continue? (y/n): ").strip().lower()
        if choice == 'y':
            break
        elif choice == 'n':
            print("Cancelled by user.")
            # Return False to indicate the run was cancelled and the
            # outer loop should restart at the URL prompt immediately.
            return False
        else:
            print("Please enter 'y' or 'n'.")

    print("Downloading video...")
    downloaded_file = download_video(url, output_format)

    download_ext = os.path.splitext(downloaded_file)[1].lstrip('.').lower()
    requested = output_format.strip().lower()
    if download_ext == requested:
        print("Downloaded file already in requested format; skipping conversion.")
        output_file = downloaded_file
    else:
        print("Converting file...")
        output_file = convert_file(downloaded_file, output_format)

    print(f"File converted! It is now saved as: {output_file}")

    # Return True to indicate a successful run (download/convert completed)
    return True


if __name__ == '__main__':
    while True:
        completed = run_once()
        if not completed:
            # User cancelled; immediately restart at the URL prompt
            continue
        # Ask whether to run again after a successful download/convert
        while True:
            choice = input("Do you wish to download another video? (y/n): ").strip().lower()
            if choice == 'y':
                break  # outer loop will continue
            elif choice == 'n':
                print("See you next time")
                sys.exit(0)
            else:
                print("Please enter 'y' or 'n'.")