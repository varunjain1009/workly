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
docker-compose up -d
```
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

## 📖 Documentation
*   **[Architecture Guide](ARCHITECTURE.md)**: System design, diagrams, and data flow.
*   **[API Reference](API.md)**: REST endpoints and WebSocket protocols.
