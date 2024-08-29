## About
A distributed in-memory key-value store implemented as a Spring Boot service. It provides a fast, scalable, and fault-tolerant solution for storing and retrieving data with automatic expiration capabilities.

## Summary
Simulated a distributed environment with multiple nodes within a single application instance. The consistent hashing algorithm is used to distribute keys across these virtual nodes.

When a key-value pair is added, the consistent hashing algorithm determines which node should store that data.
The consistentHash.get(key) call determines which node should hold the data for that key.

Check Node distribution: return a map showing how many key-value pairs are stored in each node.
