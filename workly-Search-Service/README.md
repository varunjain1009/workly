# workly-Search-Service

The **Search Service** handles skill search, expertise normalization, and autocomplete suggestions using Elasticsearch.

## 2. Dependencies
*   **Elasticsearch 7.17.10**
*   **Plugin**: `analysis-phonetic` (Required)

## 3. Infrastructure
The service requires a custom Elasticsearch image with the `analysis-phonetic` plugin.
This is handled automatically by the root `docker-compose-dev.yml`.

### Running Locally
```bash
# Build the infrastructure first
docker-compose -f ../docker-compose-dev.yml up -d --build

# Run the service
./gradlew :search-service:bootRun
```

## 4. API
See [API.md](../API.md) for details on endpoints.
