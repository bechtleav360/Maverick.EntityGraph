# Graph Events

Feature used to consume and publish events.

## Consuming Events

### entity.created

Expects an entity in the payload, which is transferred into the graph

`````json
{
    "specversion" : "1.0",
    "type" : "entity.created",
    "source" : "https://registered-service.com",
    "subject" : "123",
    "id" : "A234-1234-1234",
    "time" : "2018-04-05T17:31:00Z",
    "datacontenttype" : "text/turtle",
    "data" : "..."
}
`````

The source has to be registered in an application to identify the target store. Events are by itself not authenticated (
we assume the access to the queues is secured)

### entity.updated

Similar to created, the statements will be loaded as-is into the graph (the old entity will be removed, if the
identifier remained stable)

### entity.deleted

`````json
{
    "specversion" : "1.0",
    "type" : "entity.deleted",
    "source" : "https://registered-service.com",
    "subject" : "123",
    "id" : "A234-1234-1234",
    "time" : "2018-04-05T17:31:00Z"
}
`````

The entity has been deleted in the source system, it has to removed from the graph as well. Note that embedded entities
will not be deleted, if they are referenced by other entities. 