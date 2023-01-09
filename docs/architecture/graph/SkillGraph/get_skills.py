import numpy as np
import pandas as pd

def get_esco_skills():
    skills_frame = pd.read_csv('skills_de.csv')
    skills = np.array(skills_frame['preferredLabel'])
    skillType = np.array(skills_frame['skillType'])
    conceptURI = np.array(skills_frame['conceptUri'])
    resueLevel = np.array(skills_frame['reuseLevel'])
    print(f'we have a list of {len(skills)} skills/competences')
    return skills_frame, skills, skillType, conceptURI, resueLevel
