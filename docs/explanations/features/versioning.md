# Requirements for Versioning

* Implement the memento pattern: don't use explicit versioning, but show the entity state at the given time
* Don't have explicit version (or revision) links as part of the entity (since 99% of the time client's are not
  interested in the history).

# Versioning through transactions

We can and should represent an entity as graph

````json
{
  "@context": {
    "@base": "http://www.example.com/data/",
    "ical": "http://www.w3.org/2002/12/cal/ical#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "foaf": "http://xmlns.com/foaf/0.1/",
    "ical:dtstart": {
      "@type": "xsd:dateTime"
    }
  },
  "@graph": [
    {
      "@type": "ical:Vevent",
      "ical:attendee": {
        "@id": "mm123"
      },
      "ical:dtstart": "2022-04-09T10:00:00Z",
      "ical:location": "Main Office, Room 11",
      "ical:summary": "Project Kickoff"
    },
    {
      "@id": "mm123",
      "@type": "foaf:Person",
      "foaf:knows": {
        "@id": "mmm421"
      },
      "foaf:name": "Bob"
    }
  ]
}
````

The graph is a collection of statements (a linked data fragment) which together draw a complete picture of an entity.

## Forming the entity

We consider everything having the same id with in the entities namespace to be one particular entity. In this case, the
entity in question
is a Meeting invitation for Bob. The graph representation includes the meeting, but also the FOAF representation as Bob
as attendee.

## Adding Provenance

Definition according to W3C:

> Provenance of a resource is a record that describes entities and processes involved in producing and delivering or
> otherwise influencing that resource.

> In PROV, physical, digital, conceptual, or other kinds of thing are called entities. Examples of such entities are a
> web page, a chart, and a spellchecker. Provenance records can describe the provenance of entities, and an entity’s
> provenance may refer to many other entities. For example, a document D is an entity whose provenance refers to other
> entities such as a chart inserted into D, and the dataset that was used to create that chart. Entities may be described
> as having different attributes and be described from different perspectives. For example, document D as stored in my
> file system, the second version of document D, and D as an evolving document, are three distinct entities for which we
> may describe provenance.

In the following example, we have two versions. In the first version of the meeting, Sarah was supposed to attend.

````json

{
  "@context": {
    "@base": "http://www.example.com/data/",
    "staff": "http://www.example.com/user/",
    "ical": "http://www.w3.org/2002/12/cal/ical#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "foaf": "http://xmlns.com/foaf/0.1/",
    "ical:dtstart": {
      "@type": "xsd:dateTime"
    }
  },

  "@type": "prov:Entity",
  "@id": "_:12314213312213-1",
  "@graph": [
    {
      "@type": "ical:Vevent",
      "@id": "_:12314213312213",
      "ical:attendee": {
        "@id": "staff:mm421"
      },
      "ical:dtstart": "2022-04-09T10:00:00Z",
      "ical:location": "Main Office, Room 11",
      "ical:summary": "Project Kickoff"
    },
    {
      "@id": "staff:mm421",
      "@type": "foaf:Person",
      "foaf:name": "Bob Bobson"
    }
  ]
}

````

He declined and forwarded the invitation to Sarah.

````json

{
  "@context": {
    "@base": "http://www.example.com/data/",
    "staff": "http://www.example.com/user/",
    "ical": "http://www.w3.org/2002/12/cal/ical#",
    "xsd": "http://www.w3.org/2001/XMLSchema#",
    "foaf": "http://xmlns.com/foaf/0.1/",
    "ical:dtstart": {
      "@type": "xsd:dateTime"
    }
  },

  "@type": "prov:Entity",
  "@id": "_:12314213312213-2",
  "prov:wasRevisionOf": "_:eventId1",
  "prov:used": [
    { "@id": "_:12314213312213"}, 
    { "@id": "staff:mm123"}
  ],
  "@graph": [
    {
      "@type": "ical:Vevent",
      "@id": "_:12314213312213",
      "ical:attendee": {
        "@id": "staff:mm123"
      },
      "ical:dtstart": "2022-04-09T10:00:00Z",
      "ical:location": "Main Office, Room 11",
      "ical:summary": "Project Kickoff"
    },
    {
      "@id": "staff:mm123",
      "@type": "foaf:Person",
      "foaf:knows": {
        "@id": "mmm421"
      },
      "foaf:name": "Sarah Smith"
    }
  ]
}

````

This example illustrates how we can express versions through the provenance ontology.

An interesting variation here would be: What if Sarah has since then married Bob and taken his name, she is now known
as "Sarah Bobson"?

## Technical implementation


