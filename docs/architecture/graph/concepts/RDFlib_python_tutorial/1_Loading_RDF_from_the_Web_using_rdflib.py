# Loading & Saving
import rdflib

# Initialize a graph
g = rdflib.Graph()


# Parse in an RDF file graph the web
g.parse('http://dbpedia.org/resource/Python_(programming_language)')
print(len(g))

h = rdflib.Graph()

from rdflib import Graph, Literal, RDF, URIRef
from rdflib.namespace import FOAF, XSD

# Create an RDF URI node to use as the subject for multiple triples
mason = URIRef("http://example.org/mason")

# Add triples using store's add() method
h.add((mason, RDF.type, FOAF.Person))
h.add((mason, FOAF.nick, Literal("mason", lang="en"))) #Nickname
h.add((mason, FOAF.name, Literal("Mason Carter")))
h.add((mason, FOAF.mbox, URIRef("mailto:mason@example.org")))

print(f'Graph h has {len(h)} facts')
print(h.serialize())

for triples in h:
    print(triples)