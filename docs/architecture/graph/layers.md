# Layers

Conceptually, we distinguish between certain perspectives on the graph:

* the *schema* layer, which stores our domain model
* the immutable *entities* layer (or data, or documents, or whatever): the baseline, storing the facts. The facts are either stored
  only in the graph, or are shadow copies from a remote third-party application through connectors. Standard reasoning
  and business rules is used to alter the state of this layer.
* The mutable *assertions* layer with proposed modifications for the entities. These can either be
  *  relations which link entities using the feedback from the users. 
  *  new properties (also overwriting existing properties on the entities layer)
* the *recommendation* layer, which stores assertions about edges and values. The assertions are either coming from user
  feedback or from ML models.


### To be discussed
* the *archive* layer, which serves as sink for entities not in use anymore
* the *trust* layer, which manages the results of the integrity checks and proofs of authenticity.
* the *versions* layer, which manages access to historic versions of the entities (only for updates on the entity layer)

The layers are implemented as independent graphs to support separations of concern and individual instances (for
scaling). Pointers accross the graphs are handled through the IDs

The following example shows a combination of all layers.

```turtle
@prefix cougar.graph.model.rdf: <http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#> .
@prefix entities: <http://example.org/schema/entities#> .
@prefix versions: <http://example.org/schema/versions#> .
@prefix annot: <http://example.org/schema/annotations#> .
@prefix arch: <http://example.org/schema/archived#> .
@prefix sec: <https://w3id.org/security#> .
@prefix dc: <http://purl.org/dc/terms/> .
@prefix base: <http://example.org/> .

base:w142 a entities:WikipediaEntry ;
    dc:title "a title" ; 
    versions:previousVersion base:w141 ;
    sec:proof base:b4 ;
    << :w142 entities:inPath :p2 >> annot:certainty "0.634323233" ;
    << :w142 entities:inPath :p2 >> annot:confirmedBy "u1233" ;
    arch:archived false .


base:b4 a ns2:RsaSignature2018 ;
  dc:created "2022-01-27T09:28:44Z"^^xsd:dateTime ;
  dc:creator <https://av360.io> ;
  sec:domain "av360.io" ;
  sec:jws "eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19.." ;
  sec:nonce "456dbce6" .

```

As we can see in the example, we separate the layers by cougar.graph.model.vocabulary (identified by the prefix). Since
every line in the example is an atomic statement in the form `<subject, predicate, object>`, the can be stored
independently. Combining them all into on entity representation is the combination of different SPARL Queries from each
layer.

Taking the example from above, we would need the following query

*Get the entity*

```

PREFIX cougar.graph.model.rdf:     <http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#>
PREFIX rdfs:    <http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#>
PREFIX entities: <http://example.org/schema/entities#>

SELECT * WHERE { 
   ?s cougar.graph.model.rdf:type entities:WikipediaEntry .
   ?s ?p ?o
}
```

## Shadowed entities

not relevant yet

## Verification of integrity and authenticity

not relevant yet

## User feedback and classifications
