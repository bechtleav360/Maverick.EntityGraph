"""Creating RDF Triples
RDF allows us to make statements about resources. A statement always has the following structure:
<subject> <predicate> <object> An RDF statement expresses a relationship between two resources. The subject and the object represent the two resources being related;

the predicate represents the nature of their relationship (a property)
the relationship (property) is phrased in a directional way (from subject to object)"""

"""Creating Nodes
The subjects and objects of the triples make up the nodes in the graph

they are URI (Uniform Resource Identifier) references, Blank nodes or Literals
In RDFlib, these node types are represented by the classes URIRef, BNode, Literal
URIRefs & Bnnodes can both be thought of as resources (persons, compaies, websites, etc.)
A BNode is a node where the exact URI is not known
A UIRef is a node where the exact URI is known. They are also used to represent the properties/predicates in the RDF graph
Literals represent attribute values (name, date, number, etc). The most common literal values are XML data types (string, int,...)"""

"""(Informal) Representation of a Graph: https://miro.com/app/board/uXjVPUPbFlE=/

<Bob> <is a> <person>
<Bob> <is a friend of> <Alice>
<Bob> <is born on> <the 4th of July 1990>
<Bob> <is interested in> <the Mona Lisa>
<the Mona Lisa> <was created by> <Leonardo da Vinci>
<the video 'La Joconde a Washington> <is about> <the Mona Lisa>

1. We want to define the node: since we know their exact URI, we use the URIRef class to crate the node just by passing the URI of the entity"""

from rdflib import URIRef, BNode, Literal, Namespace
from rdflib.namespace import FOAF, DCTERMS, XSD, RDF, SDO

mona_lisa = URIRef('http://www.wikidata.org/entity/Q12418')
davinci = URIRef('http://dbpedia.org/resource/Leonardo_da_Vinci')
lajoconde = URIRef('http://data.europeana.eu/item/04802/243FA8618938F4117025F17A8B813C5F9AA4D619')

EX = Namespace('http://example.org/')
bob = EX['Bob']
alice = EX['Alice']

birth_date = Literal("1990-07-04", datatype=XSD['date'])
title = Literal('Mona Lisa', lang='en')

print(title.value, title.language)
print(title)


# let's start creating the graph and adding all the triples inside it
from rdflib import Graph
g = Graph() # initialize the graph-instance


# add sets of triples to the graph:
g.add((bob, RDF.type, FOAF.Person))
g.add((bob, FOAF.knows, alice))
g.add((bob, FOAF['topic_interest'], mona_lisa))
g.add((bob, SDO['birthDate'], birth_date))
g.add((mona_lisa, DCTERMS['creator'], davinci))
g.add((mona_lisa, DCTERMS['title'], title))
g.add((lajoconde, DCTERMS['subject'], mona_lisa))

# Take a look at the graph:
print(g.serialize(format='ttl'))


# Let'let's give the namespaces proper prefixes!!
# Bind prefix to namespace
g.bind('ex', EX)
g.bind('foaf', FOAF)
g.bind('schema', SDO)
g.bind('dcterms', DCTERMS)


"""we can simply inspect all the prefixes and all the namespaces that are part of the graph
the namespace is the first part of an IRI (Internationalized Resource Identifier)
shared by several resources"""
for prefix, nspace in g.namespaces():
  print(nspace)


# Replace a Literal value
g.set((bob, SDO['birthDate'], Literal('1991-01-01', datatype=XSD.date))) # changed bday of bob
g.set((mona_lisa, DCTERMS['title'], Literal('La Joconde', lang='fr'))) # changed the language of the title

print(g.serialize(format='ttl'))


# Remove triples from graph
g.remove((lajoconde, None, None))

print(g.serialize(format='ttl'))