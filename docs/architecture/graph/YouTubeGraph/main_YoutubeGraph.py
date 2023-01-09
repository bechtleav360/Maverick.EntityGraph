import numpy as np
import pandas as pd
from rdflib import Graph, Literal, RDF, URIRef, Namespace
from rdflib.namespace import RDF, RDFS
from get_entities import get_entities

entities_array = get_entities()

# graph initialization
g = Graph()

def fill_graph():
    for i in range(len(entities_array)):
      for j in range(len(entities_array[i])):
        g.add((URIRef(entities_array[i][0] + '/video'), RDF.Property, Literal(entities_array[i][j])))
        
fill_graph()

print(f'Graph g has {len(entities_array)} videos (entities) and {len(g)} video facts')

def check_triples(url):
    if (URIRef(url + '/video'), None, None) in g:
        print(f'Triple exists!')
    else:
        print(f'Triple does not exists!')

    # find all objects an predicates of any subject
    for s, p, o in g.triples((URIRef(url + '/video'), None, None)):
        print(f"{p} is a {o}")

# download graph & get a better plot!--> https://www.ldf.fi/service/rdf-grapher
g.serialize(destination='youtube_video_graph.ttl')