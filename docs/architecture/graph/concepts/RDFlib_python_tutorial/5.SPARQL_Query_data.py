from rdflib import Graph, URIRef
from rdflib.namespace import RDFS, SKOS

g = Graph()
g.parse('https://www.wikidata.org/wiki/Special:EntityData/Q2831.ttl')
print(f'we have {len(g)} facts in our graph')

MJ = URIRef('http://www.wikidata.org/entity/Q2831')
for label in g.objects(MJ, SKOS.altLabel):
    print(f'value:{label.value}, language:{label.language}')


# make query
qres = g.query('''
    SELECT ?label
    WHERE {
        wd:Q2831 skos:altLabel ?label .
    }
''')
for label, *_ in qres:
    print(f'value:{label.value}, language:{label.language}')

# order by label
qres = g.query('''
    SELECT DISTINCT ?label
    WHERE {
        wd:Q2831 rdfs:label | skos:prefLabel | skos:altLabel ?label .
        FILTER (lang(?label) = 'en')
    }
    ORDER BY ?label
''')
for label, *_ in qres:
    print(f'label:{label}')

# look for label & description
qres = g.query('''
    SELECT ?label ?description
    WHERE {
        wd:Q2831 wdt:P166 ?award .
        ?award rdfs:label ?label .
        FILTER (lang(?label) = 'en')
        OPTIONAL {
            ?award schema:description ?description
            FILTER (lang(?description) = 'en')
        }
    }
''')

for label, description in qres:
    print(f'Award: {label:<55} Description: {description}')
