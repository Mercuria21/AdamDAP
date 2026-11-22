import glob
import os
import yt_dlp
from ffmpeg_utils import get_ffmpeg_path


def download_video(url, output_format=None):
    os.makedirs('temp', exist_ok=True)

    ffmpeg_bin = get_ffmpeg_path()
    ffmpeg_dir = os.path.dirname(ffmpeg_bin) if ffmpeg_bin else None

    ydl_opts = {
        'outtmpl': os.path.join('temp', '%(title)s.%(ext)s'),
        'noplaylist': True,
        'no_warnings': True,
        'socket_timeout': 15,
        'retries': 3,
    }

    if ffmpeg_dir:
        ydl_opts['ffmpeg_location'] = ffmpeg_dir

    audio_exts = {'mp3', 'wav', 'm4a', 'aac', 'opus'}
    video_exts = {'mp4', 'webm', 'mkv', 'mov'}

    norm_format = output_format.strip().lstrip('.').lower() if output_format else None

    if norm_format and norm_format in audio_exts:
        ydl_opts.update({
            'format': 'bestaudio/best',
            'postprocessors': [{
                'key': 'FFmpegExtractAudio',
                'preferredcodec': norm_format,
                'preferredquality': '192',
            }],
        })
    elif norm_format and norm_format in video_exts:
        if norm_format == 'mp4':
            ydl_opts.update({
                'format': 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
                'merge_output_format': 'mp4',
            })
        elif norm_format == 'webm':
            ydl_opts.update({
                'format': 'bestvideo[ext=webm]+bestaudio[ext=webm]/best[ext=webm]/best',
                'merge_output_format': 'webm',
            })
        else:
            ydl_opts.update({
                'format': 'bestvideo+bestaudio/best',
                'merge_output_format': norm_format,
            })
    else:
        ydl_opts.update({
            'format': 'bestvideo+bestaudio/best',
        })

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        if not info:
            raise ValueError("Failed to retrieve or download media from the specified URL.")
        filename = ydl.prepare_filename(info)

    base = os.path.splitext(filename)[0]

    # 1. If requested format exists (e.g. post-processed by yt-dlp)
    if norm_format and os.path.exists(f"{base}.{norm_format}"):
        return os.path.abspath(f"{base}.{norm_format}")

    # 2. If the filename returned by prepare_filename exists
    if os.path.exists(filename):
        return os.path.abspath(filename)

    # 3. Check requested_downloads metadata from yt-dlp
    if info.get('requested_downloads'):
        for d in info['requested_downloads']:
            filepath = d.get('filepath')
            if filepath and os.path.exists(filepath):
                return os.path.abspath(filepath)

    # 4. Search temp directory for any file matching base
    matches = glob.glob(glob.escape(base) + ".*")
    if matches:
        matches.sort(key=os.path.getmtime, reverse=True)
        return os.path.abspath(matches[0])

    return os.path.abspath(filename)
