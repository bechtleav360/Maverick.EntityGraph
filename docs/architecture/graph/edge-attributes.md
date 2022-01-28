# Edge attributes

Triple Store is only statements, while Property Graphs allow for attributes on nodes and edges. 

Triple Stores have native support for JSON-LD (since it can be expressed as RDF), with property graphs we need to build 
our own solution.
 
A Knowledge Graph with self-learning rules needs to have the ability, to infer new connections and assign certainty values to it
```

:w1 a eagl:WikipediaEntry ;
    :w1 :inPath :p1
    :w1 :inPath :p2
    :w1 :inPath :p3
```

The entry ``w1``  was in this case manually preassigned to `p1`. User feedback confirmed a recommendation for ``p2``. And 
the model suggests to add it also to ``p3``. 

In a property graph, we would model it as follows (using the Arango Logic)

````
entries/w1
 {
    title: "a title"
 }

inPath/a, from w1, to p1
 {
   certainty: 1
 }

inPath/b, from w1, to p2
 {
   certainty: 0.634323233, 
   confirmedBy: u1233
 }
 
inPath/c, from w1, to p3
 {
   certainty: 0.52211212512
 }
````

## RDF-Star

.. is a concise syntax for reification, aka making statements about statements. In practice it means that we can define 
attributes on edges. 

Example (from https://w3c.github.io/rdf-star/cg-spec/editors_draft.html)
```turtle
PREFIX :    <http://www.example.org/>

:employee38 :familyName "Smith" .
<< :employee38 :jobTitle "Assistant Designer" >> :accordingTo :employee22 .
```


Taking our example from above, we would define it as


```turtle

:w1 a eagl:WikipediaEntry ;
    :title "a title" ; 
    << :w1 :inPath :p1 >> :certainty "1" ;
    << :w1 :inPath :p2 >> :certainty "0.634323233" ;
    << :w1 :inPath :p2 >> :confirmedBy "u1233" ;
    << :w1 :inPath :p3 >> :certainty "0.52211212512" ;
```

## JSON-LD Star

See more examples in the [draft standard](https://json-ld.github.io/json-ld-star/#basic-concepts) (which is fairly new)

```json

{
  "@context": {
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    "eagl": "http://av360.org/schema/eagl#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "sdo": "https://schema.org/"
  },
  "@type": "eagl:WikipediaEntry",
  "sdo:title": "a title", 
  "eagl:inPath": [
    {
      "@id": "p1",
      "@annotation": {
        "certainty": 1
      }
    },
    {
      "@id": "p2",
      "@annotation": {
        "certainty": 0.634323233, 
        "confirmedBy": "u1233"
      }
    },
    {
      "@id": "p3",
      "@annotation": {
        "certainty": 0.52211212512
      }
    }
  ]
}
```

The support in Titanium is still experimental
