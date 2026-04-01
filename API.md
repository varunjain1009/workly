# API Reference

> All endpoints require `Authorization: Bearer <jwt>` unless marked **public**.
> Admin endpoints additionally require a token issued by `POST /api/v1/admin/auth/login` (carries `role=ADMIN`).

---

## 1. Authentication (`workly-Server` / `workly-Auth-Service`)
Base URL: `http://localhost:8080/api/v1/auth` | port 8085 in microservice mode

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/send-otp` | public | Request OTP for a mobile number |
| POST | `/auth/verify-otp` | public | Verify OTP; returns JWT |
| POST | `/auth/refresh` | public | Refresh an expiring JWT |

**POST /auth/send-otp**
```json
{ "mobileNumber": "9876543210" }
```

**POST /auth/verify-otp**
```json
{ "mobileNumber": "9876543210", "otp": "1234" }
```
Response:
```json
{ "token": "<jwt>", "expiresIn": 86400 }
```

---

## 2. Jobs (`workly-Server`)
Base URL: `http://localhost:8080/api/v1/jobs`

All endpoints require authentication. The mobile number is derived from the JWT — callers never pass it explicitly.

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/jobs` | Seeker | Create a new job. Returns **201 Created**. |
| GET | `/jobs` | Seeker | List seeker's own jobs (`?type=active\|past`, `?page`, `?size`) |
| GET | `/jobs/seeker` | Seeker | Same as `GET /jobs` (explicit alias) |
| GET | `/jobs/worker` | Worker | List worker's assigned / past jobs (`?page`, `?size`) |
| GET | `/jobs/available` | Worker | Matching jobs near the worker (`?page`, `?size` max 20) |
| GET | `/jobs/{id}` | Any | Get job details by ID |
| GET | `/jobs/{id}/tracking` | Seeker | Live worker location for an ASSIGNED job |
| PUT | `/jobs/{id}` | Seeker | Reschedule a job (2-hour penalty if ASSIGNED + within 2h) |
| PATCH | `/jobs/{id}/status` | Seeker | Transition job status (`?status=BROADCASTED\|CANCELLED\|…`) |
| POST | `/jobs/{id}/accept` | Worker | Accept a broadcasted job (distributed lock, first-accept wins) |
| POST | `/jobs/{id}/complete` | Worker | Complete a job with OTP verification |

**POST /jobs body**
```json
{
  "title": "Plumbing repair",
  "description": "Fix leaky pipe under kitchen sink",
  "requiredSkills": ["Plumber"],
  "budget": 500.0,
  "immediate": true,
  "preferredDateTime": 0,
  "searchRadiusKm": 10,
  "location": {
    "address": "123 Main St, Mumbai",
    "latitude": 19.076,
    "longitude": 72.877
  }
}
```

**POST /jobs/{id}/complete body**
```json
{ "otp": "4721" }
```

**Status transition rules**
```
CREATED → BROADCASTED | SCHEDULED | CANCELLED
SCHEDULED → BROADCASTED | CANCELLED
BROADCASTED → ASSIGNED | PENDING_ACCEPTANCE | CANCELLED | EXPIRED
PENDING_ACCEPTANCE → ASSIGNED | CANCELLED
ASSIGNED → COMPLETED | CANCELLED
```

---

## 3. Profiles (`workly-Server` / `workly-Profile-Service`)
Base URL: `http://localhost:8080/api/v1` | port 8088 in microservice mode

| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/profile/worker` | Worker | Get own worker profile |
| POST | `/profile/worker` | Worker | Create or update worker profile |
| GET | `/profile/seeker` | Seeker | Get own seeker profile |
| POST | `/profile/seeker` | Seeker | Create or update seeker profile |
| POST | `/profile/worker/location` | Worker | Update GPS location (Redis Geo hot path) |
| POST | `/profile/worker/availability` | Worker | Toggle availability on/off |
| POST | `/users/fcm-token` | Any | Register or refresh FCM device token |

**POST /profile/worker body**
```json
{
  "name": "Ravi Kumar",
  "skills": ["Plumber", "Electrician"],
  "travelRadiusKm": 15,
  "available": true
}
```

---

## 4. Matching (`workly-Server` / `workly-Matching-Service`)
Base URL: `http://localhost:8080/api/v1` | port 8089 in microservice mode

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/matching/workers` | Any | Find available workers matching skills + location |

**POST /matching/workers body**
```json
{
  "requiredSkills": ["Carpenter"],
  "longitude": 72.877,
  "latitude": 19.076,
  "radiusKm": 10,
  "scheduledTimeMillis": 0
}
```
Hot path uses Redis Geo (`GEORADIUS`); falls back to MongoDB `$near` on cache miss.
Results sorted by provider tier (SUPER_PRO > PREMIUM > STANDARD).

---

## 5. Search (`workly-Search-Service`)
Base URL: `http://localhost:8083/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/skills/autocomplete?query={text}` | public | Canonical skill suggestions (exact → fuzzy → phonetic) |
| POST | `/skills/sync` | Admin | Trigger MongoDB → Elasticsearch skill index sync |

---

## 6. Chat (`workly-Chat-Service`)
WebSocket URL: `ws://localhost:8082/ws/chat?userId={mobileNumber}`

Connect with the JWT as a query parameter or `Authorization` header.

**Message format**
```json
{
  "senderId": "9876543210",
  "receiverId": "9123456789",
  "content": "Are you on your way?",
  "type": "TEXT",
  "timestamp": "2024-01-15T10:00:00Z"
}
```

**Events**

| Event | Direction | Description |
|---|---|---|
| onOpen | Server → Client | Connection acknowledged |
| onMessage | Server → Client | Incoming chat message (JSON) |
| onClosing | Server → Client | Server-initiated close |
| onFailure | — | OkHttp reconnect needed (implement exponential backoff) |

Offline delivery: if the recipient is disconnected, the message is published to the `chat-events` Kafka topic and delivered via FCM push.

---

## 7. Configuration (`workly-Config-Service`)
Base URL: `http://localhost:8084/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/config/public` | public | All public config values |
| GET | `/config/features` | public | Feature flag map (`payments`, `sms`) |
| GET | `/configs/{key}?scope={scope}` | Admin | Single config entry with version |
| POST | `/configs` | Admin | Create or update a config entry |
| POST | `/configs/{key}/rollback` | Admin | Roll back to a prior version |
| POST | `/config/sync` | Any | Acknowledge mobile config sync completion |

Config changes propagate to all services within ~10 ms via Redis Pub/Sub (`config_updates` channel).

---

## 8. Admin (`workly-Server`)
Base URL: `http://localhost:8080/api/v1/admin`

> **All admin endpoints require an admin JWT** (issued by `POST /admin/auth/login`).

| Method | Path | Description |
|---|---|---|
| POST | `/admin/auth/login` | Admin login; returns JWT with `role=ADMIN` claim |
| POST | `/admin/auth/change-password` | Change admin account password |
| GET | `/admin/seekers` | Paginated seeker list |
| GET | `/admin/providers` | Paginated worker/provider list |
| GET | `/admin/jobs` | Paginated job list |
| POST | `/admin/reports/custom` | Run a read-only SQL or MongoDB query |
| GET | `/admin/audit-logs` | Audit log trail |

**POST /admin/auth/login**
```json
{ "username": "admin", "password": "changeme" }
```

---

## 9. Live Tracking (`workly-Tracking-Service` / `workly-Server`)
Base URL: `http://localhost:8087/api/v1` | WebSocket `ws://localhost:8080/ws/tracking`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/tracking/location` | Worker | Push GPS update (Redis Geo, batch-flushed to MongoDB every 60 s) |
| WS | `/ws/tracking?jobId={id}&role=PROVIDER\|SEEKER` | Any | Real-time location relay for an ASSIGNED job |

---

## 10. Payments (`workly-Server`)
Base URL: `http://localhost:8080/api/v1/payments`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/payments/intent/{jobId}` | Seeker | Lock funds in escrow (`ESCROW_LOCKED`) |
| GET | `/payments/provider/ledger` | Worker | View pending balance and payout history |

Escrow transitions to `COMPLETED` asynchronously when `JOB_COMPLETED` Kafka event is consumed.
Payment processing is disabled by default (`workly.features.payments.enabled=false`).

---

## 11. Growth & Pricing (`workly-Server`)
Base URL: `http://localhost:8080/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/pricing/estimate?lat=&lon=&baseRate=` | public | Surge multiplier from job density (5 km radius) |
| GET | `/promotions/validate?code=&amount=` | Any | Validate promo code; returns exact discount amount |

---

## 12. Trust & Safety (`workly-Server`)
Base URL: `http://localhost:8080/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/workers/kyc/upload` | Worker | Upload ID documents (multipart) for automated KYC |
| PUT | `/reviews/{reviewId}/dispute` | Any | Flag a review and open a dispute ticket |

---

## HTTP Status Codes

| Code | Meaning |
|---|---|
| 200 | Success |
| 201 | Resource created (job creation) |
| 400 | Bad request / invalid input |
| 401 | Missing or invalid JWT |
| 403 | Valid JWT but insufficient role |
| 404 | Resource not found |
| 409 | Conflict (e.g. job already accepted) |
| 429 | Rate limit exceeded |
| 500 | Internal server error |

---

## Error Response Format

All errors use a unified envelope:
```json
{
  "success": false,
  "message": "Job not found",
  "data": null
}
```
