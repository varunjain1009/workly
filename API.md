# API Reference

## 1. Core API (`workly-Server`)
Base URL: `http://localhost:8080/api/v1`

### Authentication
*   `POST /auth/send-otp`: Request OTP for mobile number.
*   `POST /auth/verify-otp`: Verify OTP and get JWT.
*   `POST /auth/refresh`: Refresh session.

### Jobs
*   `POST /jobs`: Create a new job.
*   `GET /jobs`: List jobs (User/Worker context).
*   `GET /jobs/{id}`: Get job details.
*   `GET /jobs/{id}/tracking`: Fetch live LocationETA of ASSIGNED provider.
*   `PUT /jobs/{id}`: Reschedule a job (subject to 2-hour penalty matrix).
*   `PATCH /jobs/{id}/status`: Update job status (e.g., STARTED, CANCELLED, COMPLETED).

### User
*   `POST /users/fcm-token`: Register FCM token for push notifications.

---

## 2. Search API (`workly-Search-Service`)
Base URL: `http://localhost:8083/api/v1`

### Skills
*   `GET /skills/autocomplete?query={text}`
    *   **Returns**: List of canonical skill names.
    *   **Logic**: Exact > Fuzzy > Phonetic.
*   `POST /skills/sync` (Admin): Trigger Mongo -> Elasticsearch sync.

---

## 3. Chat Protocol (`workly-Chat-Service`)
URL: `ws://localhost:8082/ws/chat?userId={uid}`

### Message Format (JSON)
```json
{
  "senderId": "user_123",
  "receiverId": "user_456",
  "content": "Hello, are you available?",
  "type": "TEXT",
  "timestamp": "2023-12-30T10:00:00Z"
}
```

### Events
*   **On Connect**: Server acknowledges connection.
*   **On Message**: Server delivers message to receiver's socket.
*   **On Disconnect**: Server updates presence (future scope).

---

## 4. Config Service API (`workly-Config-Service`)
Base URL: `http://localhost:8084/api/v1`

### Get Active Config
*   `GET /configs/{key}?scope={scope}`
    *   **Response**: `{"key": "MAX_RADIUS", "value": "100", "version": 2}`

### Create/Update Config
*   `POST /configs`
    *   **Params**: `key`, `value`, `scope`, `adminId`

### Rollback
*   `POST /configs/{key}/rollback`
    *   **Params**: `version`, `scope`, `adminId`

---

## 5. Live Tracking WebSocket
URL: `ws://localhost:8080/ws/tracking?jobId={jobId}&role={PROVIDER|SEEKER}`

### Message Relaying
*   Providers stream raw coordinates.
*   Seekers listen and render Map UI real-time.

---

## 6. Trust & Safety APIs
Base URL: `http://localhost:8080/api/v1`

### KYC Verification
*   `POST /workers/kyc/upload`: Upload ID documents for automated verification (multipart form).

### Reporting & Disputes
*   `PUT /reviews/{reviewId}/dispute`: Flag a review and open a dispute ticket.

---

## 7. Mock Payment Ledger API
Base URL: `http://localhost:8080/api/v1`

### Escrow
*   `POST /payments/intent/{jobId}`: Create a secure payment intent for a job locking funds in escrow (`ESCROW_LOCKED`).
*   Automatically transitions to `COMPLETED` asynchronously on `JOB_COMPLETED` Kafka pulse.

### Ledger
*   `GET /payments/provider/ledger`: View pending ledger balance and mock payout history for authenticated Worker.

---

## 8. Growth & Pricing APIs
Base URL: `http://localhost:8080/api/v1`

### Surge Estimator
*   `GET /pricing/estimate?lat={lat}&lon={lon}&baseRate={baseRate}`: Returns algorithmic surge multiplier based on active job density within a 5km radius.

### Promo Validation
*   `GET /promotions/validate?code={code}&amount={amount}`: Validates string promo codes against active campaigns returning exact mathematical discount markdown safely.
