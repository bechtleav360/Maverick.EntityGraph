# Entities
Managing the entity resources 


## Summary by format

### Turtle / JSON-LD
* ``POST /api/rs`` (Create entity) /v1
* ``POST /api/rs/{id}/{prefix.key}`` (Create value or relation) /v1
* ``PUT /api/rs/{id}/{prefix.key}`` (Create annotations on edge) /v1
* ``POST /api/rs/{id}/{prefix.key}/{id}`` (Create edge to existing entity) /v1


* ``DELETE /api/rs/{id}/{prefix.key}`` (Delete value or relation) /v2
* ``GET /api/rs/{id}/{prefix.key}`` (Reads value or embedded object) /v2
* ``DELETE /api/rs/{id}`` (Delete entity) /v2
* ``GET /api/rs/{prefix.type}`` (List entities)  /v2
* ``GET /api/rs/{id}`` (Read entity) /v2

* ``GET /api/rs/{prefix.type}/{id}`` (Read entity with type coercion) /v3
* ``PUT /api/rs/{id}`` (Patch entity) /v3
* ``PUT /api/rs/{id}/{prefix.key}`` (Update value) /v3

* ``POST /api/rs/{id}`` (Read entity by example) /v5


### JSON+HATEOAS Mode 
* ``GET /api/rs/{id}`` (Read entity) /v7
* ``GET /api/rs/{prefix.type}/{id}`` (Read entity with type coercion) /v7
* ``POST /api/rs/{prefix.type}`` (Create entities) /v7
* ``GET /api/rs/{prefix.type}`` (List entities) /v7
* ``DELETE /api/rs/{prefix.type}/{id}`` (Delete entity) /v7
* ``PUT /api/rs/{prefix.type}/{id}`` (Patch entity) /v7
* 
* ``GET /api/rs/{prefix.type}/{id}/{key}`` (Get value/s) /v7
* ``PUT /api/rs/{prefix.type}/{id}/{prefix.key}`` (Create annotations on edge) /v7



---
## Get entity by Id

Returns an individual item


``GET /api/entities/{id}``


*Supported query parameters:*

* ``proof``: whether to include a proof (signature)  /v5
* ``frame``: use a named template to construct a specific view on the entity


* ``GET /api/rs/{prefix.type}/{id}``

performs the same request with type coercion (or casting) of the main entity type. 

## Create entity

``POST /api/entities``


Payload has to be either 
* a valid json-ld document with a @type and ideally @id (could be automatically generated)
* a set of turtle statements with an entity of a type (if object identifier is anonymous, it will be transformed to a valid id)

```json
{
  "@context": {
    "cougar.graph.model.rdf": "http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#",
    "eagl": "http://av360.org/schema/eagl#",
    "xsd": "http://www.w3.org/2001/XMLSchema#", 
    "sdo": "https://schema.org/"

  },
  "@graph": [
    {
      "@type": "eagl:WikipediaEntry",
      "additionalAttribute": "",
      "eagl:hasWikipediaLink": {
        "@type": "sdo:url",
        "url": "http://en.wikipedia.org/..",
        "language": "en-US"
      }
    }
  ]
}
```

If it includes a proof, it will be verified (and should signed in a credential chain)


## Partial updates
Partial updates are crucial in the graph, and natively supported.


### Adding an embedded object

``POST /api/entities/{id}/{prefix.key}``

Scenario: adding a new wikipedia link in a different language

Conflicts can arise: 
 - if the embedded object with exactly the same values (but different ids) already exists
 - if a constraint is violated (e.g. only lang de and en are supported)

The following to example have the same effect: 

Example url ``PUT /api/entities/684416b4`` with an relation and embedded entity in a graph. It will create the 
embedded entity and patch the container with the new edge. 

```json
{
  "@context": {
    "cougar.graph.model.rdf": "http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#",
    "eagl": "http://av360.org/schema/eagl#",
    "xsd": "http://www.w3.org/2001/XMLSchema#", 
    "sdo": "https://schema.org/"
  },
  "@graph": [
       {
        "@type": "sdo:url",
        "sdo:url": "http://de.wikipedia.org/..", 
        "sdo:language": "de-DE"
       }, 
       {
        "@type": "eagl:WikipediaEntry",
        "@id": "684416b4", 
        "eagl:hasWikipediaLink": {
            "@id": "df87da74-7e9b-11ec-90d6-0242ac120003"
        }
  ]
}
```


Example url ``POST /api/entities/684416b4/eagl.hasWikipediaLink``

```json
{
  "@context": { "@vocab": "https://schema.org/" },
  "@type": "url",
  "url": "http://de.wikipedia.org/..", 
  "language": "de-DE"
}
```
does the same. 

### Deleting an embedded object
v2

``DELETE /api/entities/{id}``


Conflicts can arise: 
 - if the request does not include all edges pointing to the deleted object (means, we have dangling edges)
 - if the user is not allowed to modify entities, who have edges pointing to the deleted object

```json
{
  "@context": {
    "cougar.graph.model.rdf": "http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#",
    "eagl": "http://av360.org/schema/eagl#",
    "xsd": "http://www.w3.org/2001/XMLSchema#", 
    "sdo": "https://schema.org/"
  },
  "@graph": [
       {
        "@type": "sdo:url",
        "@id": "df87da74-7e9b-11ec-90d6-0242ac120003",
        "url": "http://de.wikipedia.org/../", 
        "language": "de-DE"
       }, 
       {
        "@type": "eagl:WikipediaEntry",
        "@id": "684416b4-2ea3-48b0-b55c-6a2410e2baec", 
        "eagl:hasWikipediaLink": {
            "@id": "df87da74-7e9b-11ec-90d6-0242ac120003"
      }
  ]
}
```

### Updating literals

Any attribute not pointing to an embedded entity (with its own id) is a value (literal, no object or array)

The following json is illegal

```json
{
  "@context": {
    "eagl": "http://av360.org/schema/eagl#",
  },
  "@type": "eagl:WikipediaEntry",
  "@id": "684416b4-2ea3-48b0-b55c-6a2410e2baec", 
  "complexObject": {
    "a": 1, 
    "2": 2
}
```

The complex object must have a "@type" (and thus also an @id) definition, which clarifies its attributes. 

With this in mind, updating a literal is as simple as putting a new value to a literal of type identified by its "@id"

Changing the url of the wikipedia entry is managed through the following request: 

``PUT /api/entities/df87da74-7e9b-11ec-90d6-0242ac120003`` and the payload

```json
{
  "https://schema.org/url": "http://de.wikipedia.org/new_url/"
}
```
This time, we left out the @context definition. 


Since keys are unique within the scope of one object, we also support the following URL pattern

``PUT /api/entities/df87da74-7e9b-11ec-90d6-0242ac120003/url`` 

with the value as text in the payload. 


``POST /api/entities/df87da74-7e9b-11ec-90d6-0242ac120003`` we create a new attribute. Be careful, since we create a new statement we might create multiple values. The same request above, but with POST, would result in the following entity. 

If the schema definition has put a arity-constraint on a property, this request could result in an HTTP error. 


```json
{
  "@context": {
    "cougar.graph.model.rdf": "http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#",
    "eagl": "http://av360.org/schema/eagl#",
    "xsd": "http://www.w3.org/2001/XMLSchema#", 
    "sdo": "https://schema.org/"
  },
  "@type": "sdo:url",
  "@id": "df87da74-7e9b-11ec-90d6-0242ac120003",
  "url": [
    "http://de.wikipedia.org/../", 
    "http://de.wikipedia.org/new_url/"
  ], 
  "language": "de-DE"
}

```


### Deleting and getting literals

The following HTTP operations are supported for literals

``DELETE /api/entities/{@id}/{prefix.key}`` removes the statement completely from the entity. 

Might result in constraint validation error (if the attribute must exist according to the schema)

``GET /api/entities/{@id}/{prefix.key}`` returns only the value or embedded entity in the response, 
the format depends on the requested content type. 

### Adding annotations (statements about statements)

``PUT /api/entities/{@id}/{prefix.key}``

Let's assume we want to assign a topic from a classification service to a wikipedia entry. Since the classification 
service is based on machine learning, we want to also store the prediction quality as certainty. 

```json
{
  "@context": {
    "cougar.graph.model.rdf": "http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#",
    "eagl": "http://av360.org/schema/eagl#",
    "xsd": "http://www.w3.org/2001/XMLSchema#", 
    "sdo:" "https://schema.org/"

  },

  "@type": "eagl:WikipediaEntry",
  "eagl:hasWikipediaLink": {
      "@type": "sdo:url",
      "url": {
        "@value": "http://en.wikipedia.org/.."
      }, 
      "lang": "en-US"
    }
  }, 
  "eagl:topic": {
     "@id": "topics/RiskManagement", 
     "@annotation": {
       "certainty": 0.6
     }

  }
```
See more examples in the [draft standard](https://json-ld.github.io/json-ld-star/#basic-concepts) (which is fairly new)
The support in Titanium is still experimental