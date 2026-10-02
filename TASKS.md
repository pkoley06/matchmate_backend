# MatchMate: Implementation Task Checklist & Execution Plan

This document tracks the step-by-step implementation tasks for MatchMate, covering both the **Java 25 / Spring Boot 3.x backend** and the **Flutter mobile frontend**.

---

## Progress Overview

| Milestone | Focus Area | Status | Target Completion |
| :--- | :--- | :---: | :---: |
| **M01** | Database Baseline & Spatial Persistence (PostGIS) | ⏳ Pending | Sprint 1 |
| **M02** | Authentication Handshake & Stateless Security (Firebase/JWT) | ⏳ Pending | Sprint 1 |
| **M03** | Deterministic Lifestyle Matching Engine | ⏳ Pending | Sprint 1 |
| **M04** | Spatial Candidate Discovery Feed Service | ⏳ Pending | Sprint 2 |
| **M05** | Listing Management & S3/R2 Presigned Media | ⏳ Pending | Sprint 2 |
| **M06** | Flutter Foundation, Riverpod & Network Layer | ⏳ Pending | Sprint 3 |
| **M07** | Flutter Onboarding & Profile Gamification | ⏳ Pending | Sprint 3 |
| **M08** | Flutter Discovery Feed & Geolocation Engine | ⏳ Pending | Sprint 4 |
| **M09** | Flutter Listing Creation & Interactive Map Pinning | ⏳ Pending | Sprint 4 |
| **M10** | End-to-End Testing, Dockerization & CI/CD | ⏳ Pending | Sprint 5 |

---

## Milestone 1: Database Baseline & Spatial Persistence (Backend)

- [ ] **Task 1.1: PostGIS & Core Users Migration**
  - **File:** `src/main/resources/db/migration/V1__init_schema.sql`
  - **Details:** Ensure `CREATE EXTENSION IF NOT EXISTS postgis;` is included. Create `app_users` table with `GEOGRAPHY(Point, 4326)` for `last_known_location` and GiST index `idx_app_users_location`.
  - **Acceptance Criteria:** `docker compose up -d postgres` and `./gradlew flywayMigrate` (or app boot) creates extension and `app_users` table without syntax errors.

- [ ] **Task 1.2: Listings Migration**
  - **File:** `src/main/resources/db/migration/V2__create_listings_schema.sql`
  - **Details:** Create `listings` table with foreign key `host_user_id -> app_users(id)`, room financials, `GEOGRAPHY(Point, 4326)` column, `photo_urls` text array, and GiST spatial index.
  - **Acceptance Criteria:** Querying `\d listings` in `psql` shows foreign key cascade and spatial GiST index.

- [ ] **Task 1.3: JPA Entities & Hibernate Spatial Types**
  - **Files:**
    - `src/main/java/com/matcher/matchmate_backend/model/entity/AppUser.java`
    - `src/main/java/com/matcher/matchmate_backend/model/entity/Listing.java`
  - **Details:** Map entities using Hibernate 6/7 spatial types (`org.locationtech.jts.geom.Point`), JPA enums, audit timestamps (`@CreationTimestamp`, `@UpdateTimestamp`).
  - **Acceptance Criteria:** Spring Boot starts with `spring.jpa.hibernate.ddl-auto: validate` without mapping discrepancies.

---

## Milestone 2: Authentication Handshake & Stateless Security (Backend)

- [ ] **Task 2.1: Firebase Admin SDK Integration**
  - **Files:**
    - `build.gradle` (Add `com.google.firebase:firebase-admin:9.3.0` if not present)
    - `src/main/java/com/matcher/matchmate_backend/config/FirebaseConfig.java`
  - **Details:** Initialize `FirebaseApp` using credentials from environment variable or resource file `firebase-service-account.json`. Provide a stub/mock bean for testing.
  - **Acceptance Criteria:** Spring context loads `FirebaseAuth` bean reliably.

- [ ] **Task 2.2: Stateless JWT Security Filter & Claims Provider**
  - **Files:**
    - `src/main/java/com/matcher/matchmate_backend/security/JwtTokenProvider.java`
    - `src/main/java/com/matcher/matchmate_backend/security/JwtAuthenticationFilter.java`
    - `src/main/java/com/matcher/matchmate_backend/config/SecurityConfig.java`
  - **Details:** Implement JWT generation and signing with HMAC256. Filter extracts Bearer token, validates signature/expiration, and populates `SecurityContextHolder`.
  - **Acceptance Criteria:** Requests to protected routes without a valid JWT return HTTP 401 Unauthorized.

- [ ] **Task 2.3: Token Verification & User Onboarding API**
  - **Files:**
    - `src/main/java/com/matcher/matchmate_backend/controller/AuthController.java`
    - `src/main/java/com/matcher/matchmate_backend/service/AuthService.java`
    - `src/main/java/com/matcher/matchmate_backend/dto/request/TokenVerifyRequest.java`
    - `src/main/java/com/matcher/matchmate_backend/dto/response/AuthTokenResponse.java`
  - **Details:** Implement `POST /api/v1/auth/verify-token`. Verifies Firebase ID Token, auto-creates or fetches `AppUser`, and returns internal MatchMate JWT.
  - **Acceptance Criteria:** Integration test passes verifying new user auto-provisioning and existing user token refresh.

- [ ] **Task 2.4: Bare Essentials Demographics Endpoint**
  - **Files:**
    - `src/main/java/com/matcher/matchmate_backend/controller/UserController.java`
    - `src/main/java/com/matcher/matchmate_backend/service/UserService.java`
    - `src/main/java/com/matcher/matchmate_backend/dto/request/DemographicsRequest.java`
  - **Details:** Implement `PUT /api/v1/users/me/demographics`. Validates $Age \ge 18$, saves gender, name, avatar, and returns initial profile strength (40%).
  - **Acceptance Criteria:** Attempting to submit DOB of user younger than 18 returns HTTP 400 with descriptive error.

---

## Milestone 3: Deterministic Matching Engine (Backend)

- [ ] **Task 3.1: Questionnaire Enums & Domain Models**
  - **Files:**
    - `src/main/java/com/matcher/matchmate_backend/model/enums/CleanlinessLevel.java`
    - `src/main/java/com/matcher/matchmate_backend/model/enums/SleepSchedule.java`
    - `src/main/java/com/matcher/matchmate_backend/model/enums/SmokingHabit.java`
    - `src/main/java/com/matcher/matchmate_backend/model/enums/DietaryPreference.java`
    - `src/main/java/com/matcher/matchmate_backend/model/enums/GuestTolerance.java`
  - **Details:** Define ordinal normalized scales and string mappings.

- [ ] **Task 3.2: Algorithmic Matching Heuristic Engine**
  - **File:** `src/main/java/com/matcher/matchmate_backend/service/MatchingHeuristicEngine.java`
  - **Details:**
    - Smoking Dealbreaker check (Non-smoker vs Regular smoker = 0%).
    - Dimension weighting: Cleanliness ($w=2.0$), Sleep ($w=1.5$), Guest ($w=1.2$), Diet ($w=1.0$).
    - Weighted formula: $\text{Similarity} = \max(0, 100 - (\frac{\text{Distance}}{5.7} \times 100))$.
    - Compatibility highlight chips generation (e.g. "Both Night Owls", "Both Non-Smokers").
  - **Acceptance Criteria:** 100% unit test branch coverage for all edge combinations (dealbreaker, full match, partial match, incomplete vectors).

- [ ] **Task 3.3: Progressive Lifestyle Vector Endpoint**
  - **Files:**
    - `src/main/java/com/matcher/matchmate_backend/controller/UserController.java`
    - `src/main/java/com/matcher/matchmate_backend/dto/request/LifestyleRequest.java`
  - **Details:** Implement `PUT /api/v1/users/me/lifestyle`. Computes dynamic profile strength (40% to 100%).
  - **Acceptance Criteria:** Successfully updates lifestyle vector and returns updated strength percentage.

---

## Milestone 4: Spatial Discovery Feed Service (Backend)

- [ ] **Task 4.1: PostGIS Native Spatial Query in Repository**
  - **File:** `src/main/java/com/matcher/matchmate_backend/repository/ListingRepository.java`
  - **Details:** Native query utilizing `ST_DWithin` and `ST_Distance` on `GEOGRAPHY(Point, 4326)` with geodesic meter conversion and distance sorting.
  - **Acceptance Criteria:** Spatial query correctly retrieves listings strictly within specified `radiusKm` bounding sphere.

- [ ] **Task 4.2: Feed Assembly & Compatibility Ranking**
  - **File:** `src/main/java/com/matcher/matchmate_backend/service/DiscoveryService.java`
  - **Details:** Combines spatial query results with `MatchingHeuristicEngine`. Handles missing coordinates by falling back to Central Kolkata (`88.3639, 22.5726`). Applies gender, budget, and age filters.
  - **Acceptance Criteria:** Response items contain calculated `distanceKm`, `matchPercentage`, and `compatibilityHighlights`.

- [ ] **Task 4.3: Discovery Controller API**
  - **File:** `src/main/java/com/matcher/matchmate_backend/controller/DiscoveryController.java`
  - **Details:** Implement `GET /api/v1/discovery` with pagination (`Pageable`), filter query params, and validation.
  - **Acceptance Criteria:** Requesting feed returns paginated candidates with HTTP 200 OK.

---

## Milestone 5: Listing Management & S3/R2 Presigned Media (Backend)

- [ ] **Task 5.1: S3 / Cloudflare R2 Presigned Upload Service**
  - **Files:**
    - `src/main/java/com/matcher/matchmate_backend/service/MediaStorageService.java`
    - `src/main/java/com/matcher/matchmate_backend/controller/MediaController.java`
  - **Details:** Generate secure, 10-minute expiring presigned PUT URLs via AWS S3 SDK for direct mobile client upload.
  - **Acceptance Criteria:** Mobile client can upload binary image directly using the returned presigned URL.

- [ ] **Task 5.2: Listing Creation & Management CRUD**
  - **Files:**
    - `src/main/java/com/matcher/matchmate_backend/controller/ListingController.java`
    - `src/main/java/com/matcher/matchmate_backend/service/ListingService.java`
  - **Details:** Implement `POST /api/v1/listings`, `GET /api/v1/listings/{id}`, and `GET /api/v1/listings/my`. Validate coordinates and save S3 image URLs.
  - **Acceptance Criteria:** Newly created listing appears in subsequent geospatial discovery feed queries.

---

## Milestone 6: Flutter Foundation, Riverpod & Network Layer (Frontend)

- [ ] **Task 6.1: Initialize Flutter App Shell**
  - **Directory:** `matchmate_flutter/`
  - **Details:** Setup Flutter project (`com.matcher.matchmate`), configure `pubspec.yaml` with `flutter_riverpod`, `dio`, `flutter_secure_storage`, `geolocator`, `google_maps_flutter`, and `go_router`.
  - **Acceptance Criteria:** App compiles and launches on Android and iOS devices/emulators.

- [ ] **Task 6.2: Core Network Client & Auth Interceptor**
  - **Directory:** `matchmate_flutter/lib/core/network/`
  - **Details:** Configure Dio singleton with logging, base URL, connect/read timeouts, and interceptor attaching `Authorization: Bearer <jwt>`.
  - **Acceptance Criteria:** All API calls automatically include token from `FlutterSecureStorage`.

- [ ] **Task 6.3: Firebase Authentication & Token Exchange**
  - **Directory:** `matchmate_flutter/lib/features/auth/`
  - **Details:** Implement Welcome Screen, Google Sign-In button, and Phone OTP verification. Call backend `/api/v1/auth/verify-token` and store JWT.
  - **Acceptance Criteria:** Successful Google/Phone login redirects to demographics (if new) or feed (if profile complete).

---

## Milestone 7: Flutter Onboarding & Profile Gamification (Frontend)

- [ ] **Task 7.1: Bare Essentials 20-Second Onboarding Form**
  - **Directory:** `matchmate_flutter/lib/features/onboarding/`
  - **Details:** Screen with 4 inputs: Full Name, Gender choice chips, DOB picker (with client-side age $\ge 18$ validation), and Avatar picker.
  - **Acceptance Criteria:** Under-18 DOB displays inline error; valid submission calls `/api/v1/users/me/demographics` and advances.

- [ ] **Task 7.2: Gamified Profile Tab & Strength Meter**
  - **Directory:** `matchmate_flutter/lib/features/profile/`
  - **Details:** Profile screen displaying circular "Profile Strength" meter (40% - 100%) and 5 interactive lifestyle question cards.
  - **Acceptance Criteria:** Tapping a card updates the lifestyle vector and animates the progress meter up to 100%.

---

## Milestone 8: Flutter Discovery Feed & Geolocation Engine (Frontend)

- [ ] **Task 8.1: Location Resolution Provider**
  - **Directory:** `matchmate_flutter/lib/features/discovery/presentation/providers/`
  - **Details:** Implement multi-tier coordinate provider:
    1. GPS (via `geolocator`)
    2. Google Places Autocomplete search bar
    3. Fallback Central Kolkata (`22.5726, 88.3639`)
  - **Acceptance Criteria:** Denying GPS displays notification banner and seamlessly renders Kolkata flatmates.

- [ ] **Task 8.2: Discovery Feed & Compatibility Cards UI**
  - **Directory:** `matchmate_flutter/lib/features/discovery/presentation/widgets/`
  - **Details:** Scrollable feed displaying flatmate/room cards with image carousel, rent, distance badge, compatibility percentage badge, and highlight chips.
  - **Acceptance Criteria:** Cards render cleanly with smooth image loading and placeholder monograms.

- [ ] **Task 8.3: Filter Drawer Widget**
  - **Directory:** `matchmate_flutter/lib/features/discovery/presentation/widgets/filter_drawer.dart`
  - **Details:** Slide-out drawer with radius slider (2 - 50 km), gender chips, budget range slider, and smoking/dietary dealbreaker toggles.
  - **Acceptance Criteria:** Tapping "Apply Filters" refreshes feed with matching criteria.

---

## Milestone 9: Flutter Listing Creation & Map Pinning (Frontend)

- [ ] **Task 9.1: Direct Presigned Media Upload**
  - **Directory:** `matchmate_flutter/lib/features/listings/`
  - **Details:** Multi-image picker using `image_picker`. Obtains presigned upload URL from backend and uploads directly via Dio PUT.
  - **Acceptance Criteria:** Images upload smoothly with upload progress indicators.

- [ ] **Task 9.2: Interactive Map Location Picker**
  - **Directory:** `matchmate_flutter/lib/features/listings/presentation/widgets/`
  - **Details:** Draggable Google Maps marker allowing host to pinpoint exact room location. Reverse-geocodes locality and city name.
  - **Acceptance Criteria:** Marker position updates latitude/longitude form fields.

- [ ] **Task 9.3: Listing Creation Form**
  - **Directory:** `matchmate_flutter/lib/features/listings/presentation/`
  - **Details:** Form capturing rent, deposit, room type (private/shared), furnishing status, and description.
  - **Acceptance Criteria:** Submitting form calls `POST /api/v1/listings` and shows success confirmation.

---

## Milestone 10: End-to-End Testing & CI/CD Pipeline

- [ ] **Task 10.1: Backend Integration Tests with Testcontainers**
  - **Directory:** `src/test/java/com/matcher/matchmate_backend/`
  - **Details:** Spin up PostgreSQL PostGIS container via Testcontainers. Run complete auth, spatial discovery, and lifestyle matching test suite.
  - **Acceptance Criteria:** `./gradlew test` passes 100% without external database dependencies.

- [ ] **Task 10.2: Flutter Unit & Widget Tests**
  - **Directory:** `matchmate_flutter/test/`
  - **Details:** Test Riverpod notifiers, mock network calls via Mockito/Mocktail, and run golden widget tests.
  - **Acceptance Criteria:** `flutter test` completes with 0 errors.

- [ ] **Task 10.3: Multi-Stage Production Dockerfile & Compose**
  - **Files:** `Dockerfile`, `docker-compose.yml`
  - **Details:** Multi-stage build for Spring Boot application (Java 25 eclipse-temurin / amazoncorretto) with healthchecks and compose orchestration.
  - **Acceptance Criteria:** `docker compose up --build` brings up database and backend in under 60 seconds.
