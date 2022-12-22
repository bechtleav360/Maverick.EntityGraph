from rdflib import Graph
from SPARQLWrapper import SPARQLWrapper, JSON, N3
from pprint import pprint

# Wrapper 1. Example
sparql = SPARQLWrapper('https://dbpedia.org/sparql')
sparql.setQuery('''
    SELECT ?object
    WHERE { dbr:Python rdfs:label ?object .}
    # WHERE { dbr:Python dbo:abstract ?object .}
''')

sparql.setReturnFormat(JSON)
qres = sparql.query().convert()
pprint(qres)

for result in qres['results']['bindings']:
    print(result['object'])
    
    lang, value = result['object']['xml:lang'], result['object']['value']
    print(f'Lang: {lang}\tValue: {value}')
    if lang == 'en':
        print(value)

sparql = SPARQLWrapper("http://dbpedia.org/sparql")
sparql.setQuery('''
CONSTRUCT { dbc:Machine_learning skos:broader ?parent .
            dbc:Machine_learning skos:narrower ?child .} 
WHERE {
    { dbc:Machine_learning skos:broader ?parent . }
UNION
    { ?child skos:broader dbc:Machine_learning . }
}''')

sparql.setReturnFormat(N3)
qres = sparql.query().convert()
print(qres)

# Get a List of our queries in a graph
g = Graph()
g.parse(data=qres, format='n3')
print(g.serialize(format='ttl'))


