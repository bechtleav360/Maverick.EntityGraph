# Example 1:
from rdflib import Graph

# Initialize a graph
g = Graph()


# Parse from an external resource
# Parse in an RDF file graph dbpedia
g.parse('http://dbpedia.org/resource/Michael_Jackson')


# Loop through each triple in the graph (subj, pred, obj)
for index, (subj, pred, obj) in enumerate(g):
    print(f"subject (entity = resource) of resource Michael Jackson: {subj} predicate (property = IRI) of resource Michael Jackson: {pred}  object (value = Literal) of resource Michael Jackson: {obj}")
    if index == 10:
        break


# Print the size of the graph
print(f'graph has {len(g)} facts')

# Print the entire graph in the RDF Turtle format (this is the complete version of the michael jackson graph from wikipedia)
print(g.serialize(format='ttl'))

# Example 2
from rdflib import Graph, Literal, RDF, URIRef
from rdflib.namespace import FOAF, XSD


# Generate a Graph
g = Graph()


# Create a node and identify this node by the RDF-URI
# Create an RDF URI node to use as the subject for multiple triples
mason = URIRef("http://example.org/mason")

# Add triples using store's add() method
g.add((mason, RDF.type, FOAF.Person))
g.add((mason, FOAF.nick, Literal("mason", lang="en"))) #Nickname
g.add((mason, FOAF.name, Literal("Mason Carter")))
g.add((mason, FOAF.mbox, URIRef("mailto:mason@example.org")))


# Add another person
shyla = URIRef("http://example.org/shyla")

# Add triples using store's add() method
g.add((shyla, RDF.type, FOAF.Person))
g.add((shyla, FOAF.nick, Literal("shyla", datatype=XSD.string)))  #Nickname  # dtype instead of a language
g.add((shyla, FOAF.name, Literal("Shyla Sharples")))
g.add((shyla, FOAF.mbox, URIRef("mailto:shyla@example.org")))


# Iterate over triples in store and print them out
for s, p, o in g:
    print(s, p, o)


"""Now we want to get our access only the nicknames of the entities, which are of type "person":
Get all the entities which are of thpe "person" (iterate over the subjects) & there u specify the filter
-> this is going to filter all the entities thich are not of the rdf type and the object foaf of person.
Iterate over the list of objects and define another filter, which were the subject is going to be a person from the first loop and the predicate is going to be foaf nickname
=> it brings out all nicknames of the persons"""
# For each foaf:Person in the graph, print out their nickname's value
for person in g.subjects(RDF.type, FOAF.Person):
    for nick in g.objects(person, FOAF.nick):
        print(nick)


# Notation 3 Format
# Bind the FOAF namespace to a prefix for more readable output
g.bind("foaf", FOAF)

# Print all the data in the n3 format
print(g.serialize(format='n3'))