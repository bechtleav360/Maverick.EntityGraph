# Entity


In its broadest sense, an entity is anything which exists. Anything which exists can be either named or identified. An entity is a unique *individual* with its own identity. An entity might exist in the real world (but in this context, this doesn't matter).

From a philosophical perspective, an entity is considered to be any being or existence, with a focus on its nature, properties, and the relationships it holds within the universe. In information technology, an entity is often referred to as a data unit or object that can be identified within a database or information system, distinct in its characteristics and identifiable through a unique key. In business and domain modeling, entities are tangible or intangible items like a person, place, or event around which data is stored. 

Anything else are concepts, values (or value objects according to domain-driven design) or classifier. An entity can have properties (
which are values). One of them is a unique identifier. Concepts like "money"
or "knowledge" can help us to categorize categories (within ontologies). In (object-oriented) software design, we can
consider the classes or types as concepts, while the concrete instances are the entities.

A more technical (or formal definition) would be: An entity is a globally unique set of properties and links.



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
