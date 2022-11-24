"""Navigating RDF Graphs
retrieve the triples
subjects or specific labels of the entities"""

import rdflib
g = rdflib.Graph()
g.parse('http://dbpedia.org/resource/Berlin')


# Iterating over graph
for s, p, o in g:
    print(s, p, o)
    break

# How many triples does the graph have?
print(len(g))

# Check if the triple exists
from rdflib import URIRef

if (s, p, o) in g:
    print(f'Triple exists!')
else:
    print(f'Triple does not exists!')


# Check if the triple exists
from rdflib import URIRef

if (None, None, URIRef('http://dbpedia.org/resource/Protestant')) in g:
    print(f'Triple exists!')
else:
    print(f'Triple does not exists!')



# Get a set of properties
from rdflib.namespace import RDF, RDFS, OWL, FOAF

''' iterate over all triples 
& add all the properties into a list or setin (sets remove all the duplicates)'''

properties1 = set()
for s, p, o in g:
    properties1.add(p)

from pprint import pprint 
print(properties1)
print(len(properties1))

# same thing a little bit easier:
properties2 = set()
for p in g.predicates():
    properties2.add(p)
pprint(properties2)
print(len(properties2))


""" Iterate over list of labels (over the subjects & objects of the graph of the entity "Berlin")
& specifying the filter that we want the property/predicate to be our RDFS label"""

for s, o in g.subject_objects(RDFS.label):
    print(o)
    # print(o.value, o.language, o.datatype)
    #print(type(o))


# Lets check out some other properties (URL's or other databases that have information about the same entity)
for s, o in g.subject_objects(OWL.sameAs):
    print(o)
    #print(type(o))


"""Just specify the object. 
Since we know the graph is about that entity "Berlin", we dont have to specify the subject"""
# Object: Population size of Berlin
population = URIRef('http://dbpedia.org/ontology/populationTotal')
for o in g.objects(None, population):
        print(o)


"""If we dont want to print out the whole URL references, we can define a dbpedia namespace

instead of iterating over the list, we can simply use the g.value function
it returns just a single value for"""
# Get one value from the graph
from rdflib import Namespace

DBO = Namespace('http://dbpedia.org/ontology/') # dbpedia ontology namespace
DBR = Namespace('http://dbpedia.org/resource/') # dbpedia resource

print(g.value(DBR['Berlin'], DBO['populationTotal'], None)) # since we are looking for an object, we specify it as none

