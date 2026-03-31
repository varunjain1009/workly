# Workly Platform — Bug Fixes & Chat Feature

Four distinct issues have been identified across the backend and both Android apps. The plan is split into four sequential phases, each ending with a git commit.

---

## Issues Diagnosed

| # | Bug | Root Cause |
|---|-----|------------|
| 1 | Jobs appear late on provider find-job screen | Throttle cache applies even on first load; `getMatchingJobs()` fetches unordered |
| 2 | Accepted job not visible on provider active-jobs screen | `MyJobsFragment` is a stub — no ViewModel, no API call, no RecyclerView |
| 3 | Seeker gets no FCM push when job is accepted | `handleJobStatusUpdated` Kafka listener only logs; never calls `FCMService` |
| 4 | Seeker job-detail shows "----" OTP and missing provider name/phone | `JobDTO` & `toDto()` omit `completionOtp`, `workerName`, `workerMobileNumber` |
| 5 | No chat between provider and seeker | Provider chat code is fully commented-out; `seekerMobileNumber` missing from provider `Job` model |

---

## Phase 1 — Backend Fixes
> **Commit:** `fix(backend): add worker details + OTP to JobDTO, send FCM on job accepted`

### 1.1 [MODIFY] `JobDTO.java`
Add missing fields:
- `completionOtp` (String)
- `workerName` (String)
- `workerMobileNumber` (String)
- `seekerMobileNumber` (String) — needed for provider chat

### 1.2 [MODIFY] `JobController.java` — `toDto()` method
- Populate `completionOtp` from `job.getCompletionOtp()`
- Populate `seekerMobileNumber` from `job.getSeekerMobileNumber()`
- Look up the `WorkerProfile` via `profileService.getWorkerProfile(job.getWorkerMobileNumber())` and populate `workerName` and `workerMobileNumber`

### 1.3 [MODIFY] `NotificationService.java` — `handleJobStatusUpdated()`
- Inject `FCMService` and `UserTokenService`
- When `event.getStatus() == JobStatus.ASSIGNED`:
  - Load the job from `jobRepository`
  - Look up the seeker's FCM token via `userTokenService.getToken(job.getSeekerMobileNumber())`
  - Call `fcmService.sendNotification(token, "Job Accepted", "Your job has been accepted by a provider")` with additional data payload `{ jobId, type: "JOB_ACCEPTED" }`
- Also handle `JOB_COMPLETED` to notify seeker

### 1.4 [MODIFY] `FCMService.java`
- Add `sendNotificationWithData(String token, String title, String body, Map<String,String> data)` overload so `jobId` can be sent in the data payload (enables deep-linking on the notification tap)

### 1.5 [MODIFY] `JobRepository.java`
- Add `findByWorkerMobileNumberOrderByCreatedDateDesc(String mobile)` to return provider's jobs newest-first (uses `MongoBaseEntity.createdDate`)

---

## Phase 2 — Provider App Fixes
> **Commit:** `fix(provider): active-jobs screen, job-appear latency, chat wiring`

### 2.1 [NEW] `MyJobsViewModel.java`
- Inject `JobRepository` (provider)
- Expose `LiveData<List<Job>> myJobs`
- Call `apiService.getWorkerJobs()` on init and on manual refresh
- Sort list by most recent first (newest `id` / creation order)

### 2.2 [MODIFY] `MyJobsFragment.java`
- Replace stub with proper implementation:
  - Bind `MyJobsViewModel`
  - `RecyclerView` with `LinearLayoutManager`
  - Use existing `JobAdapter`
  - `SwipeRefreshLayout` for pull-to-refresh
  - Navigate to `JobDetailsFragment` on item click, passing the job bundle
  - Empty-state text view when no jobs

### 2.3 [MODIFY] Provider `ApiService.java`
- Add `@GET("jobs/worker") Call<ApiResponse<List<Job>>> getWorkerJobs()`

### 2.4 [MODIFY] Provider `Job.java` model
- Add `seekerMobileNumber` (String) field + getter/setter

### 2.5 [MODIFY] `JobDetailsFragment.java` (Provider)
- Uncomment / wire up `btnChatSeeker` visibility and click listener when `job.getStatus() == ASSIGNED`
- Navigate to `ChatFragment` using `job.getSeekerMobileNumber()` as `otherUserId`

### 2.6 Fix job-appear latency
- In `JobRepository.refreshAvailableJobs()`: bypass the throttle on the very first call (`lastFetchTime == 0`) — currently it checks `isCacheFresh()` which returns `false` when `lastFetchTime == 0` (correct), but the `HomeViewModel` constructor already calls `refreshAvailableJobs(false)` which is fine. The real fix is to ensure `HomeViewModel` always does a network call on first creation regardless of throttle — verify the `DEFAULT_STALENESS_MS` is not being triggered erroneously for new sessions.

---

## Phase 3 — Seeker App Fixes
> **Commit:** `fix(seeker): FCM job-accepted push, provider details, OTP display`

### 3.1 [MODIFY] Seeker `Job.java` model
- Add `workerName` (String), `workerMobileNumber` (String) fields with getters/setters (already has `completionOtp`)

### 3.2 [MODIFY] `MyFirebaseMessagingService.java` (Seeker)
- In `onMessageReceived`, handle `type == "JOB_ACCEPTED"`:
  - Extract `jobId` from data payload
  - Post a system notification (`NotificationCompat.Builder`) so the seeker gets an OS-level heads-up notification
  - Broadcast a local intent (`LocalBroadcastManager`) with the `jobId` so any active `HomeFragment` can immediately invalidate the job cache and refresh without a pull

### 3.3 [MODIFY] Seeker `HomeFragment.java`
- Register a `BroadcastReceiver` for the `JOB_ACCEPTED` local intent in `onStart()`/`onStop()`
- On receipt, call `viewModel.invalidateCache("active")` then `viewModel.loadJobs("active", true)` — this gives the seeker instant in-app refresh without pull-down

### 3.4 [MODIFY] Seeker `JobDetailsFragment.java`
- `tvOtp` line: already shows `job.getCompletionOtp()` but falls back to `"----"` when null. This will be fixed once Phase 1 populates `completionOtp` in the DTO.
- Add display of `workerName` and `workerMobileNumber` in the ASSIGNED state card (update binding)

### 3.5 [MODIFY] Seeker `fragment_job_details.xml` layout
- Add `tvWorkerName` and `tvWorkerPhone` TextViews inside the existing OTP/assigned card view so provider details display cleanly

---

## Phase 4 — Chat Integration (Provider-side wiring)
> **Commit:** `feat(chat): enable provider↔seeker chat for assigned jobs`

### 4.1 [MODIFY] Provider `navigation/nav_graph.xml`
- Verify/add action `action_jobDetails_to_chatFragment` pointing to `ChatFragment`
- Pass `otherUserId` (seekerMobileNumber) as argument

### 4.2 [MODIFY] Provider `JobDetailsFragment.java`
- Finalize `startChat()` method using `job.getSeekerMobileNumber()`
- Show `btnChatSeeker` only when `job.getStatus() == ASSIGNED`

### 4.3 [MODIFY] Provider `fragment_job_details.xml`
- Ensure `btnChatSeeker` button exists (may need to add/unhide it)

---

## Verification Plan

### Phase 1
- Start backend, accept a job, call `GET /api/v1/jobs/{id}` and confirm `completionOtp`, `workerName`, `workerMobileNumber` appear in the response
- Check backend logs for FCM mock-send on `JOB_ACCEPTED` event

### Phase 2
- Navigate to provider "My Jobs" tab after accepting; verify the job appears immediately, newest first

### Phase 3
- Accept a job as provider while seeker app is open; verify seeker gets a heads-up notification and the active-jobs list auto-refreshes
- Open the assigned job in seeker app; verify OTP shows a 4-digit code and provider name/phone are shown

### Phase 4
- While an ASSIGNED job is open in provider app, tap "Chat with Seeker"; verify navigation to ChatFragment works

---

## Open Questions

> [!IMPORTANT]
> The `MongoBaseEntity` must have a `createdDate` field for date-based sorting to work. Please confirm.

> [!NOTE]
> FCM push notifications require the device to have a registered token (registered at login time via `updateDeviceToken`). If the seeker has never logged in on the device during this session the notification will silently be skipped — this is expected behaviour for MVP.

> [!WARNING]
> The provider `Job.java` model currently has no `seekerMobileNumber` field. Adding it requires updating the provider `JobAdapter` display logic as well if it renders any seeker-specific info. Please confirm no card layout shows seeker mobile.
