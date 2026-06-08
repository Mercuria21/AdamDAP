import os
import yt_dlp


def download_video(url, output_format=None):
    os.makedirs('temp', exist_ok=True)

    ydl_opts = {
        'outtmpl': 'temp/%(title)s.%(ext)s',
    }

    audio_exts = {'mp3', 'wav', 'm4a', 'aac', 'opus'}
    video_exts = {'mp4', 'webm', 'mkv', 'mov'}

    if output_format and output_format.lower() in audio_exts:
        ydl_opts.update({
            'format': 'bestaudio/best',
            'postprocessors': [{
                'key': 'FFmpegExtractAudio',
                'preferredcodec': output_format.lower(),
                'preferredquality': '192',
            }],
        })
    elif output_format and output_format.lower() in video_exts:
        of = output_format.lower()
        if of == 'mp4':
            ydl_opts.update({
                'format': 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
                'merge_output_format': 'mp4',
            })
        elif of == 'webm':
            ydl_opts.update({
                'format': 'bestvideo[ext=webm]+bestaudio[ext=webm]/best[ext=webm]/best',
                'merge_output_format': 'webm',
            })
        else:
            ydl_opts.update({
                'format': 'bestvideo+bestaudio/best',
                'merge_output_format': of,
            })
    else:
        ydl_opts.update({
            'format': 'bestvideo+bestaudio/best',
        })

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        filename = ydl.prepare_filename(info)

    # Adjust filename extension if postprocessor changed it
    base = os.path.splitext(filename)[0]
    if output_format:
        filename = f"{base}.{output_format.lower()}"

    return filename