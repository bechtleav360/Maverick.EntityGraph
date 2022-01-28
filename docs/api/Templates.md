# Templates
Endpoints to manage the templates (reusable JSON-LD Frames)

## Get template
`GET /api/frames?for=esco:Skill`

returns a list of all named templates as json array


*Query Parameters:*

* `for` filter the result to show only templates targetting the given type (either with prefix or fully qualified)


``GET /api/frames/{name}``

returns the template in valid frames syntax (and thus directly reusable)

## Create template

``POST /api/frames/{name}``

Creates a new template. Example can be found here: https://www.w3.org/TR/json-ld11-framing/#framing

*Query Paramaters:*

* ``verifiable``: if set to true, the template will be checked if it suits the needs for a verifiable presentation, see https://www.w3.org/TR/vc-data-model/#presentations

```json

{
  "@context": {"@vocab": "http://av360.org/schema/eagl#"},
  "@type": "LearningPath",
  "hasItem": {
    "@explicit": true,
    "@type": "WikipediaEntry",
    "hasWikipediaLink": { }
  }
}
```

We use the flag "@explicit" in the example. Any other attributes in the graph will be omitted.

The following flags are supported:

* [@embed](https://www.w3.org/TR/json-ld11-framing/#object-embed-flag) (default is "@once", but can also be "@never" or "@always")
* [@explicit](https://www.w3.org/TR/json-ld11-framing/#explicit-inclusion-flag) (if true, not mentioned attributes will be omitted)
* @omitDefault
* @omitGraph for single node objects
