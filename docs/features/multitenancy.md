# Multi-tenancy through applications

This feature allows for separating different entity graphs for individual applications. Each application has its own
store.

Benefits of this approach are:

* **flexible scalability**: Storage access is typically the bottleneck in a service. Lots of traffic in one application
  shouldn't impact the load of another.
* **quick testing**: Applications can be configured with in-memory storage only. Deleting an application will delete the
  store.
* **improved security**: Applications manage their own subscriptions (through access tokens)

## Activating the feature

Add the following lines to your application properties.

=== "YAML"

````yaml
application:
  features:
    modules:
      applications: true
````

=== "JSON"

````json
{
  "application": {
    "features": {
      "modules": {
        "applications": true
      }
    } 
  }  
}
````

## Usage

### Creating a new application

### Generating a new subscription token

### Revoking a subscription token