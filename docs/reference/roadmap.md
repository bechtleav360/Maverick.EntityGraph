# Roadmap along versions

# 1 - Loading data

- [x] Support for native queries
- [x] Create entity
- [x] Add value
- [x] Create relation to existing entity
- [x] Service configuration with working swagger UI
- [x] Instance on cloud with persistent storage
- [x] Preloaded videos

# 2 - Maverick.EntityGraph MVP

- [x] Read entity
- [x] Multitenancy API
- [ ] K8S Deployment Pipeline (via Helm chart)
    - [ ] Configuration of applications through Config Map
    - [ ] Applications json through values and Helm templating
- [x] Read value or relation
- [x] Delete entity
- [x] Delete value or relation
- [x] Update value
- [ ] Activating EntityGraph as WebApp

# 3 - Data Sync Strategy

- [ ] Configure application events (with documentation)
- [ ] Create new feature which forwards events to webhook
- [ ] Create sidecar to forward application events via webhook to message queue
- [ ] Implement regular nquads dumps in S3 of each registered application as new feature
- [ ] Regular import pipeline through preconfigured s3 bucket
- [ ] Consolidate application configuration in single config bean

# 4 - Entity API complete

- [ ] Paging support
- [ ] HATEOAS navigation
- [ ] Patch entity
- [ ] Query parameter

# 5 - Schema aware

- [ ] a
- [ ] a

# 6 - Frames

- [ ] Support presentation query param in GetEntity-Operation
- [ ] Query by example

# 7 - Proof

- [ ] Support proof query parameter

# 8 - JSON only mode
