# Queries
Support for querying the graph

---

## Run Query
Version: 2

``GET /api/query?type=eagl:LearningUnit&expand=true``

returns entities of given type

*Supported query parameters:*

* ``type``: the type of the requested entity
* ``expand``: include all entities, whose type is a subclassof the given type
* ``frame``: name of the template which should be used to construct the results
* ``query``: a RSQL query (optional)
* ``limit``:
* ``page``:

In this example, the type `eagl:LearningUnit` is abstract. There are no direct instances in the graph, we only have WikipediaEntries and YoutubeVideos. By adding the ``expand``-flag, we define that we are interested in all entities whose type inherits from the query parameter.

Query support comes through attribute matching in the frames.

## Query by example
Version: 2

`POST /api/query/example`

like Get, but with a frame in post body. Query Parameters are equivalent with the exception of the ``presentation``-parameter 

## Query with SPARQL
Version: 1

`POST /api/query/sparql`

expects a valid [sparql](https://www.w3.org/TR/sparql11-protocol/) query like the following example: 

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

Only SELECT Queries are supported, no UPDATES, DELETE or INSERTS

The results are rdf statements, supported formats are

* JSON
* CSV
* 