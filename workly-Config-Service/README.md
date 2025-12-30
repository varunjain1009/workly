# workly-Config-Service

The **Config Service** is a centralized storage for runtime configurations, feature flags, and system limits. It enables dynamic updates to running services without restarts.

## Features
*   **Key-Value Storage**: Store JSON or primitive values.
*   **Versioning**: Every change creates a new version.
*   **Deep Rollback**: Instantly revert to any previous version.
*   **Real-time Propagation**: Uses Redis Pub/Sub to push updates to `workly-Server` and others.
*   **Audit Logging**: Tracks `createdBy` and `createdAt`.

## Tech Stack
*   **Java 17**, **Spring Boot 3.4**
*   **MongoDB**: Persistent storage (History preserved).
*   **Redis**: Messaging channel (`config_updates`).

## Running
```bash
./gradlew :config-service:bootRun
```
Port: `8084`

## API
See [API.md](../API.md) for details.
