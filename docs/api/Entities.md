# Entities

Managing the entity resources



## Working with entities

``GET /api/entities?limit=0&offset=0``
* List entities
* Status: implemented in v1


``POST /api/entities`` 
* Create entity
* Status: implemented in v1

``GET /api/entities/{id}`` \
``GET /api/entities/{id}?property=rdfs.label`` 
* Reads entity
* The property param can be used to specify, that the id is not the unique entity id, but the value of the property
* Status: implemented in v1

``DELETE /api/entities/{id}``
* Deletes the entity and all its direct relations (values and links)
* Deletes all links where the entity is object (incoming links) <- Test required 

``PUT /api/entities/{id}``
* Not supported, Graph is idempotent


## Entity Values
Values are identified by `source entity - prefix.key - language`

``GET /api/entities/{id}/values``
* Returns only the entity values 
* Supported mimetypes: RDF Formats, application/json

``POST /api/entities/{id}/values/{prefix.key} ``\
``PUT /api/entities/{id}/values/{prefix.key} ``\
* Consumes ``text/plain``
* Create value
* Requires a language tag (default "en" is appended)
* Status: implemented in v1
* Missing: Overwrite existing value
* Missing: add language tag as query parameter

``POST /api/entities/{id}/values``
* Consumes: ``text/turtle``
* The payload must contain the referenced entity, all connected statements are stored
* Create nested/embedded object
* The embedded object is still an entity value (and will be deleted if the entity is deleted)
* Status: TODO
* Missing: Scheduler: if a second entity points to embedded object, it has to be converted to an entity

``PUT /api/entities/{id}/values/{prefix.key} text/plain`` 
* Update value
* is equivalent to POST request, but should ideally throw exception if value does not exist

``DELETE /api/entities/{id}/values/{prefix.key}``
* Removes the unique property value

## Entity Links
Relations are identified by `source entity - prefix.key - target entity`

``PUT /api/entities/{id}/links/{prefix.key}/{targetId}`` 
* Create edge to existing entity identified by target id
* Fails if target does not exist
* Status: TODO v2
* Multiple values are allowed to exist

``DELETE /api/entities/{id}/links/{prefix.key}/{targetId}`` 
* Deletes the relation (but nothing else)
* Status: TODO v2

``GET /api/entities/{id}/links/{prefix.key}`` 
* Returns all links of the given type
* Status: TODO v2

``PUT /api/entities/{id}/links/{prefix.key}``
* Creates one (or more) links 
* Status: TODO v4

``GET /api/entities/{id}/links``
* Returns all links
* Status: TODO v2


## Property Details (Annotations)

``POST /api/entities/{id}/values/{prefix.key}/details/{prefix.key}?append=true`` \
``POST /api/entities/{id}/links/{prefix.key}/{targetId}/details/{prefix.key}``
* Consumes: ``text/plain``,
* Creates a statement about a statement use the post body as value
* Creates also the value if it doesn't exist yet
* For keep in line with the other endpoints, this call will overwrite an existing prefix.key combination. Add the query param "?append" to support multiple values. 

``POST /api/entities/{id}/values/{prefix.key}/details`` \
``POST /api/entities/{id}/links/{prefix.key}/{targetId}/details`` 
* Consumes: ``application/x-turtlestar``, 
* Creates a statement about a statement using the rdf* syntax
* Creates also the value if it doesn't exist yet
* The payload requires the original statement, e.g. ``<<:a sdo:teaches :b>> :suggestedBy :x``


``GET /api/entities/{id}/values/{prefix.key}/details?hash=true`` \
``GET /api/entities/{id}/links/{prefix.key}/{targetId}/details``
* Returns all details for a value or link
* If the hash property is true, a hash for each detail will be computed. This can be used to delete it later

``DELETE /api/entities/{id}/values/{prefix.key}/details`` \
``DELETE /api/entities/{id}/links/{prefix.key}/{targetId}/details``
* Purges all property details

``DELETE /api/entities/{id}/values/{prefix.key}/details/{prefix.key}?multiple=true&hash=21312`` \
``DELETE /api/entities/{id}/links/{prefix.key}/{targetId}/details/{prefix.key}?multiple=true&hash=true``
* Removes a specific detail. 
* If multiple exist: add the ``multiple`` parameter to delete all or use the hash parameter to identify the correct one

````
<<:a sdo:teaches :b>>
<<:hasComment "long text">>
:hash "1394801298329"
<<:hasComment "even longer text">>
:hash "ß09ß1392ß32ß0"
````
## Further Endpoints for the future

* ``GET /api/entities/{prefix.type}/{id}`` (Read entity with type coercion) /v3

* ``PUT /api/entities/{id}`` (Patch entity) /v3

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
* a set of turtle statements with an entity of a type (if object identifier is anonymous, it will be transformed to a
  valid id)

```json
{
  "@context": {
    "rdf": "http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#",
    "mav": "http://av360.org/schema/maverick#",
    "xsd": "http://www.w3.org/2001/XMLSchema#", 
    "sdo": "https://schema.org/"

  },
  "@graph": [
    {
      "@type": "mav:WikipediaEntry",
      "additionalAttribute": "",
      "mav:hasWikipediaLink": {
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
    "mav": "http://av360.org/schema/maverick#",
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
        "@type": "mav:WikipediaEntry",
        "@id": "684416b4", 
        "mav:hasWikipediaLink": {
            "@id": "df87da74-7e9b-11ec-90d6-0242ac120003"
        }
  ]
}
```

Example url ``POST /api/entities/684416b4/mav.hasWikipediaLink``

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
    "mav": "http://av360.org/schema/maverick#",
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
        "@type": "mav:WikipediaEntry",
        "@id": "684416b4-2ea3-48b0-b55c-6a2410e2baec", 
        "mav:hasWikipediaLink": {
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
    "mav": "http://av360.org/schema/maverick#",
  },
  "@type": "mav:WikipediaEntry",
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

``POST /api/entities/df87da74-7e9b-11ec-90d6-0242ac120003`` we create a new attribute. Be careful, since we create a new
statement we might create multiple values. The same request above, but with POST, would result in the following entity.

If the schema definition has put a arity-constraint on a property, this request could result in an HTTP error.

```json
{
  "@context": {
    "cougar.graph.model.rdf": "http://www.w3.org/1999/02/22-cougar.graph.model.rdf-syntax-ns#",
    "rdfs": "http://www.w3.org/2000/01/cougar.graph.model.rdf-schema#",
    "mav": "http://av360.org/schema/maverick#",
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
    "mav": "http://av360.org/schema/maverick#",
    "xsd": "http://www.w3.org/2001/XMLSchema#", 
    "sdo:" "https://schema.org/"

  },

  "@type": "mav:WikipediaEntry",
  "mav:hasWikipediaLink": {
      "@type": "sdo:url",
      "url": {
        "@value": "http://en.wikipedia.org/.."
      }, 
      "lang": "en-US"
    }
  }, 
  "mav:topic": {
     "@id": "topics/RiskManagement", 
     "@annotation": {
       "certainty": 0.6
     }

  }
```

See more examples in the [draft standard](https://json-ld.github.io/json-ld-star/#basic-concepts) (which is fairly new)
The support in Titanium is still experimental