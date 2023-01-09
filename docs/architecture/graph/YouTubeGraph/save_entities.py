import numpy as np
import pandas as pd
import time
from get_video_urls import get_youtube_video_urls
from get_infos import video_info


urls = get_youtube_video_urls()
print(f'we work with {len(urls)} videos')

infos = []
for url in urls:
  try:
    info = video_info(url)
    infos.append(info)
    # Delay the next request by 5 seconds
    time.sleep(5)
  except:
    print('error')
    continue

all_infos = pd.concat(infos, axis=0)
all_infos.to_csv('video_infos.csv')

