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
Spin up the required databases and message brokers (**Run from Project Root**):
```bash
docker-compose -f docker/docker-compose.yml up -d --build
```
> **Important**: Use `docker/docker-compose.yml`. It is the **unified** configuration that includes the custom Elasticsearch build (with plugins) required by the Search Service.

This starts:
*   **MongoDB**: Primary Data & Chat Logs
*   **PostgreSQL**: Billing & Subscriptions
*   **Redis**: Caching & PubSub
*   **Kafka & Zookeeper**: Event Streaming
*   **Elasticsearch**: Search Engine

### 2. Backend Services
You can run the system in two modes: **Monolith** or **Microservices**. The mode is controlled by the `app.mode` property in `workly-Server/src/main/resources/config.properties`.

#### Option A: Monolith Mode (Recommended for quick testing)
In monolith mode, all components (Auth, Matchmaking, Chat WebSocket, Search) run within the single `workly-Server` instance.
1. Edit `workly-Server/src/main/resources/config.properties`:
   ```properties
   app.mode=monolith
   ```
2. Start the core server:
   ```bash
   ./gradlew :server:bootRun
   ```

#### Option B: Microservices Mode
In microservices mode, the core server delegates specific responsibilities to independent services.
1. Edit `workly-Server/src/main/resources/config.properties`:
   ```properties
   app.mode=microservice
   ```
2. Start the core server:
   ```bash
   ./gradlew :server:bootRun
   ```
3. In separate terminal windows, start the respective microservices:
   ```bash
   ./gradlew :chat-service:bootRun
   ./gradlew :search-service:bootRun
   ./gradlew :config-service:bootRun
   ```

### 3. Frontend (Admin)
```bash
cd workly-Admin-Portal
npm run dev
```

### 4. Mobile Apps (Android)
You can build the Android APKs using the provided Docker environments, which handle the SDK and JDK requirements automatically.

**Build workly-Help-Provider APK:**
```bash
cd workly-Help-Provider
docker build -t workly-help-provider-build .
docker create --name provider-temp workly-help-provider-build
docker cp provider-temp:/app/app/build/outputs/apk/debug/app-debug.apk ./workly-help-provider-debug.apk
docker rm provider-temp
```

**Build workly-Help-Seeker APK:**
```bash
cd workly-Help-Seeker
docker build -t workly-help-seeker-build .
docker create --name seeker-temp workly-help-seeker-build
docker cp seeker-temp:/app/app/build/outputs/apk/debug/app-debug.apk ./workly-help-seeker-debug.apk
docker rm seeker-temp
```

*Alternatively, open the projects in **Android Studio**, update `src/main/assets/config.properties` if testing on a physical device, and run them directly.*

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
*   **[Product Roadmap](PRODUCT_ROADMAP.md)**: Upcoming features and critical functionality gaps.
*   **[Architecture Guide](ARCHITECTURE.md)**: System design, diagrams, and data flow.
*   **[API Reference](API.md)**: REST endpoints and WebSocket protocols.
