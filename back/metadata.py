import os
import yt_dlp
from ffmpeg_utils import get_ffmpeg_path


def get_info(url):
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'noplaylist': True,
        'socket_timeout': 15,
        'retries': 3,
    }

    ffmpeg_bin = get_ffmpeg_path()
    if ffmpeg_bin:
        ydl_opts['ffmpeg_location'] = os.path.dirname(ffmpeg_bin)

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)

        if not info:
            raise ValueError("Could not retrieve information for this URL.")

        if info.get('_type') == 'playlist':
            raise ValueError(
                "This looks like a playlist link, not a single track/video. "
                "Please paste a link to an individual song or video."
            )

        duration = info.get('duration')
        if duration is None:
            duration = 0

        uploader = (
            info.get('uploader')
            or info.get('channel')
            or info.get('uploader_id')
            or 'Unknown Uploader'
        )

        bitrate = (
            info.get('abr')
            or info.get('tbr')
            or info.get('vbr')
            or 0
        )
        try:
            bitrate = int(round(float(bitrate)))
        except (ValueError, TypeError):
            bitrate = 0

        return {
            'title': info.get('title') or 'Unknown Title',
            'duration': duration,
            'uploader': uploader,
            'bitrate': bitrate,
        }
