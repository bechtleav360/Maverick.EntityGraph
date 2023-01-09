import youtube_dl
import pandas as pd
from get_video_urls import get_youtube_video_urls

urls = get_youtube_video_urls()

# video-info as entity in node
def video_info(url):
        ydl_opts = {}
        with youtube_dl.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)

            #description = info["description"] # for NLP
            title = info["title"]
            author = info["uploader"]
            duration = info["duration"]
            thumb = info["thumbnail"]
            
            video_information = pd.DataFrame(data= {'Title': [title],
                                                    'Author' : [author],                                              
                                                    'Duration': [duration],
                                                    'Thumbnail' : [thumb]},
                                            index=[url])
            video_information.index.name = 'VideoURL'
            return video_information
