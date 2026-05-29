from downloader import download_video
from converter import convert_file

url = input("Enter the video URL: ")
output_format = input("Convert to (mp3, mp4, webm?): ")

print("Downloading video...")
downloaded_file = download_video(url)

print("Converting file...")
output_file = convert_file(downloaded_file, output_format)

print(f"File converted! It is now saved as: {output_file}")