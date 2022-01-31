# Simple (but opinionated) Restful Api

## Summary

* ``GET /api/rs/{prefix.type}/{id}`` (Read resource)
* ``GET /api/rs/{id}``
* ``GET /api/rs/{prefix.type}`` (List resources)
* ``POST /api/rs/{prefix.type}`` (Create resource)
* ``DELETE /api/rs/{prefix.type}/{id}``
* ``PUT /api/rs/{prefix.type}/{id}``
* ``GET /api/rs/{prefix:type}/{id}/{key}`` (Get value/s)
* `` ``
* `` ``

--- 

What if we try to make the api as simple as possible?

Let's assume the following constraints: 

- A node object always as a unique type registered in the schema
- Embedded objects always have a unique type


With those constraints in mind, we could build a simple JSON Api (no JSON-LD or RDF at all). Let's take the following example model

`````yaml
record: 
  @type: Record
  @id: "4214214"
   title: "applications"
   cases: 
     - @type: Case
       @id: "2142132"
        status: "rejected"
   owner: 
     @type: OrganizationalUnit
     @id: "S12"
     description: "HR"

`````

A few concepts: 

* `Values` are the literals of the current object (aka no JSON arrays or objects)
* The type is typically prefixed with the namespace identifier. Since `:` and `#` are reserved characters, we use the `.`
* Types typically exist in a type hierarchy (subClassOf-Relations)
* Composites are mereologic relations (aka parthood-relations). They habe to be a subPropertyOf of ``partOf``

## API Methods
We could expose the following API Methods

### Create Entity 

``POST /api//{prefix.type}``

``POST /api/{prefix.type}/{id}``

creates an entity

``POST /api/mav.record``
````json
{
  "title": "applications"
}
````

will create a new entity of type "record" (from namespace with prefix `mav`) and a generated id. It probably violates schema constraints (e.g. the owner is missing). 
In this case, the request should throw a SchemaViolationError with the according message, the following request would resolve this issue
````json
{
  "title": "applications", 
  "owner": "OrganizationalUnit/S12"
}
````
If the given type does not exist in the schema, we throw an error as well. 

If a json value consists of a pair "@type/@id", we can infer a connection of the given json ke

``POST /api/mav.record/4214214``

will create also the entity with the given id. Will fail if the id already exists (this operation might be only useful to repair dangling incoming links by recovering an entity)




### Read Entity


``GET /api/{prefix.type}/{id}``

``GET /api/{id}``

return one or more entities. 


``GET /api/mav.record/4214214``

Will return by default a simple json representation of the object and its direct neighbours. 


````json
{
  "type": "record",
  "id": "4214214",
  "name": "applications",
  "cases": [
    {
      "type": "case", 
      "id": "2142132", 
      "status": "rejected"
    }
  ], 
  "owner": {
    "type": "OrganizationalUnit",
    "id": "S12",
    "description": "HR"
  }
  
}
````
The response will always be in the type requested in the url. If the given type is a generalized type of the actual entity type, an upcast will be performed. Let's say `record` is subClass of `http://schema.org/Thing`, which only requires a title. The response for  

``GET /api/sdo.thing/4214214``

would be

````json
{
  "type": "thing",
  "id": "4214214",
  "name": "applications"
}
````


HATEOAS links could be build to further traverse the graph (or for downcasts), e.g. to show all documents in the cases. 

``GET /api/4214214``
will always return the entity in its most general form. This is only useful if an outdated link is requested (e.g. a type conversion happened and the old type does not exist anymore). 

### List Entities
``GET /api/{prefix.type}``

will return a paged list of all entities of the given type. It does support the same query operations as the main api. 


### Delete Entity

``DELETE /api/{prefix.type}/{id}``

Is a bit tricky for composites (e.g. the cases cease to exist if the records is deleted, the unit should of course stay). 

We could apply the following rules: 
- if the relation (e.g. "cases") is a parthood-relation, it (and its children) will be deleted.
- in any other case, only the relation itself will be deleted, the referred object stays

Note that in way it doesn't matter which type (in a valid hierarchy) is given, the two following urls are equivalent

``DELETE /api/sdo.thing/21312``

``DELETE /api/mav.record/21312``

Since we don't typically now on which level of the hierarchy we operate.



### Update Entity

``PUT /api/{prefix.type}/{id}``

will always patch the values in the payload:

``PUT /api/mav.records/4214214``
````json
{
  "description": "current job applications"
}
````

* an existing value is changed
* a new value is added
* a null value is deleted
* any object or array in the payload leads to an error

### Get values

``GET /api/{prefix.type}/{id}/{prefix.key}``


will return the value of entity field, can be either a value, object or array. The response might contain annotations

``GET /api/mav.case/093r9e039/mav.topic``
````json
{
  "value": "job application",
  "certainty": 0.24122
}
````

``GET /api/mav.record/4214214/mav.owner``

````json
{
  "value": {
    "type": "OrganizationalUnit",
    "id": "S12",
    "description": "HR"
  }
}
````




### Create embedded entity 
``POST /api/record/4214214/case``

with the body

````json
{   
  "status": "open"
}
````

will create a new entity of type "Case" and create a connection to the "Record". It will return the case itself (with it's direct neighbours again). 
Note that the ``case`` relation must be defined in the schema as ``hasPart/partOf``-Relation for this to work. 

````json
{
  "type": "Case",
  "id": "093r9e039",
  "status": "open",
  "record": {
    "type": "record",
    "id": "4214214",
    "title": "applications"
  }
}
````

The above request is equivalent to the request ``POST /api/case`` and the call ``POST /api/case/093r9e039/partOf/4214214`` afterwards. 
If an embedded object requires the existence of its parthood-relation, it will automatically marked as invalid. 

To add a document to the case, we have to use 

`POST /api/case/093r9e039/documents`

and so on. We only work on direct neighbours. 



### Create Relation

``POST /api/record/4214214/owner/S12``


with an id of an existing entity and the body
````json
{ }
````

will only create the relation. Any attributes in the body are annotations (eg. RDF-Star). 

Constraints might be violated by this operation. The request 
``POST /api/record/3221332/case/093r9e039`` has not be allowed, since `case` is of type `hasPart`. It is bi-directional, its creation will also trigger the creation of the according `partOf`-relation in the case. 
But if a constraint has been defined that a case can only be part of exactly one record, this transaction must fail. 




### Delete Relation

``DELETE /api/record/4214214/owner/S12``

will delete the edge. Might throw a schema validation error, if the edge is required.