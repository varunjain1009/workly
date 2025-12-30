# Workly Backend Server

The core engine for the Workly platform, providing high-performance matching, profile management, and job tracking.

---

## 🚀 Getting Started

### Prerequisites
- **Java 17** (OpenJDK recommended)
- **Docker & Docker Compose**
- **Gradle 8.x** (Included via Wrapper)

### 1. External Infrastructure
Workly requires several services. Start them using the provided Docker Compose file:
```bash
docker-compose up -d
```
This will launch:
- **MongoDB**: Primary document storage.
- **PostgreSQL**: Relational storage for subscriptions.
- **Redis**: Caching and locking.
- **Kafka & Zookeeper**: Messaging backbone.

### 2. Configuration
Review `src/main/resources/application.yml`. You can override any property using environment variables:
- `SPRING_DATA_MONGODB_URI`: Connection string for Mongo.
- `SPRING_DATASOURCE_URL`: JDBC URL for Postgres.
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka broker list.

### 3. Running Locally
Run the server using the Gradle wrapper:
```bash
./gradlew bootRun
```
The server will start on `http://localhost:8080`.

### 4. Monitoring

#### Health Check
The server provides a health check endpoint at `/health`. It returns a 200 OK status if the server is running correctly.

```bash
curl http://localhost:8080/health
```


---

## 🧪 Testing & Quality

### Running Tests
Workly uses JUnit 5 for unit and component testing.
```bash
./gradlew test
```

### Coverage Reports
JaCoCo is integrated to provide detailed code coverage reports.
```bash
./gradlew jacocoTestReport
```
View the results at: `build/reports/jacoco/test/html/index.html`

**Current Status**: 
- **Tests Passing**: 100%
- **Instruction Coverage**: ~83.2%

---

## 📖 Documentation Reference

- **[Architecture Guide](docs/ARCHITECTURE.md)**: Deep dive into system design and module interactions.
- **[API Contract](docs/API_CONTRACT.md)**: Exhaustive list of REST endpoints and JSON models.

---

## 📦 Deployment

### Building for Production
Create an executable JAR:
```bash
./gradlew build -x test
```
The artifact will be located in `build/libs/workly-server-*.jar`.

### Production Profiles
Activate the production profile to use optimized configurations:
```bash
java -jar workly-server.jar --spring.profiles.active=prod
```

---

## 🛠 Features
- **OTP Auth**: Secure onboarding via mobile number.
- **Geospatial Matching**: Find workers near job locations instantly.
- **Stateless Session**: JWT-based security for scalability.
- **Polyglot Persistence**: Right tool for the right data (Mongo + Postgres).
- **Event-Driven**: Kafka-based status updates and notifications.
- **Search Integration**: Proxies or federates with `workly-Search-Service`.
- **Chat Integration**: Handshakes with `workly-Chat-Service` for user context.
