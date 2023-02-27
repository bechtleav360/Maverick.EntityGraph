# Support for applications

All API endpoints change if the application feature is enabled. The application label has to be in the path to support 
valid URIs. An application can be either a public labeled dataset or a private project.  


Let's assume we have the following registered applications: 
* The public **acme-products** dataset with a product catalog
* A private dataset **acme-teams** which lists the maintained products                   
 
The private entities should be related to the public geonames entities 

Examples: 

``GET /api/app/acme-products/entities?limit=0&offset=0``

with the response
`````json
{
  "@context": {
    "sdo": "https://schema.org",
    "base": "https://graph.example.com/api/app/x-products"
  },
  "@graph": [
    {
      "id": "base:a0",
      "type": "sdo:IndividualProduct",
      "additionalType": "http://www.productontology.org/id/Collaborative_software",
      "description": "Our collaboration product.",
      "name": "ACME Collab Suite"
    }
  ]
}
`````

and

``GET /api/app/acme-teams/entities/lfskewisjds ``

with the response
`````json
{
  "@context": {
    "sdo": "https://schema.org/",
    "base": "https://graph.example.com/api/app/x-teams",
    "prod": "https://graph.example.com/api/app/x-products"
  },
  "@id": "base:b0",
  "@type": "sdo:Organization",
  "sdo:owns": "prod:a0"
}
`````

``GET /api/app/x-team/entities/{id}`` \
``GET /api/app/x-team/entities/{id}?property=rdfs.label``




``DELETE /api/entities/{id}``
* Deletes the entity and all its direct relations (values and links)
* Deletes all links where the entity is object (incoming links) <- Test required

``PUT /api/entities/{id}``
* Not supported, Graph is idempotent
