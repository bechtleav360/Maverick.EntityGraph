import pandas as pd
import numpy as np

def get_entities():
        entities = pd.read_csv('video_infos.csv')
        entities_array = np.array(entities)
        return entities_array

