# Phase 6 — Database Sharding & Advanced Caching

> Goal: Push capacity from ~1M users (post-Phase 5) toward Ola/Uber-level scale by sharding hot
> collections by region, migrating the matching hot-path to Redis Geo, and adding secondary read
> routing + layered caching throughout.

---

## Story 6.1 — Region Tagging Infrastructure
**Status:** completed

Add a `region` field (1°×1° lat/lon grid cell, e.g. `"19_72"`) to `Job` and `WorkerProfile`.
Populate it automatically on job creation and on every MongoDB location flush.
This field will become the MongoDB shard key in Story 6.2.

**Files changed:**
- `workly-Server/.../modules/job/Job.java` — add `region` field
- `workly-Server/.../modules/profile/WorkerProfile.java` — add `region` field
- `workly-Server/.../core/RegionHelper.java` (new) — derive region from lat/lon
- `workly-Server/.../modules/job/JobService.java` — call `RegionHelper` on create
- `workly-Server/.../modules/location/LocationService.java` — call `RegionHelper` on flush

**Commit:** `feat(scale): add region tagging to Job and WorkerProfile for shard key`

---

## Story 6.2 — Shard-Key Compound Indexes
**Status:** completed

Add compound indexes aligned with the MongoDB shard keys and the most common query shapes:
- `jobs`: `{ region: 1, status: 1, requiredSkills: 1 }` and `{ region: 1, location: "2dsphere" }`
- `worker_profiles`: `{ region: 1, available: 1, skills: 1 }` and `{ region: 1, lastLocation: "2dsphere" }`

Provide a `mongo-shard-init.js` script with the `sh.shardCollection()` commands ready to run
against a sharded cluster.

**Files changed:**
- `workly-Server/.../modules/job/Job.java` — `@CompoundIndex` annotations
- `workly-Server/.../modules/profile/WorkerProfile.java` — `@CompoundIndex` annotations
- `docker/mongo-shard-init.js` (new) — shard key + zone commands

**Commit:** `feat(scale): add compound indexes and shard-init script for region-based sharding`

---

## Story 6.3 — Redis Geo as Primary Matching Source
**Status:** completed

`MatchingService.findMatches()` currently always queries MongoDB `$near`.
Migrate the hot path to Redis `GEORADIUS` (already written by `LocationService`),
falling back to MongoDB only when Redis returns an empty set (cold start / cache miss).

This makes matching sub-millisecond for workers already tracked in Redis Geo.

**Files changed:**
- `workly-Server/.../modules/matching/MatchingService.java` — Redis Geo primary, MongoDB fallback
- `workly-Matching-Service/.../matching/service/MatchingService.java` — same change in extracted service

**Commit:** `perf(scale): use Redis Geo as primary source for worker matching with MongoDB fallback`

---

## Story 6.4 — MongoDB Secondary Read Preference
**Status:** completed

Route all read-heavy, non-critical queries to MongoDB replica secondaries.
Configure a secondary `MongoTemplate` bean and apply it to `JobRepository` list queries
and `WorkerProfileRepository` find queries.
Primary is still used for all writes and job acceptance.

**Files changed:**
- `workly-Server/.../config/MongoReadConfig.java` (new) — secondary-preferred `MongoTemplate` bean
- `workly-Server/.../modules/job/JobRepository.java` — annotate listing queries with secondary template
- `workly-Server/.../modules/profile/WorkerProfileRepository.java` — annotate read queries

**Commit:** `perf(scale): route read-heavy MongoDB queries to replica secondaries`

---

## Story 6.5 — Layered Job Listing Cache
**Status:** completed

Add a two-level cache for `GET /api/v1/jobs/available` (worker's matching jobs):
- **L1**: Spring `@Cacheable` backed by Redis, key = `jobs:available:{mobileNumber}`, TTL 30s
- **L2**: `@CacheEvict` on `createJob`, `updateJobStatus` (status → ASSIGNED/COMPLETED/CANCELLED)

Also cache `getSeekerJobs` results with key `jobs:seeker:{mobile}:{type}`, TTL 15s.

**Files changed:**
- `workly-Server/.../config/CacheConfig.java` (new or update) — TTL definitions
- `workly-Server/.../modules/job/JobService.java` — `@Cacheable` + `@CacheEvict`

**Commit:** `perf(scale): add Redis-backed cache for job listing endpoints (30s TTL)`

---

## Story 6.6 — Cache & Redis Geo Metrics
**Status:** not started

Expose cache hit/miss counters and Redis Geo set size as Prometheus metrics so the
Grafana dashboard (from SCALE_PLAN Phase 3) can show cache efficiency in real time.

**Files changed:**
- `workly-Server/.../config/CacheMetricsConfig.java` (new) — `MeterRegistry` cache binder
- `workly-Server/.../modules/location/LocationService.java` — emit `location.redis.geo.size` gauge
- `workly-Server/src/main/resources/application.yml` — expose `metrics` actuator endpoint

**Commit:** `feat(observability): expose cache hit-rate and Redis Geo size as Prometheus metrics`

---

## Summary

| Story | What | Impact |
|-------|------|--------|
| 6.1 | Region tagging on Job + WorkerProfile | Shard key foundation |
| 6.2 | Compound indexes + shard-init script | Ready for `mongos` sharding |
| 6.3 | Redis Geo primary matching | Matching latency: ~50ms → <1ms |
| 6.4 | Secondary read routing | 2–3× read throughput on MongoDB cluster |
| 6.5 | Job listing cache (30s TTL) | Eliminates repeated geo queries under load |
| 6.6 | Cache + Geo metrics | Operational visibility for all of the above |
