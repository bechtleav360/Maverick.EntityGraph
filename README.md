# eagl-service-graph
API to access skills, learning units, and everything else through a json-ld api


## Setting up


### Configure Azure WebApp

1. Create new Webapp
2. Set following parameters for Container Registry
  - Private Container registry as registry source
  - ``https://ghcr.io`` as server url
  - Your GIT PAT for the credentials
  - ``bechtleav360/eagl-service-graph:latest`` as full image name
3. Enable Application logging
4. Add application property "SPRING_APPLICATION_JSON" (see below)


### Configure Storage

1. Create a new storage account (if one doesn't exist yet in your resource group)
2. Create the following new file share within the account:
  - ``graph-entities``
  - ``graph-transactions``
3. Switch to Configuration for your webapp and there to "Path mappings"
   1. Create a new mount point for each file share under "/var/data"


### Access the logs

Here are a few commands to remember

````shell
az webapp list -o table

az webapp log tail -n graphs -g eagl-services

````

Go to Kudu via "Advanced tools"


### Run as Azure Web App

Configure a new web app and define the  application setting (as Deployment Option) with the Key SPRING_APPLICATION_JSON and the following value

```json
{ "security": { "apiKey": "xxx" }, "spring": { "profiles": { "active": "test" }, "security": { "user": { "name": "admin", "password": "xxx" } } }, "logging": { "level": { "com": { "bechtle": "TRACE" } } } }
```

The application setting PORT should point to the port configured in the application properties. 