# Multitenancy through subscriptions

The admin api provides methods to create new applications. Each application can have muliple API Keys associated.

The API Key identifies the application, each application has its own repository (only admin and base schema are shared)

## Individual Repositories

We want to separate the storage for entities and transactions for each application

Options:

- default volumes, applications create subfolders
  drawbacks: no individual scalability, low security

- individual volumes
  drawbacks: creating new application would mean that we have to create new volumes

- dynamic volumes (operator pattern)
  Whenever a new application is created, we use the kubernetes API to create volumes and adapt deployment