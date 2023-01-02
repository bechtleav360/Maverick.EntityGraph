# Schema

Endpoints to manage the schema

---

## List types

`GET /api/types`

Lists all configured types currently in schema database.

*Query Parameters:*

* `prefix` focus only on types defined within the schema identified by the prefix
* `limit`
* `page`

*Response*:
It returns a JSON-LD Graph, such as as the schema.org
example: https://schema.org/version/latest/schemaorg-current-https.ttl

Supported formats are:

- JSON (as JSON-LD)
- Turtle

## Create type

Version: 1

Creates one or more type definitions.

`POST /api/types`

*Payload:*
A valid type definition. The definition can include as many types as needed

The following example is using the Turtle Syntax

```turtle
@prefix cougar.graph.model.rdf: <http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#> .
@prefix mav: <http://av360.org/schema/maverick#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix schema: <https://schema.org/> .

mav:WikipediaEntry a rdfs:Class ;
    rdfs:label "Wikipedia Entry" ;
    rdfs:comment "A wikipedia entry about a certain topic." ;
    rdfs:subClassOf mav:LearningUnit .

mav:hasWikpediaLink a cougar.graph.model.rdf:Property ;
    rdfs:domain mav:WikipediaEntry ;
    rdfs:target schema:Url .

```

and the following using json-ld (as graph)

```json
{
  "@context": {
    "rdfs": "http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#",
    "mav": "http://av360.org/schema/maverick#",
    "xsd": "http://www.w3.org/2001/XMLSchema#"
  },
  "@graph": [
       {
        "@id": "mav:WikipediaEntry",
        "@type": "rdfs:Class",
        "rdfs:comment": "A wikipedia entry about a certain topic.",
        "rdfs:label": "Wikipedia Entry",
        "rdfs:subClassOf": {
            "@id": "mav:LearningUnit"
        }
      }, 
      {
        "@id": "mav:hasWikpediaLink",
        "@type": "rdfs:Property",
        "rdfs:domain": "mav:WikipediaEntry",
        "rdfs:target": "schema:Url"
      }
  ]
}
```

*Possible Errors:*

- ``NamespaceConflict``, if a prefix in the given type definition is already assigned to a different namespace
- ``TypeAlreadyExists``, if the given type already exists within the namespace

## Get type

Version: 1

`GET /api/types/{prefix}.{type}`

`GET /api/types?type={qualified type}`

Returns the schema definition of this particular type

*Possible Errors:*

* `NotFound`

## Update type

Version: 3

`POST /api/types/{prefix}.{type}`

*Notes on conflicts:*

* Schema versioning makes sense. But what are the implications:
    * Adding a version tag to the type name (e.g. `"@type": "mav:WikipediaEntry#1`). Hard to handle and breaks JSON-LD
      Handlers
    * Adding a new prefix: will result in a lot of prefixes, produces just chaos
* But e.g. adding a new (mandatory) value to a type automatically means an invalid schema?
* Making versioning implicit through timestamps, e.g. an entity with last_creation_date before the schema update means
  it is still valid, any new update to the entity will enforce the new rules

## Delete type

Version: 3

`DELETE /api/types/{prefix}.{type}`

is only possible if no entites of this type exist in the graph

Another option would be automatic upcasting of all entities to the more general type of the deleted type.

## Get namespaces

Provides access to the supported schemas identified by namespaces and prefixes

`GET /api/namespaces`

Provides a list of all supported prefixes as valid @Context definitions. Those are automatically determined from the
uploaded types

```json
{
 "@context": {
    "jsonld": "http://www.w3.org/ns/json-ld"
  },
  "@graph": [
      {
        "@Type": "jsonld:PrefixDefinition",
        "jsonld:term": "esco", 
        "jsonld:iri": "http://esco.eu/model"
      }, 
      {
        "@Type": "jsonld:PrefixDefinition",
        "jsonld:term": "mav", 
        "jsonld:iri": "http://av360.io/schema/mav#"
      }
  ]
}
```
