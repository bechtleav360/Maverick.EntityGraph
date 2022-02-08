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
  - 


### Access the logs

Here are a few commands to remember

````shell
az webapp list -o table

az webapp log tail -n graphs -g eagl-services

````

Go to Kudu via "Advanced tools"


### Run as Azure Web App

Configure a new web app and define the  application setting (as Deployment Option) with the Key SPRING_APPLICATION_JSON and the following value

```
{
  "endpoints": {
    "wallet": {
      "url": "http://xxxx.azurecontainer.io:8080/api/wallet",
      "apikey": "xxx"
    },
    "user": {
      "url": "http://xxxx.azurecontainer.io:8080/api/user",
      "apikey": "xxx"
    }
  },
  "key": "xxxx"
}
```

The application setting PORT should point to the port configured in the application properties. 