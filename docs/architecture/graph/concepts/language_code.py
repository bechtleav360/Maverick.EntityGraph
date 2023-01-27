import youtube_dl
from langdetect import detect, DetectorFactory

# language code of the youtube video is required
def get_language_code(url):
    ydl_opts = {
        'quiet': True,
        'skip_download': True,
        'simulate': True,
        'writeinfojson': True
    }
    with youtube_dl.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)
    DetectorFactory.seed = 0
    language_code = detect(info['description'])
    return language_code