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

## ⚙️ Third-Party Integrations & Configurations
`workly` delegates key external responsibilities to 3rd party providers. For local development, all features are simulated. Below is the guide to enabling them for production in `workly-Server/src/main/resources/application.yml` or `config.properties`.

### 1. Feature Flags (Bypassing Payments & SMS)
By default, **Payments** and **SMS** are bypassed to allow seamless compilation and testing. Setting `payments.enabled=false` forces the Escrow engine to generate mock "BYPASSED" intents without querying Stripe.
Check `application.yml`:
```yaml
workly:
  features:
    payments: false # Disables real Escrow checkout locking
    sms: false      # Disables Twilio/Fast2SMS and logs OTP to console
```
*Note: The frontend mobile apps automatically read these flags dynamically via `GET /api/v1/config/features` on launch.*

### 2. Stripe Payment Gateway Setup
When moving to production, update your gateway credentials:
1. Turn `workly.features.payments.enabled=true`
2. Configure your secret mapping in `application.yml` (future integration):
```yaml
stripe:
  secret-key: "sk_test_..."
  webhook-secret: "whsec_..."
```

### 3. SMS & OTP (Twilio / Amazon SNS)
Currently, `sms.provider` maps to `mock`. To enable real text messages:
1. Turn `workly.features.sms.enabled=true`
2. Update the specific gateway settings under the `sms:` block.

### 4. Push Notifications (Firebase / FCM)
Real-time push notifications are handled via Google Firebase Cloud Messaging.
1. Download `google-services.json` from your Firebase Console.
2. Drop the file inside the root of both Android Mobile Apps architectures:
   - `workly-Help-Provider/app/google-services.json`
   - `workly-Help-Seeker/app/google-services.json`
3. Download your backend `firebase-adminsdk.json` and place it in `workly-Server/src/main/resources/`.

### 5. Local Networking vs Android Emulator IPs
If you are testing from a physical Android Device, ensure your host machine IP (e.g., `192.168.1.5`) replaces `localhost` inside:
- `application.yml`
- Android App's `NetworkClient.java` or `strings.xml`.

If testing on the **Android Emulator**, use the specialized localhost bridge: `192.168.31.112`.

## 📖 Documentation
*   **[Product Roadmap](PRODUCT_ROADMAP.md)**: Upcoming features and critical functionality gaps.
*   **[Architecture Guide](ARCHITECTURE.md)**: System design, diagrams, and data flow.
*   **[API Reference](API.md)**: REST endpoints and WebSocket protocols.
