import yt_dlp

def get_info(url):
    
    ydl_opts = {
        'quiet': True,
        'noplaylist': True,
        'socket_timeout': 15,
        'retries': 3,
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)

        if info.get('_type') == 'playlist':
            raise ValueError(
                "This looks like a playlist link, not a single track/video. "
                "Please paste a link to an individual song or video."
        )


        return {
            'title': info.get('title', 'Unknown Title'),
            'duration': info.get('duration', 0),
            'uploader': info.get('uploader', 'Unknown Uploader'),
            'bitrate': info.get('abr', 0),
        }