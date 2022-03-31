# Defining aspects in the schema

An option for the validation of incoming entities can be the shape expression language (SHEX). An example is

**our EmployeeShape reuses the FOAF ontology**

```shex
<EmployeeRecordShape> {                # An <EmployeeShape> has:
    foaf:givenName  xsd:string+,   # at least one givenName.
    foaf:familyName xsd:string,    # one familyName.
    foaf:phone      IRI*,          # any number of phone numbers.
    
}

```

Supported features are: 
- cardinality
- enums
- types
- inheritance


## Aspects 

Possible aspects could be
- verifable (if true, a hashsum will be generated on storage)
- versionable
- etc.

### Possible solutions

- presence of attributes in shape (e.g. proof -> verifiable)
- markers separate from shape (shapeId: verifiable, ...)
- 

