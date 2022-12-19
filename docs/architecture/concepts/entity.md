# Key Concepts

A knowledge base can be any technology used to store arbitrary structured and unstructured data. Knowledge Graphs build
on graph structures with its data models and topology information to capture the inherent relationships within this
data.
According to Wikipedia, [Knowledge Graphs](https://en.wikipedia.org/wiki/Knowledge_graph) are typically used to "store
interlinked descriptions of entities – objects, events,
situations or abstract concepts". Today, Knowledge Graphs are wideley used
by [Google](https://en.wikipedia.org/wiki/Google_Knowledge_Graph), Microsoft or Facebook.

From a technical point of view, we consider the entity graph to be an opinionated (or constrained) knowledge graph.
While the knowledge graph allows for storing arbitrary data (as long as it can be
expressed in nodes and edges), the entity graph puts the focus on entities.

## Entities

In its broadest sense, an entity is anything which exists. An entity is a unique *individual* with its own identity. An
entity might exist in the real world (but for the graph, this doesn't matter).
Anything else are concepts, values (or value objects according to domain-driven design). An entity can have properties (
which are values). One of them is a unique identifier. Concepts like "money"
or "knowledge" can help us to categorize categories (within ontologies). In (object-oriented) software design, we can
consider the classes or types as concepts, while the concrete instances are the entities.

A more technical (or formal definition) would be: An entity is a globally unique set of properties and links.

## Properties

Any value uniquely bound to an entity is a property. Properties are always compositions (whole-part relations): The
lifetime of the property is dependent of the lifetime of the entity (or: all properties are removed together with its
parent entity).
Values, or [literals](https://www.w3.org/TR/rdf11-concepts/#section-Graph-Literal) in the RDF model, are atomic numbers
and text.

### Characteristic Properties

*Characteristic properties* (Guarino) are used in the real world to infer identity. Typical examples for characteristic
properties are values like location or addresses for buildings, name and birthday for persons, title and author for
creative work, etc.

### Identifier

Identifiers are a special kind of characteristic property. They imply, by itself, identity (without the need of
combining it with other properties). A book's ISBN Number, a person's tax identifier, URIs or UUIDs are typical
examples. Identifiers have to be atomic values encodable in persistent urls.

### Composites

Values are atomic: Each value should represent exactly one observation. An object's geographic location is a value
consisting of its longitude and latitude. A composite can be a characteristic property (e.g. a full name consisting of
title, first and last name), but never an identifier.

## Relations

Relations link entities. On a technical level, everything in a triple store is such a link (including the values). A
link which points to an an identifier is a relation.

### Entity Relations

Relations are in between entities. The link is always typed. The type must not be a characteristic property (e.g.
labels, locations).

### Classification

Classifiers a categorical values, which can (and in fact, have to be) shared. During upload, classifiers might be values
which have to be resolved into individual entities.

# Requirements

The definitions above create these requirements:

> *An entity must have (at least) one unique identifier. Each unique identifier must resolve to exactly one entity.*

Uploading any data which cannot be mapped to entities (e.g. are missing any identifiers) should result in an error. If
anonymous (e.g. randomly generated identifiers) are used, additional characteristic properties are required
to allow for deduplication (to be able to verify, that this particular individual is not already present in the graph)

> *An entity can have an open set of values*

The values must be either simple data values (e.g. number or strings) or composites (tree structures only with values)

> *Values are bound to the entity. Values cannot exist on their own and cannot be shared.*

If the entity is destroyed, all values (including trees) have to be destroyed as well.

> *Classifiers must follow a standard*

Since classifiers are used to structure the entities, they must follow a common scheme. Classifiers such as tags must
use well-known relations or types to identifiable as such.

> *Only entities can be linked*

Entities are linked to classifiers and other entities. No values of classifiers can be linked to anything.

## Readings

* http://qedcode.com/content/entities-values-facts-and-projections.html
*
Guarino: [A Formal Ontology of Properties](https://www.researchgate.net/publication/2362508_A_Formal_Ontology_of_Properties/link/0912f5051ab33a34ff000000/download) 
