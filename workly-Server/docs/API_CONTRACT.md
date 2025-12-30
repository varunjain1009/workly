# Workly Server API Contract

All API endpoints are prefixed with `/api/v1`. All requests and responses use `application/json` unless otherwise specified.

---

## 1. Authentication (`/auth`)

Endpoints for user registration and login via OTP.

### POST `/auth/generate-otp`
Generates a 6-digit OTP for the provided mobile number.
- **Request**: `{ "mobileNumber": "1234567890" }`
- **Response**: `ApiResponse<String>` (Success message)

### POST `/auth/verify-otp`
Authenticates the user and returns a JWT.
- **Request**: `{ "mobileNumber": "1234567890", "otp": "123456" }`
- **Response**: `ApiResponse<AuthResponse>`
- **Model**: `AuthResponse { token: string, mobileNumber: string }`

---

## 2. Profiles (`/profiles`)
*Requires Authentication*

### GET `/profiles/worker`
Retrieves current authenticated worker's profile.
- **Response**: `ApiResponse<WorkerProfile>`

### POST `/profiles/worker`
Creates or updates a worker profile.
- **Request**: `WorkerProfile` object.
- **Response**: `ApiResponse<WorkerProfile>`

### GET `/profiles/seeker`
Retrieves current authenticated seeker's profile.
- **Response**: `ApiResponse<SkillSeekerProfile>`

### POST `/profiles/seeker`
Creates or updates a seeker profile.
- **Request**: `SkillSeekerProfile` object.
- **Response**: `ApiResponse<SkillSeekerProfile>`

### PATCH `/profiles/worker/availability`
Toggles worker availability.
- **Query Params**: `available=true|false`
- **Response**: `ApiResponse<Void>`

---

## 3. Jobs (`/jobs`)
*Requires Authentication*

### POST `/jobs`
Creates a new job posting.
- **Request**: `Job` object (seeker details auto-filled from context).
- **Response**: `ApiResponse<Job>`

### GET `/jobs/seeker`
Lists jobs created by the authenticated seeker.
- **Response**: `ApiResponse<List<Job>>`

### GET `/jobs/worker`
Lists jobs assigned to or broadcasted to the authenticated worker.
- **Response**: `ApiResponse<List<Job>>`

### PATCH `/jobs/{jobId}/status`
Updates job status (e.g., to `IN_PROGRESS` or `COMPLETED`).
- **Query Params**: `status=ENUM_VALUE`
- **Response**: `ApiResponse<Job>`

---

## 4. Monetization (`/monetization`)
*Requires Authentication*

### GET `/monetization/status`
Checks if the user has an active subscription or if monetization is bypassed.
- **Response**: `ApiResponse<Boolean>` (true if authorized)

### POST `/monetization/subscribe`
Enrolls the user in a subscription plan.
- **Request**: `{ "planType": "string", "durationDays": number }`
- **Response**: `ApiResponse<Subscription>`

---

## 5. Verification (`/verification`)
*Requires Authentication*

### POST `/verification/job/complete`
Completes a job using the OTP provided by the seeker to the worker.
- **Request**: `{ "jobId": "string", "otp": "string" }`
- **Response**: `ApiResponse<Void>`

---

## Standard Response Format
All responses wrap data in a standard `ApiResponse` envelope:
```json
{
  "success": true,
  "message": "Action completed successfully",
  "data": { ... }
}
```
In case of error:
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```
