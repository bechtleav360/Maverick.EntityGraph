Various concepts and terms, which are relevant to understand the graph's architecture

## Idempotence

[Wikipedia definition](https://en.wikipedia.org/wiki/Idempotence)

The graph is meant to consume event streams. Events are usually not guaranteed to be delivered exactly once. Creating an
entity which already exists should not create a duplicate. It shouldn't have any effect at all. 
