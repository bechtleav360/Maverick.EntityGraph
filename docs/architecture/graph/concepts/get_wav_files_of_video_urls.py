import numpy as np
import pandas as pd
from pytube import YouTube
from pytube.exceptions import VideoUnavailable
from pydub import AudioSegment

# videos from crawler
def get_youtube_video_urls():
    urls = pd.read_csv(r'data_benchmarks/urls.csv')
    urls = urls['0']
    return urls

urls = get_youtube_video_urls()
print(f'we work with {len(urls)} videos')

def download_audio(url_list):
    # get audio-mp4
    for url in url_list:
      try:
          selected_video = YouTube(url)
          video_id = selected_video.video_id
          audio = selected_video.streams.filter(only_audio=True, file_extension='mp4').first()
          audio.download(filename=f'{video_id}.mp4', output_path='data_benchmarks/audios_benchmark/MP4_Data')

          # convert to audio-wave
          sound = AudioSegment.from_file(f'data_benchmarks/audios_benchmark/MP4_Data/{video_id}.mp4', format="mp4")
          sound.export(f'data_benchmarks/audios_benchmark\WAV_Data/{video_id}.wav', format="wav")
      except VideoUnavailable:
          print(f"Video {url} is unavailable.\n skip")

# ex. for the first 3 urls:
example_urls = np.array(urls[:3])
download_audio(example_urls)