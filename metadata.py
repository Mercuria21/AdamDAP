import yt_dlp

def get_info(url):
    with yt_dlp.YoutubeDL({'quiet': True}) as ydl:
        info = ydl.extract_info(url, download=False)

        return {
            'title': info.get('title', 'Unknown Title'),
            'duration': info.get('duration', 0),
            'uploader': info.get('uploader', 'Unknown Uploader'),
            'bitrate': info.get('abr', 0),
        }