
# Values

Any value uniquely bound to an entity is a property. Properties are always compositions (whole-part relations): The
lifetime of the property is dependent of the lifetime of the entity (or: all properties are removed together with its
parent entity).


## Characteristic Properties (or "nature")

*Characteristic properties* (Guarino) are used in the real world to infer identity. Typical examples for characteristic
properties are values like location or addresses for buildings, name and birthday for persons, title and author for
creative work, etc.

The "nature" of something typically refers to its essence or intrinsic characteristics that define what it fundamentally is. It's about the core qualities that make something uniquely itself. On the other hand, "properties" are specific attributes or features that can describe or quantify aspects of an entity, often observable and measurable. While "nature" speaks to the inherent identity of an entity, "properties" detail the various characteristics that can be attributed to it, offering a more external or observable perspective on what the entity is like.

## Identifier

Identifiers are a special kind of characteristic property. They imply, by itself, identity (without the need of
combining it with other properties). A book's ISBN Number, a person's tax identifier, URIs or UUIDs are typical
examples. Identifiers have to be atomic values encodable in persistent urls.

## Composites

Values are atomic: Each value should represent exactly one observation. An object's geographic location is a value
consisting of its longitude and latitude. A composite can be a characteristic property (e.g. a full name consisting of
title, first and last name), but never an identifier.



Values, or [literals](https://www.w3.org/TR/rdf11-concepts/#section-Graph-Literal) in the RDF model, are atomic numbers
and text.