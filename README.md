# Workly Platform

Workly is a hyper-local blue-collar job marketplace that connects service seekers with skilled providers in real-time. It is built as a distributed system with a focus on scalability, real-time communication, and intelligent matching.

## 📂 Project Structure

| Module | Description | Tech Stack |
| :--- | :--- | :--- |
| **[workly-Server](workly-Server)** | Core API gateway, Auth, Job Management | Spring Boot, Mongo, Postgres, Kafka |
| **[workly-Chat-Service](workly-Chat-Service)** | Real-time Messaging & Presence | Spring Boot, WebSocket, Redis, Mongo |
| **[workly-Search-Service](workly-Search-Service)** | Expertise Normalization & Autocomplete | Spring Boot, Elasticsearch, Redis |
| **[workly-Config-Service](workly-Config-Service)** | Runtime Configuration & Analytics | Spring Boot, Mongo, Redis |
| **[workly-Admin-Portal](workly-Admin-Portal)** | Admin Dashboard for Configs | React, Vite, TailwindCSS |
| **[workly-Help-Seeker](workly-Help-Seeker)** | Android App for Customers | Java, XML, MVVM, Room |
| **[workly-Help-Provider](workly-Help-Provider)** | Android App for Workers | Java, XML, MVVM, Room |

## 🚀 Quick Start

### 1. Infrastructure
Spin up the required databases and message brokers:
```bash
cd workly-Server/docker
docker-compose -f docker-compose-dev.yml up -d --build
```
*Note: The first run will build a custom Elasticsearch image with the `analysis-phonetic` plugin installed.*
This starts:
*   **MongoDB**: Primary Data & Chat Logs
*   **PostgreSQL**: Billing & Subscriptions
*   **Redis**: Caching & PubSub
*   **Kafka & Zookeeper**: Event Streaming
*   **Elasticsearch**: Search Engine

### 2. Backend Services
You can run the services individually using Gradle.

**Core Server**:
```bash
./gradlew :server:bootRun
```
**Chat Service**:
```bash
./gradlew :chat-service:bootRun
```
**Search Service**:
```bash
./gradlew :search-service:bootRun
```
**Config Service**:
```bash
./gradlew :config-service:bootRun
```

### 3. Frontend (Admin)
```bash
cd workly-Admin-Portal
npm run dev
```

### 4. Mobile Apps (Android)
*   Open the project in **Android Studio**.
*   Sync Gradle.
*   Update `src/main/assets/config.properties` if testing on a physical device (default is `10.0.2.2` for emulator).
*   Run `workly-Help-Seeker` or `workly-Help-Provider`.

### 5. Unified Local Debugging (VS Code)
**Prerequisites**: Docker Desktop, VS Code, Java Extension Pack.

The project includes a **"One Click" Debug setup** that runs all services (`Server`, `Chat`, `Search`, `Config`) simultaneously and attaches the debugger to each.

1.  Open Project in **VS Code**.
2.  Go to **Run and Debug** (`Ctrl+Shift+D`).
3.  Select **"Debug All Services"** from the dropdown loop.
4.  Press **F5**.

**What happens?**
*   **Infrastructure**: VS Code automatically starts Redis, Mongo, Postgres, Kafka, Elastic via `docker-compose-dev.yml`.
*   **Services**: All 4 microservices start in parallel.
*   **Debug**: Breakpoints work in any service instantly.

### 6. Infrastructure Only
If you prefer running services from command line but need databases:
```bash
docker-compose -f docker-compose-dev.yml up -d
```

## 📖 Documentation
*   **[Architecture Guide](ARCHITECTURE.md)**: System design, diagrams, and data flow.
*   **[API Reference](API.md)**: REST endpoints and WebSocket protocols.
