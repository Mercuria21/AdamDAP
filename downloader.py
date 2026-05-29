import yt_dlp

def download_video(url):
    ydl_opts = {
        'format': 'bestvideo + bestaudio/best',
        'outtmpl': 'temp/%(title)s.%(ext)s',
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        filename = ydl.prepare_filename(info)

    return filename