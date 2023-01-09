# Sharding

The current prototype uses RDF4J as internal triplestore for the backend. It comes with a number of connectors [for commercial products](https://rdf4j.org/about/#third-party-database-solutions) (which support more complex sharding scenarios)

* Ontotext GraphDB
* Halyard
* Stardog
* Amazon Neptune
* Systap Blazegraph™
* Oracle RDF Graph Adapter for RDF4J
* MarkLogic RDF4J API
* Strabon
* Openlink Virtuoso RDF4J Provider

The [core databases](https://rdf4j.org/about/#core-databases) are (see )

* The RDF4J Memory Store is a transactional RDF database using main memory with optional persistent sync to disk. It is fast with excellent performance for small datasets. It scales with amount of RAM available.

* The RDF4J Native Store is a transactional RDF database using direct disk IO for persistence. It is a more scalable solution than the memory store, with a smaller memory footprint, and also offers better consistency and durability. It is currently aimed at medium-sized datasets in the order of 100 million triples.

* The RDF4J ElasticsearchStore is an experimental RDF database that uses Elasticsearch for storage. This is useful if you are already using Elasticsearch for other things in your project and you want to add some small scale graph data. A good usecase is if you need reference data or an ontology for your application. The built-in read cache makes it a good choice for data that updates infrequently, though for most usecases the NativeStore will be considerably faster.

