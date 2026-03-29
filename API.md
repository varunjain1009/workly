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
*   `PUT /jobs/{id}/status`: Update job status (e.g., STARTED, COMPLETED).

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

## 5. Upcoming APIs (Roadmap)

In support of the [Product Roadmap](PRODUCT_ROADMAP.md), the following endpoints are planned:

### Trust & Safety (`workly-Server`)
*   `POST /provider/kyc/upload`: Upload ID documents for automated verification.
*   `POST /reviews/{jobId}`: Submit a dual-sided rating/review after job completion.
*   `PUT /reviews/{reviewId}/dispute`: Flag a review or open a dispute ticket.

### Payments (`workly-Payment-Service` - Future)
*   `POST /payments/intent`: Create a secure payment intent for a job locking in escrow.
*   `POST /payments/webhook`: Webhook endpoint for the 3rd-party payment gateway to confirm capture.
*   `GET /provider/payouts`: View pending ledger balance and payout history.
