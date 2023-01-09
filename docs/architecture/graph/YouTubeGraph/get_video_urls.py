import pandas as pd

# videos from crawler
def get_youtube_video_urls():
    urls = pd.read_csv('urls.csv')
    urls = urls['0']
    return urls