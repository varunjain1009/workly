# Phase 5: Microservice Decomposition

## Story 1: Foundation (`workly-Common`)
- [ ] Create `workly-Common` gradle module
- [ ] Move `ApiResponse`, exception handlers, and basic DTOs/Entities
- [ ] Move Security, JWT utilities, and filters
- [ ] Wire dependencies into `workly-Server`, `workly-Chat-Service`, `workly-Search-Service`
- [ ] Run build and confirm no compilation errors
- [ ] Commit "feat(scale): extract workly-Common shared library"

## Story 2: Extract Auth Service (`workly-Auth-Service`)
- [ ] Create `workly-Auth-Service` module
- [ ] Move `auth`, `verification` and OTP services
- [ ] Commit "feat(scale): extract Auth Service"

## Story 3: Introduce API Gateway (`workly-Gateway`)
- [ ] Create `workly-Gateway` module using Spring Cloud Gateway
- [ ] Configure routes for Auth and Server fallback
- [ ] Update `docker-compose.yml` for Gateway
- [ ] Commit "feat(scale): add Spring Cloud Gateway"

## Story 4: Extract Tracking & Notification Services
- [ ] Create `workly-Notification-Service`
- [ ] Create `workly-Tracking-Service`
- [ ] Move FCM and WebSocket tracking logic
- [ ] Commit "feat(scale): extract Notification and Tracking Services"

## Story 5: Extract Profile & Matching Service
- [ ] Create `workly-Profile-Service`
- [ ] Create `workly-Matching-Service`
- [ ] Commit "feat(scale): extract Profile and Matching Services"

## Story 6: Documentation and Final Verification
- [ ] Update `README.md`
- [ ] Update `ARCHITECTURE.md`
- [ ] Commit "docs(scale): update architecture for microservices"
