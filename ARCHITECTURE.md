# System Architecture

## High-Level Overview

Workly operates on a microservices-based architecture where specialized components handle Auth, Matching, Chat, and Search interactively.

```mermaid
graph TD
    subgraph Clients [Mobile Applications]
        ClientS[Help Seeker App]
        ClientP[Help Provider App]
    end

    subgraph Backend [Backend System]
        API[Core Server / Gateway]
        Chat[Chat Service]
        Search[Search Service]
        Config[Config Service]
        Consumer[Notification Service]
        
        %% Upcoming
        Payment[Payment Service]
        Review[Review Service]
    end

    ClientS -->|HTTP / REST| API
    ClientP -->|HTTP / REST| API
    
    ClientS -->|WebSocket| Chat
    ClientP -->|WebSocket| Chat
    
    API -->|HTTP| Search
    API -->|Kafka| Consumer
    Chat -->|Kafka| Consumer
    
    subgraph Storage [Data Layer]
        API --> Postgres[(Postgres)]
        API --> Mongo[(MongoDB)]
        Search --> Elastic[(Elasticsearch)]
        Search --> Redis[(Redis Cache)]
    end
```

## Expertise & Spelling Normalization

The system handles misspelled skills (e.g., "carpantur" -> "Carpenter") using a multi-step normalization process in the **Search Service**.

### Spelling Correction Flow

```mermaid
sequenceDiagram
    participant App as Mobile App
    participant Server as Core Server
    participant Search as Search Service
    participant Redis as Redis Cache
    participant Elastic as Elasticsearch

    App->>Server: Search "carpantur"
    Server->>Search: Normalize "carpantur"
    
    rect rgb(240, 248, 255)
        note right of Search: Step 1: Exact Match
        Search->>Elastic: Query (Exact match on Name/Alias)
        alt Match Found
            Elastic-->>Search: Return "Carpenter"
            Search-->>Server: "Carpenter"
        else No Match
            note right of Search: Step 2: Fuzzy Match (Levenshtein)
            Search->>Elastic: Query (Fuzzy match ~2 edits)
            alt Match Found
                Elastic-->>Search: Return "Carpenter"
                Search-->>Server: "Carpenter"
            else No Match
                note right of Search: Step 3: Fallback
                Search-->>Server: Return Original "carpantur"
            end
        end
    end
    
    Server-->>App: Results for "Carpenter"
```

### Technical Implementation Details

Based on the `AutocompleteService` and `SkillDocument` implementation:

1.  **Index Structure (`skills_index`)**:
    *   **Documents**: Each skill is a document containing a `canonicalName` (e.g., "Electrician") and a list of `aliases` (e.g., ["wiring expert", "lineman", "electrican"]).
    *   **Mapping**: `aliases` are stored as a standard text field.

2.  **Fuzzy Match Logic (Levenshtein)**:
    The search query in `AutocompleteService.searchInElasticsearch` constructs a **Boolean OR** criteria that attempts to match in three ways simultaneously:
    *   **Prefix Match**: `Criteria("canonicalName").contains(query)` - Finds exact substring matches.
    *   **Fuzzy Match**: `Criteria("canonicalName").fuzzy(query)` - Applies Edit Distance (Levenshtein). ElasticSearch's default `AUTO` setting is used:
        *   **0 edits** allowed for strings < 3 characters.
        *   **1 edit** allowed for strings 3-5 characters.
        *   **2 edits** allowed for strings > 5 characters.
    *   **Alias Match**: `Criteria("aliases").is(query)` - Checks the `aliases` array for a match.

This approach ensures that "plumer" matches "Plumber" (via Fuzzy) and "wiring expert" matches "Electrician" (via Alias) in a single query execution.

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

---

## Future Architecture Enhancements

To see the detailed business functionality gaps this architecture will address, refer to the [Product Roadmap](PRODUCT_ROADMAP.md). Upcoming architectural changes include:

*   **Payment & Escrow Layer**: Integration of a dedicated internal service communicating with Stripe/Razorpay via HTTP. This will hold funds in a `Postgres` transactions table before disbursing via payout APIs.
*   **KYC & Verification**: A background worker to pass provider uploaded ID documents to a 3rd party OCR/verification service, updating the `Mongo` user context asynchronously.
*   **Review Engine**: An asynchronous feedback processing engine using `Kafka` logging and aggregated in `Elasticsearch` for swift provider ranking changes upon new reviews.
