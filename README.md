# Maverick.EntityGraph

API to access skills, learning units, and everything else through a json-ld api

## Working with the code

Check the following documentation for instructions how to develop, build and run the service.

### Develop

Start locally with param ``spring_profiles_active=dev`` in your run configuration. The main method can be found in the
main-module.

### Package, Test and Run

Run the following commands (starting from this dir) to start the service

````
mvn install
mvn -f maverick.graph.main/pom.xml spring-boot:run 
````

By default, the "dev" profile is active. Repositories are all in-memory. To start with another profile, append the
following argument `-Dspring-boot.run.profiles=stage`

### Build image (and push)

Use the following command to build the docker image

````
mvn -f maverick.graph.main/pom.xml spring-boot:build-image 
````

To push the built image to a container registry, you can (and have to) override the following arguments:

````
docker.publish=true
docker.credentials.user=XX
docker.credentials.password=XX
docker.registry.host=https://docker.io
image.name=docker.io/yourusername/image:latest

````

Example (for publishing to the defaut Github Container registry):

````
mvn -f maverick.graph.main/pom.xml spring-boot:build-image -Ddocker.publish=true -Ddocker.credentials.user=XX  docker.credentials.password=XX
````

### Deploy

#### Run in Kubernetes

You can find a Helm chart in the infra folder. Follow the directions documented there.

#### Configure Azure WebApp

1. Create new Webapp
2. Set following parameters for Container Registry

- Private Container registry as registry source
- ``https://ghcr.io`` as server url
- Your GIT PAT for the credentials
- ``bechtleav360/maverick-entity-graph:latest`` as full image name

3. Enable io.av360.maverick.graph.Application logging
4. Add application property "SPRING_APPLICATION_JSON" (see below)
5. Add application property "PORT" in configuration

#### Configure Storage

1. Create a new storage account (if one doesn't exist yet in your resource group)
2. Create the following new file share within the account:

- ``graph-entities``
- ``graph-transactions``

3. Switch to Configuration for your webapp and there to "Path mappings"
    1. Create a new mount point for each file share under "/var/data"

#### Access the logs

Here are a few commands to remember

````shell
az webapp list -o table

az webapp log tail -n graphs -g maverick-services

````

Go to Kudu via "Advanced tools"

#### Run as Azure Web App

Configure a new web app and define the application setting (as Deployment Option) with the Key SPRING_APPLICATION_JSON
and the following value

```json
{ "security": { "apiKey": "xxx" }, "spring": { "profiles": { "active": "test" }, "security": { "user": { "name": "admin", "password": "xxx" } } }, "logging": { "level": { "com": { "bechtle": "TRACE" } } } }
```

The application setting PORT should point to the port configured in the application properties. 