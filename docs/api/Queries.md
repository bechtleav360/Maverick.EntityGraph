# Queries

Support for querying the graph

## Summary

* ``GET /api/query`` (Query with parameter) /v3
* `POST /api/query/example` (Query by example) /v5
* `POST /api/query/native` (Native query) /v1

---

## Run Query

Version: 2

``GET /api/query?type=mav:LearningUnit&expand=true``

returns entities of given type

*Supported query parameters:*

* ``type``: the type of the requested entity
* ``expand``: include all entities, whose type is a subclassof the given type
* ``frame``: name of the template which should be used to construct the results
* ``query``: a RSQL query (optional)
* ``limit``:
* ``page``:

In this example, the type `mav:LearningUnit` is abstract. There are no direct instances in the graph, we only have
WikipediaEntries and YoutubeVideos. By adding the ``expand``-flag, we define that we are interested in all entities
whose type inherits from the query parameter.

Query support comes through attribute matching in the frames.

## Query by example

Version: 2

`POST /api/query/example`

like Get, but with a frame in post body. Query Parameters are equivalent with the exception of the ``presentation``
-parameter. It makes use of the value matching properties.

Not sure about the performance, we probably require a least the type in the frame.

## Query with SPARQL

Version: 1

`POST /api/query/native`

expects a query native to the underlying storage engine. The storage engine is typically sparql, but might also be
cypher, gremlin, woql, aql, or anything else.

In case of a RDF triple store, this endpoint requires a valid [sparql](https://www.w3.org/TR/sparql11-protocol/) query
like the following example:

```sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?name 
       ?email
WHERE
  {
    ?person  a          foaf:Person .
    ?person  foaf:name  ?name .
    ?person  foaf:mbox  ?email .
  }
```

Only Tuple Query (SELECT) and Graph Queries (CONSTRUCT) Queries are supported, no UPDATES, DELETE or INSERTS
Which parsers are invoked depends on the Accept-Header

For ``select``

* ``text/csv``
* ``application/sparql-results+json``

For ``construct``

* ``text/turtle``
* ``application/ld+json``
  (and others)
