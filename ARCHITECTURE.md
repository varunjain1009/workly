# System Architecture

## High-Level Overview

Workly operates on a microservices-based architecture where specialized components handle Auth, Matching, Chat, and Search interactively.

```mermaid
graph TD
    ClientS[Seeker App] -->|HTTP| API[Core Server]
    ClientP[Provider App] -->|HTTP| API
    
    ClientS -->|WebSocket| Chat[Chat Service]
    ClientP -->|WebSocket| Chat
    
    API -->|Kafka| Consumer[Notification Module]
    Chat -->|Kafka| Consumer
    
    API -->|HTTP| Search[Search Service]
    Admin[Admin Portal] -->|HTTP| Config[Config Service]
    
    subgraph specific [Storage Layer]
        API --> Postgres[(Postgres)]
        API --> Mongo[(MongoDB)]
        Chat --> MongoChat[(Mongo ChatLogs)]
        Search --> Elastic[(Elasticsearch)]
        Search --> Redis[(Redis Cache)]
        Config --> ConfigDB[(MongoDB/Redis)]
    end

    Config -->|Pub/Sub| API
    Config -->|Pub/Sub| Chat
    Config -->|Pub/Sub| Search
    Config -->|Pub/Sub| Consumer
```

## Module Responsibilities

### 1. Core Server (`workly-Server`)
*   **Authentication**: OTP-based login (Twilio/Mock).
*   **Job Management**: CRUD for Jobs, Assignments, and Status updates.
*   **Matching Engine**: Geospatial queries to find providers within range.
*   **Notifications**: Consumes Kafka events to send FCM push notifications.

### 2. Chat Service (`workly-Chat-Service`)
*   **Protocol**: WebSocket (`/ws/chat`).
*   **Persistence**: "Persist-before-Delivery" model using MongoDB.
*   **Events**: Publishes `chat-events` to Kafka for notifications.
*   **Security**: Token-based handshake authentication.

### 3. Search Service (`workly-Search-Service`)
*   **Goal**: Normalize messy user input into canonical skills (e.g., "electrisian" -> "Electrician").
*   **Stack**: Elasticsearch for Fuzzy/Phonetic search, Redis for Prefix caching.
*   **Data Flow**:
    ```mermaid
    sequenceDiagram
        Client->>API: Autocomplete "elec"
        API->>Redis: Check Cache
        alt Cache Hit
            Redis-->>Client: ["Electrician"]
        else Cache Miss
            API->>Elastic: Fuzzy Search "elec"
            Elastic-->>API: ["Electrician"]
            API->>Redis: Cache Result
            API-->>Client: ["Electrician"]
        end
    ```

## Key Flows

### Job Creation & Notification

```mermaid
sequenceDiagram
    participant Seeker as Seeker App
    participant API as workly-Server
    participant DB as Database
    participant Kafka as Kafka
    participant Consumer as Notification Consumer
    participant FCM as Firebase FCM
    participant Worker as Provider App

    Seeker->>API: POST /jobs (Create Job)
    API->>DB: Save Job Details
    DB-->>API: Job Saved
    API->>DB: Geospatial Query (Find Nearby Workers)
    DB-->>API: Matching Workers List
    API->>Kafka: Publish job-created Event
    API-->>Seeker: Job Created Successfully
    
    Kafka->>Consumer: Consume job-created
    loop For each matching worker
        Consumer->>FCM: Send Push Notification
        FCM->>Worker: "New Job Available"
    end
```

### Real-Time Chat

```mermaid
sequenceDiagram
    participant Seeker as Seeker App
    participant Chat as Chat Service
    participant DB as MongoDB
    participant Worker as Provider App
    participant Kafka as Kafka
    participant Consumer as Notification Consumer
    participant FCM as Firebase FCM

    Seeker->>Chat: Connect WebSocket
    Chat-->>Seeker: Connection Established
    Worker->>Chat: Connect WebSocket
    Chat-->>Worker: Connection Established
    
    Seeker->>Chat: Send Message to Worker
    Chat->>DB: Persist Message
    DB-->>Chat: Saved
    
    alt Worker is Online
        Chat->>Worker: Push Message via WebSocket
    else Worker is Offline
        Chat->>Kafka: Publish chat-event
        Kafka->>Consumer: Consume chat-event
        Consumer->>FCM: Send Push Notification
        FCM->>Worker: "New Message from Seeker"
    end
```

### Configuration Flow (Server-Side Runtime)

```mermaid
sequenceDiagram
    participant Admin as Admin Portal
    participant Config as Config Service
    participant DB as MongoDB
    participant Redis as Redis Pub/Sub
    participant Server as workly-Server
    participant Chat as Chat Service
    participant Search as Search Service

    Admin->>Config: Update Configuration
    Config->>DB: Save Config v2
    DB-->>Config: Saved
    Config->>Redis: Publish config_updates Event
    
    par Listeners Update Cache
        Redis->>Server: config_updates
        Server->>Server: Update In-Memory Cache
        Redis->>Chat: config_updates
        Chat->>Chat: Update In-Memory Cache
        Redis->>Search: config_updates
        Search->>Search: Update In-Memory Cache
    end
    
    Note over Server,Search: Next request uses new config (< 10ms latency)
```

### Dynamic Configuration Sync to Mobile Apps

The platform implements real-time configuration synchronization using **Firebase Cloud Messaging (FCM)** to push updates to mobile applications without requiring app updates.

```mermaid
sequenceDiagram
    participant Admin as Admin/Config Service
    participant Server as workly-Server
    participant DB as MongoDB
    participant FCM as Firebase Cloud Messaging
    participant App as Mobile App

    Admin->>Server: Config Updated
    Server->>DB: Query UserTokens
    DB-->>Server: Return Tokens
    
    loop For each token
        Server->>Server: Check throttle interval
        alt Interval passed
            Server->>FCM: Send CONFIG_UPDATE
            Server->>DB: Update lastConfigNotificationTime
            FCM->>App: Push Notification
            App->>Server: GET /api/v1/config/public
            Server-->>App: Latest Config
            App->>Server: POST /api/v1/config/sync
            Server->>DB: Update lastSyncedTime
        end
    end
```

**Key Components:**
- **Throttling**: Notifications limited to 1-hour intervals per device (configurable)
- **Tracking**: `UserToken` stores `lastConfigNotificationTime` and `lastSyncedTime`
- **Endpoints**: 
  - `GET /api/v1/config/public` - Fetch latest configuration
  - `POST /api/v1/config/sync` - Acknowledge sync completion
- **Mobile Integration**: Apps listen for `CONFIG_UPDATE` FCM messages and trigger `ConfigManager.syncConfig()`
