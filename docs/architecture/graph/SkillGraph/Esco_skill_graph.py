import numpy as np
import pandas as pd
from rdflib import Graph, Literal, RDF, URIRef, BNode, Namespace
from rdflib.namespace import FOAF, DCTERMS as dc, XSD, RDF, SDO, SKOS as skos, RDFS
from get_skills import get_esco_skills

skills_frame, skills, skillType, conceptURI, resueLevel = get_esco_skills()

# create graph
g = Graph()

# fill graph
def fill_graph():
    for i in range(len(skills)):
      g.add((URIRef(conceptURI[i] + '/skill'), RDF.Property, Literal(skills[i])))

fill_graph()

# get graph & triples (s,p,o)
def get_triples():
    print('Graph g:\n', g.serialize(format='ttl'))
    for triples in g:
        print(f'triples{triples}')


print(f'Graph g has {len(g)} facts')

# save the graph
#g.serialize(destination='esco_skill_graph.ttl')