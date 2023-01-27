import os 
import speech_recognition as sr 
from pydub import AudioSegment
from pydub.silence import split_on_silence
import time

from language_code import get_language_code

# create a speech recognition object
r = sr.Recognizer()

def get_large_audio_transcription(path):
    """
    Splitting the large audio file into chunks
    and apply speech recognition on each of these chunks
    """
    file_name, file_ext = os.path.splitext(os.path.basename(path))

    # open the audio file using pydub
    sound = AudioSegment.from_wav(path)  

    # split audio sound where silence is 700 miliseconds or more and get chunks
    chunks = split_on_silence(sound,
        # experiment with this value for your target audio file
        min_silence_len = 500,
        # adjust this per requirement
        silence_thresh = sound.dBFS-14,
        # keep the silence for 1 second, adjustable as well
        keep_silence=500,
    )
    folder_name = "audio-chunks"

    # create a directory to store the audio chunks
    if not os.path.isdir(folder_name):
        os.mkdir(folder_name)
    whole_text = ""

    # process each chunk 
    for i, audio_chunk in enumerate(chunks, start=1):
        # export audio chunk and save it in the `folder_name` directory.
        chunk_filename = os.path.join(folder_name, f"chunk{i}.wav")
        audio_chunk.export(chunk_filename, format="wav")

        with sr.AudioFile(chunk_filename) as source:
            audio_listened = r.record(source)
            
            # try converting it to text
            try:
                text = r.recognize_google(audio_listened, language=get_language_code('https://www.youtube.com/watch?v=' + file_name))
                time.sleep(5)
            except sr.UnknownValueError as e:
                print("Error:", str(e))
            else:
                text = f"{text.capitalize()}. "
                print(chunk_filename, ":", text)
                whole_text += text
    return whole_text


import os
import csv

# path to the folder with WAV files
folder_path = 'data_benchmarks/audios_benchmark/WAV_Data'

# create a list of all WAV files in the folder
wav_files = [f for f in os.listdir(folder_path) if f.endswith('.wav')]

# create an empty list to save the transcriptions and file names
results = []

# iterate over all WAV files in the folder
for wav_file in wav_files:
    file_path = os.path.join(folder_path, wav_file)
    try:
        transcription = get_large_audio_transcription(file_path)
        results.append([wav_file, transcription])
    except Exception as e:
        print(f"error in the transcription of {wav_file}, error: {e}")


import pandas as pd

def save_DataFrame():
        transcriptions_frame = pd.DataFrame(results)
        return transcriptions_frame

transcriptions_frame = save_DataFrame()
transcriptions_frame.to_csv('transcriptions_benchmark.csv', index=False)
