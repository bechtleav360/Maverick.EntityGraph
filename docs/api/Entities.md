# Entities
Managing entities

---
## Get entity
``GET /api/entities/{id}``

*Supported query parameters:*
Returns an invidual item

* ``proof``: whether to include a proof (signature)
* ``presentation``: use a named template to construct a specific view on the entity


## Create entity

``POST /api/entities``

Payload has to be a valid json-ld document with a @type and ideally @id (could be automatically generated)

```json
{
  "@context": {
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    "eagl": "http://av360.org/schema/eagl#",
    "xsd": "http://www.w3.org/2001/XMLSchema#", 
    "sdo:" "https://schema.org/"

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
  ]
}
```

If it includes a proof, it will be verified (and should signed in a credential chain)


## Partial updates
Partial updates are crucial in the graph, and natively supported. "@id" is required


### Adding an embedded object

Scenario: adding a new wikipedia link in a different language

Conflicts can arise: 
 - if the embedded object with exactly the same values (but different ids) already exists
 - if a constraint is violated (e.g. only lang de and en are supported)
 - 
```json
{
  "@context": {
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
    "eagl": "http://av360.org/schema/eagl#",
    "xsd": "http://www.w3.org/2001/XMLSchema#", 
    "sdo": "https://schema.org/"
  },
  "@graph": [
       {
        "@type": "sdo:url",
        "@id": "df87da74-7e9b-11ec-90d6-0242ac120003",
        "url": "http://de.wikipedia.org/..", 
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

### Deleting an embedded object

Is the same request again via HTTP DELETE. 

Conflicts can arise: 
 - if the request does not include all edges pointing to the deleted object (means, we have dangling edges)
 - if the user is not allowed to modify entities, who have edges pointing to the deleted object

```json
{
  "@context": {
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
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

Any attribute which is not it's own entity has to be literal. 

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
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
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

``DELETE /api/entities/{entity @id}/{key}`` removes the statement completely from the entity. Might result in constraint validation error (if the attribute must exist according to the schema)

``GET /api/entities/{entity @id}/{key}`` returns only the value in the response, the format depends on the requested content type. 

### Adding annotations (statements about statements)

Let's assume we want to assign a topic from a classification service to a wikipedia entry. Since the classification service is based on machine learning, we want to also store the prediction quality as certainty. 

```json
{
  "@context": {
    "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
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