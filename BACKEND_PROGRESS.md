# MatchMate Backend: Implementation Tracker & Step-by-Step Guide

> **Notice**: This document tracks the implementation progress for the MatchMate Spring Boot backend. It maintains context across prompts so you can understand what has been accomplished, what the immediate next step is, which file is used for what, and how to execute it without needing to ask.
>
> **How to use this doc**: You (the developer) type all the code yourself. Every file below has the code **and** a plain-English explanation of what it is, why it exists, and what each class/method does. Anything in the original guide that was wrong or risky has been corrected and is listed in **"Read First: Corrections"** — the code blocks in this doc already contain the corrected versions.
>
> *Last reviewed: 2026-09-20*

---

## 📊 Status Dashboard

| Item | Status | Notes |
| :--- | :--- | :--- |
| **Current Stage** | **Milestone 2: Authentication Handshake & Stateless Security** | Milestone 1 (Database & Entities) is 100% Complete |
| **Current Active Task** | **Step 2.0 → 2.4: Housekeeping, Security, Firebase, JWT & Auth APIs** | User implementation step (you write the code, this doc explains it) |
| **Gradle Build** | 🟢 `BUILD SUCCESSFUL` | Spring Boot 4.1.1 / Java 25 / JJWT 0.13.0 / Firebase Admin 9.3.0 |
| **Database Migrations** | 🟢 Complete (V1, V2) · 🔴 V3 pending | `V1__init_schema.sql` (PostGIS + users), `V2__create_listings_schema.sql` (listings), **`V3__add_onboarding_completed.sql` (new, Step 2.1b)** |
| **JPA Entities & Repos** | 🟢 Complete | `AppUser`, `Listing`, `AppUserRepository`, `ListingRepository` (with PostGIS `Point`). `AppUser` needs one new field in Step 2.1b |
| **Database Container** | 🟢 Configured | `docker-compose.yml` (`postgis/postgis:16-3.4-alpine`) |
| **Firebase Project** | 🟢 Created (`matchmate-ab67a`) | Flutter has `google-services.json` + `firebase.json` (correct files). Backend has service-account JSON(s) — see Step 2.0 |
| **Milestone 2 code files** | 🟡 14 of 14 written · app does not start yet | `bootRun` fails: `Schema validation: missing table [app_users]`. Fix: use `spring-boot-starter-flyway` (Boot 4 no longer runs Flyway from `flyway-core` alone) and create `V3__add_onboarding_completed.sql`. See `learning/11-startup-troubleshooting.md` |

---

## 🔐 Secrets setup (DONE 2026-10-02 — replaces Step 2.0 and the yml in Step 2.1)

Steps 2.0 and 2.1 below tell you to set a Windows environment variable by hand. **That's no longer needed.** It was replaced by a `.env` file that is read automatically. What was changed:

| File | Change | Why |
| :--- | :--- | :--- |
| `.env` *(new, git-ignored)* | Holds `JWT_SECRET` (a **new random** 32-byte key), `DB_*` settings, `FIREBASE_CREDENTIALS_PATH`. | One place for all local secrets. Spring Boot **and** Docker Compose both read it automatically. |
| `.env.example` *(new, committed)* | Same keys with empty/placeholder values + how to generate a secret. | Shows a new machine (or future you) what to fill in. |
| `secrets/firebase-service-account.json` *(moved)* | Was `src/main/resources/firebase/matchmate-ab67a-firebase-adminsdk-fbsvc-020090e50b.json`. The old folder and the copy inside `build/` were deleted. | Files in `src/main/resources` get packed into the jar, and it was not git-ignored. |
| `.gitignore` | Added `.env`, `.env.*` (except `.env.example`), `secrets/`, `src/main/resources/firebase/`, `*firebase-adminsdk*.json`, `*service-account*.json`. | Git can never pick up the secrets, even by accident. |
| `application.yml` | `spring.config.import: optional:file:.env[.properties]`; `jwt.secret: ${JWT_SECRET}` (no default); DB url/user/password from `.env`; Firebase path default `file:./secrets/...`; **`spring.web.error.include-message/include-binding-errors: always`**. | Removes the public sample key (C1) and makes error text reach Flutter (C6). |
| `docker-compose.yml` | `POSTGRES_DB/USER/PASSWORD` now come from `.env` (`${DB_PASSWORD:?...}` fails loudly if missing). | No password written in the compose file. The values were kept identical, so your existing database volume still works. |

**Verified:** `bootRun` starts (Flyway V3, Firebase initialized from `secrets/`), protected routes give 401, and error JSON now contains `"message"`.

**Things to know:**
- **Spring Boot 4 renamed** `server.error.include-message` → `spring.web.error.include-message`. The old key (used in Step 2.1 below) is **silently ignored**. Use the new one.
- `.env` and `./secrets/...` are found **relative to the folder the app is started from**. `gradlew bootRun` uses the project folder automatically. In IntelliJ, make sure the run configuration's *Working directory* is `matchmate_backend`.
- **Real environment variables beat `.env`.** In production you set `JWT_SECRET` etc. as real env vars and don't ship a `.env`.
- If you change `JWT_SECRET`, every JWT issued before becomes invalid (users simply get a new one at next login).
- **Check once in the Firebase console** (⚙ Project settings → Service accounts → *Manage service account permissions* → `firebase-adminsdk-fbsvc@…` → *Keys*): only the key whose ID matches `private_key_id` in `secrets/firebase-service-account.json` should exist. Delete any older extra key (the guide mentioned a second key file that's no longer on disk).

---

## ⚠️ Read First: Corrections to the Original Guide

While checking the project against the guide, these problems were found. The code blocks further down are **already corrected**; this list tells you *what* changed and *why*, so you are not surprised when the code differs from an older copy of the guide.

| # | Problem in the original guide | Fix | Applied in |
| :-: | :--- | :--- | :--- |
| **C1** | **JWT secret was a public sample key** (`404E6352…6B5970`). It is a well-known example string copied from public tutorials, so anyone could forge login tokens for any user. | Generate your own secret, store it in the `JWT_SECRET` environment variable, and read it in `application.yml` with `${JWT_SECRET}`. | Step 2.0 + 2.1 |
| **C2** | **Firebase service-account JSON files are unsafe where they are**: two different private keys sit in `src/main/resources/firebase/` (which gets packed into the jar), and `.gitignore` does not exclude them. | Keep one key, move it to a git-ignored `secrets/` folder, delete the extra key, update `.gitignore`. | Step 2.0 |
| **C3** | **Compile error**: `AuthService` used `Gender.OTHER`, but your enum value is `OTHERS`. | Use `Gender.OTHERS`. | Step 2.3 (`AuthService`) |
| **C4** | **`isProfileComplete` would always be `true`**. The DB requires `gender` and `date_of_birth` (`NOT NULL`), so new users get placeholder values, and the guide's check ("gender and DOB are not null") passes immediately. Flutter would skip onboarding. | Add a real flag: new column `onboarding_completed` (default `false`). `AuthService` reads it; `UserService` sets it to `true` after the onboarding form. | Step 2.1b, 2.3, 2.4 |
| **C5** | **Missing/invalid token returned `403`, not `401`**. Without an "entry point", Spring Security answers 403 Forbidden. `TASKS.md` (Task 2.2) requires 401. | Add `.exceptionHandling(... HttpStatusEntryPoint(UNAUTHORIZED))` to `SecurityConfig`. | Step 2.2 (`SecurityConfig`) |
| **C6** | **Error messages are hidden by default**. Spring Boot does not put our text (e.g. "User must be at least 18 years old") into error JSON, but `TASKS.md` (Task 2.4) requires a *descriptive* 400 error. | Add `server.error.include-message: always` and `include-binding-errors: always` (dev setting). | Step 2.1 (`application.yml`) |
| **C7** | **`FirebaseConfig` had a fake "mock mode" fallback** that does not really work — with no credentials the app still crashes at startup, but with a confusing message. | Fail fast: if the JSON file is missing, stop with a clear message telling you what to fix. | Step 2.1 (`FirebaseConfig`) |

> **Also remember (from C4):** users with `onboarding_completed = false` still hold *placeholder* gender/date-of-birth. Milestone 3/4 (matching + discovery feed) **must exclude them**. This is noted in Milestone 4 below.

---

## 🗺️ Master Implementation Checklist

### Milestone 1: Database Baseline & Spatial Persistence
- [x] **Step 1.0**: Configure `build.gradle` (Dependencies: Spring Security, Validation, Web, JPA, Flyway, PostgreSQL, Hibernate Spatial, Firebase Admin SDK, JJWT 0.13.0).
- [x] **Step 1.1**: PostGIS & Core Users Migration (`V1__init_schema.sql`).
- [x] **Step 1.2**: Listings Migration (`V2__create_listings_schema.sql`).
- [x] **Step 1.3**: JPA Entities, Enums & Spring Data Repositories
  - Enums (`Gender`, `PrimaryAuthProvider`, `OccupationCategory`, `SmokingHabit`, `DietaryPreference`, `SleepSchedule`, `CleanlinessLevel`, `GuestTolerance`, `ListingType`, `RoomType`, `FurnishingStatus`).
  - Entities (`AppUser`, `Listing`) mapped to PostgreSQL via Hibernate Spatial (`Point`).
  - Repositories (`AppUserRepository`, `ListingRepository`) with query derivation.

---

### 👉 Milestone 2: Authentication Handshake & Stateless Security `[CURRENT ACTIVE MILESTONE]`
- [x] **Step 2.0: Security Housekeeping** *(done 2026-10-02 via `.env` + `secrets/`, see "Secrets setup" above)*
  - Generate your own JWT secret and store it in the `JWT_SECRET` environment variable.
  - Move the Firebase service-account JSON into a git-ignored `secrets/` folder, remove the duplicate key, update `.gitignore`.
- [ ] **Step 2.1: Configuration Properties & Firebase Admin Setup** *(fixes C1, C6, C7)*
  - Update `application.yml` (`jwt.*`, `firebase.*` and `server.error.*`). The `jwt`/`firebase` sections already exist but still need the corrections.
  - Create `config/FirebaseConfig.java` to initialize the Google `FirebaseAuth` bean (**currently an empty stub**).
- [ ] **Step 2.1b: `onboarding_completed` column** *(fixes C4)*
  - Create `db/migration/V3__add_onboarding_completed.sql`.
  - Add the `onboardingCompleted` field to `AppUser.java`.
- [ ] **Step 2.2: Stateless JWT Security Infrastructure** *(fixes C5)*
  - Create `security/AuthenticatedUserPrincipal.java` (Spring Security UserDetails representation).
  - Create `security/JwtTokenProvider.java` (JJWT 0.13 token generation, claims extraction, HMAC-SHA256 signing, validation).
  - Create `security/JwtAuthenticationFilter.java` (`OncePerRequestFilter` parsing Bearer token and setting `SecurityContextHolder`).
  - Create `config/SecurityConfig.java` (`SecurityFilterChain`: stateless session, CORS, CSRF disabled, public `/api/v1/auth/**` vs secured routes, **401 for unauthenticated**).
- [ ] **Step 2.3: Token Verification & User Auto-Provisioning Flow** *(fixes C3, C4)*
  - Create DTOs: `TokenVerifyRequest.java`, `AuthTokenResponse.java`, `PrefilledUserData.java`.
  - Create `service/AuthService.java` (verifies Firebase ID Token, auto-provisions `AppUser`, generates MatchMate JWT).
  - Create `controller/AuthController.java` (`POST /api/v1/auth/verify-token`).
- [ ] **Step 2.4: Demographics Onboarding API** *(fixes C4)*
  - Create DTOs: `DemographicsRequest.java` (with Age $\ge 18$ validation), `UserDemographicsResponse.java`.
  - Create `service/UserService.java` (demographics update, profile strength calculation).
  - Create `controller/UserController.java` (`PUT /api/v1/users/me/demographics`).
- [ ] **Step 2.5: Build, Run & Test** (compile, start, manual tests, common errors).

---

### Milestone 3: Deterministic Matching Engine
- [ ] **Step 3.1**: Algorithmic Matching Heuristic Engine (`MatchingHeuristicEngine.java`)
  - Smoking dealbreaker check
  - Distance & weighted similarity calculation ($w_{\text{clean}}=2.0, w_{\text{sleep}}=1.5, w_{\text{guest}}=1.2, w_{\text{diet}}=1.0$)
  - Compatibility highlight chips generator
- [ ] **Step 3.2**: Progressive Lifestyle Vector Endpoint (`PUT /api/v1/users/me/lifestyle`)

---

### Milestone 4: Spatial Discovery Feed Service
- [ ] **Step 4.1**: PostGIS Native Spatial Queries in `ListingRepository` (`ST_DWithin`, `ST_Distance`)
- [ ] **Step 4.2**: Feed Assembly & Compatibility Ranking (`DiscoveryService.java`)
  - ⚠️ **Must exclude users where `onboarding_completed = false`** — they only have placeholder gender/date-of-birth from auto-provisioning (see C4).
- [ ] **Step 4.3**: Discovery Feed API (`GET /api/v1/discovery`)

---

### Milestone 5: Media & Listing Management
- [ ] **Step 5.1**: S3/Cloudflare R2 Presigned Upload Service (`MediaStorageService.java`)
- [ ] **Step 5.2**: Listing CRUD APIs (`POST /api/v1/listings`, `GET /api/v1/listings/{id}`)

---

## 🧠 Mental Model for Milestone 2: Why Each Component Exists & What It Does

Before writing code, here is the complete explanation of how all components fit together.

**The one-sentence idea:** Firebase proves *who the person is* (Google/phone login). Our backend then hands out its *own* small ticket (a JWT) so that every later request is fast and doesn't need to ask Google again.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 1. The Login Handshake Flow                            │
└────────────────────────────────────────────────────────────────────────────────────────┘

[ Flutter App ]
      │
      │ 1. Signs in via Google / Phone SMS OTP
      ▼
[ Firebase Auth ] ──► Returns Firebase ID Token (RS256 JWT from Google)
      │
      │ 2. POST /api/v1/auth/verify-token { idToken, provider: "GOOGLE" }
      ▼
[ SecurityConfig ] (Permits /api/v1/auth/** without JWT)
      ▼
[ AuthController ]
      ▼
[ AuthService ]
      ├──► [ FirebaseConfig / FirebaseAuth ]: Verifies token signature with Google
      ├──► [ AppUserRepository ]: Checks if user exists by firebase_uid
      │       ├── New user? Creates row in app_users (auto-provisions full_name, avatar)
      │       └── Existing user? Loads user from PostgreSQL
      └──► [ JwtTokenProvider ]: Generates MatchMate Session JWT (HMAC-SHA256, userId=1)
      ▼
[ Flutter App ] ◄── Receives { accessToken, userId, isProfileComplete }
                    Saves accessToken in FlutterSecureStorage for all future requests!
```

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              2. Subsequent Authenticated Requests                       │
│                        (e.g., PUT /api/v1/users/me/demographics)                       │
└────────────────────────────────────────────────────────────────────────────────────────┘

[ Flutter App ] ──► Sends HTTP Request with Header: "Authorization: Bearer <accessToken>"
      │
      ▼
[ JwtAuthenticationFilter ] (The Bouncer)
      ├── 1. Extracts "Bearer <accessToken>" from HTTP header
      ├── 2. Calls JwtTokenProvider to validate HMAC signature and expiry
      ├── 3. Extracts userId (e.g. 1) from token claims
      └── 4. Injects AuthenticatedUserPrincipal into Spring's SecurityContextHolder
      │
      ▼
[ SecurityConfig ] (Checks: Is user authenticated? Yes -> Allow through!)
      │
      ▼
[ UserController ] (Accesses authenticated user directly via @AuthenticationPrincipal)
      │
      ▼
[ UserService ] ──► Updates demographics in app_users via AppUserRepository
      │
      ▼
[ Flutter App ] ◄── Receives updated profile (200 OK)
```

### What happens when something is wrong?

| Situation | Who stops it | Result |
| :--- | :--- | :--- |
| Request to a protected route with **no** `Authorization` header | `SecurityConfig` (entry point) | `401 Unauthorized` |
| Token is expired / tampered / signed with another secret | `JwtAuthenticationFilter` ignores it → `SecurityConfig` | `401 Unauthorized` |
| `verify-token` called with a fake/expired **Firebase** token | `AuthService` | `401 Invalid or expired Firebase ID token` |
| Request body fails validation (blank name, DOB in the future…) | `@Valid` on the controller | `400 Bad Request` |
| Date of birth makes the user younger than 18 | `UserService` | `400 User must be at least 18 years old…` |

---

## 📖 Plain-English Glossary (read once, refer back anytime)

| Word | Meaning in simple terms |
| :--- | :--- |
| **Firebase ID Token** | A signed "ticket" Google/Firebase gives the phone after a successful login. It says "this person is Firebase user X". It lives about 1 hour. Our server never trusts it blindly — it asks the Firebase Admin SDK to check the signature. |
| **MatchMate JWT (access token)** | *Our own* ticket, created by our server after it verified the Firebase ticket. Flutter sends it with every later request. Signed with our `jwt.secret`. |
| **JWT** | A string with 3 parts: `header.payload.signature`. The payload holds *claims* (small facts such as "user id = 5, expires at …"). The signature proves nobody changed the payload. **The payload is readable by anyone (not encrypted)** — never put passwords in it. |
| **Claim** | One fact inside the JWT payload (e.g. `sub` = subject = user id, `exp` = expiry time). |
| **HMAC-SHA256 (HS256)** | The signing method: the server mixes the token with a **secret** and produces a signature. Same secret signs and verifies, so the secret must never leave the server. |
| **Bearer token** | The header format `Authorization: Bearer <token>` — "whoever *bears* this token is the user". |
| **Stateless** | The server remembers nothing between requests (no login sessions in memory). Everything it needs is inside the token. |
| **Bean** | An object that Spring creates once and hands to any class that asks for it in its constructor (this is "dependency injection"). |
| **Filter** | Code that runs on every HTTP request *before* it reaches a controller. |
| **Principal** | "The person making this request" as a Java object (`AuthenticatedUserPrincipal`). |
| **SecurityContextHolder** | A per-request notepad where Spring Security remembers who the current user is. |
| **DTO** (Data Transfer Object) | A plain class that defines the shape of JSON going **in** (`...Request`) or **out** (`...Response`). It keeps database entities out of the public API. |
| **Entity** | A Java class mapped to a database table (`AppUser` ↔ `app_users`). |
| **Repository** | The class that talks to the database (`AppUserRepository`). Spring writes the SQL from method names. |
| **Service** | The class holding business rules ("must be 18+", "create the user if new"). |
| **Controller** | The class that receives HTTP requests, calls a service, and returns the HTTP response. |
| **Flyway migration** | A numbered SQL file (`V3__...sql`) that changes the database once, in order. **Never edit a migration that already ran** — add a new one instead. |
| **CSRF** | An attack that abuses browser *cookies*. We use no cookies (token in a header), so we switch it off. |
| **CORS** | A *browser* rule about which websites may call an API. A native Flutter app is not a browser, so it ignores CORS. It matters only if you add a web frontend later. |

---

## 📁 Component Directory & Responsibilities for Milestone 2

| File | Package | What is it? | Why is it needed? |
| :--- | :--- | :--- | :--- |
| `.gitignore` *(edit)* | project root | Git ignore list | Stops secrets (`secrets/`, Firebase keys, `.env`) from ever being committed. |
| `secrets/firebase-service-account.json` *(move, not create)* | project root | Firebase Admin private key | Lets our server prove to Google that it may verify tokens. **Secret — never commit, never put in Flutter.** |
| `application.yml` *(edit)* | `resources/` | Configuration file | Defines JWT secret (from env var), token expiration time, path to Firebase service account JSON, and error-message settings. |
| `V3__add_onboarding_completed.sql` | `resources/db/migration/` | Flyway migration | Adds the `onboarding_completed` column so we know who really finished onboarding (fix C4). |
| `AppUser.java` *(edit)* | `model/entity/` | JPA entity | Gets the matching `onboardingCompleted` field. Needed because `ddl-auto: validate` compares entity ↔ table. |
| `FirebaseConfig.java` | `config/` | Spring `@Configuration` | Creates a Spring `@Bean` for `FirebaseAuth`. Verifies Google Firebase ID tokens sent from the Flutter mobile app. |
| `AuthenticatedUserPrincipal.java` | `security/` | `UserDetails` implementation | A lightweight Java class holding the currently logged-in user's `id`, `firebaseUid`, and roles inside Spring Security context. |
| `JwtTokenProvider.java` | `security/` | Token utility class | Generates, signs, and parses our own fast, lightweight HMAC-SHA256 JWT session tokens using JJWT 0.13.0. |
| `JwtAuthenticationFilter.java` | `security/` | `OncePerRequestFilter` | Runs on every incoming HTTP request. Reads the `Authorization: Bearer <token>` header and authenticates the user in Spring Security. |
| `SecurityConfig.java` | `config/` | Security configuration | Configures the HTTP security filter chain: turns off sessions (stateless), disables CSRF, sets CORS, permits public login routes, secures all other routes, and answers 401 when not logged in. |
| `TokenVerifyRequest.java` | `dto/request/` | Input DTO | Payload sent by Flutter containing the Firebase `idToken` and `provider`. |
| `AuthTokenResponse.java` | `dto/response/` | Output DTO | Payload returned to Flutter containing MatchMate `accessToken`, `userId`, and `isProfileComplete`. |
| `PrefilledUserData.java` | `dto/response/` | Helper DTO | Contains name and avatar extracted from Google to prefill the onboarding screen. |
| `AuthService.java` | `service/` | Business logic | Verifies Firebase ID token, auto-creates or updates the user in PostgreSQL, and generates MatchMate JWT. |
| `AuthController.java` | `controller/` | REST Controller | Exposes `POST /api/v1/auth/verify-token` endpoint. |
| `DemographicsRequest.java` | `dto/request/` | Input DTO | Validates onboarding input (ensuring age $\ge 18$, non-blank full name, valid gender). |
| `UserDemographicsResponse.java` | `dto/response/` | Output DTO | Returns updated profile with calculated age and profile strength percentage. |
| `UserService.java` | `service/` | Business logic | Updates demographics, calculates user age from `dateOfBirth`, and marks onboarding complete. |
| `UserController.java` | `controller/` | REST Controller | Exposes `PUT /api/v1/users/me/demographics`. |

**Base package for all Java files:** `com.matcher.matchmate_backend` → folder `src/main/java/com/matcher/matchmate_backend/`.
**Enum packages that already exist:** `model.enums.auth` (`Gender`, `PrimaryAuthProvider`, `OccupationCategory`) and `model.enums.lifestyle`.

---

## 🎯 Step-by-Step Implementation Guide for Milestone 2

Follow these steps in order. Each step includes the exact code and a plain-English explanation.

---

### Step 2.0: Security Housekeeping *(fixes C1 and C2 — do this before writing any Java)*

#### Part A — Create your own JWT secret (C1)

**Why:** The JWT secret is the "stamp" that makes our tokens unforgeable. The sample value that was in the guide is public, so anyone could stamp their own tokens with it. Your secret must be random and known only to your server.

1. Generate 32 random bytes and print them as Base64 (gives a 44-character string; 256 bits of randomness, which is exactly what HS256 wants):

   ```powershell
   $b = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
   [Convert]::ToBase64String($b)
   ```

2. Save it as an environment variable for your Windows user (replace the placeholder), then **restart IntelliJ and any open terminal** (programs only read environment variables when they start):

   ```powershell
   [Environment]::SetEnvironmentVariable("JWT_SECRET", "PASTE_THE_GENERATED_VALUE_HERE", "User")
   ```

   *Alternative:* IntelliJ → Run/Debug Configurations → your Spring Boot config → *Modify options* → *Environment variables* → `JWT_SECRET=...`.

3. Rules: never write the value in `application.yml`, this doc, chat, or git. Use a **different** value for production.

> **Note:** the code (`JwtTokenProvider`) uses the secret's text bytes directly, and JJWT refuses keys shorter than 32 bytes (`WeakKeyException`). A 44-character Base64 string is 44 bytes, so it works, and JJWT automatically picks HS256 for a key of that size.

#### Part B — Protect the Firebase service-account key (C2)

**Why:** The service-account JSON contains a **private key** that lets anyone act as your Firebase Admin. Two problems existed:
- **Two different private keys** for the same account were in `src/main/resources/firebase/` (`matchmate-ab67a-firebase-adminsdk-fbsvc-020090e50b.json` and `matchmate_firebase_private_key.json`). You only need one.
- Files in `src/main/resources/` are packed **inside the built jar**, and `.gitignore` did not exclude them. This project is not a git repo yet, but **once a key is committed it stays in git history forever** — so fix this *before* your first `git init`/commit.

Steps (from the project root `matchmate_backend/`):

1. **Choose which key to keep.** The guide/`application.yml` pointed to `...-020090e50b.json`, so keep that one. Open both files and note the `private_key_id` of the one you keep.

2. **Move it and delete the duplicate:**

   ```powershell
   New-Item -ItemType Directory -Force secrets | Out-Null
   Move-Item "src\main\resources\firebase\matchmate-ab67a-firebase-adminsdk-fbsvc-020090e50b.json" "secrets\firebase-service-account.json"
   Remove-Item "src\main\resources\firebase\matchmate_firebase_private_key.json"
   Remove-Item "src\main\resources\firebase" -Recurse
   .\gradlew.bat clean
   ```

   `gradlew clean` matters: the `build\` folder still holds a copy of the old file until you clean it.

3. **Revoke the key you deleted** in Google's servers, otherwise it stays valid: Firebase Console → ⚙ Project settings → *Service accounts* → *Manage service account permissions* → click `firebase-adminsdk-fbsvc@matchmate-ab67a.iam.gserviceaccount.com` → *Keys* tab → delete the key whose ID is **not** the one you kept.

4. **Update `.gitignore`** — add at the bottom:

   ```gitignore
   ### Secrets (never commit) ###
   secrets/
   src/main/resources/firebase/
   *firebase-adminsdk*.json
   .env
   ```

5. **What about Flutter?** It is correct as it is: the Flutter project only has `android/app/google-services.json` and `firebase.json` (public client settings for project `matchmate-ab67a`). **Never copy the service-account JSON into the Flutter app** — the app can be unpacked by anyone who downloads it.

---

### Step 2.1: Configuration Properties & Firebase Setup *(fixes C1, C6, C7)*

#### 1. Update `src/main/resources/application.yml`
The `jwt` and `firebase` sections already exist. Change them to match the block below (the **new/changed** lines are marked with `# CHANGED` or `# NEW`):

```yaml
server:
  port: 8080
  error:                                   # NEW (C6) - dev setting
    include-message: always                # show our error text in the JSON (e.g. "User must be at least 18...")
    include-binding-errors: always         # show which field failed @Valid (e.g. "fullName: must not be blank")

spring:
  application:
    name: matchmate-backend

  datasource:
    url: jdbc:postgresql://localhost:5432/matchmate_db
    username: dev_user
    password: matchmate
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# ==========================================
# Security & JWT Configuration
# ==========================================
jwt:
  # CHANGED (C1): read from the JWT_SECRET environment variable. No default on purpose:
  # if you forget to set it, the app refuses to start instead of silently using a weak key.
  secret: ${JWT_SECRET}
  # 86400000 ms = 24 hours
  expiration-ms: 86400000
  issuer: matchmate-backend

# ==========================================
# Firebase Admin SDK Configuration
# ==========================================
firebase:
  # CHANGED (C2): the JSON now lives outside src/main/resources, in the git-ignored secrets/ folder.
  # "file:./secrets/..." is relative to the folder you start the app from (the project root).
  # If your IDE starts the app elsewhere, set FIREBASE_CREDENTIALS_PATH to an absolute path,
  # e.g.  file:D:/Projects/personal/matchmate/matchmate_backend/secrets/firebase-service-account.json
  credentials-path: ${FIREBASE_CREDENTIALS_PATH:file:./secrets/firebase-service-account.json}
```

> **Why this configuration? (line by line)**
> - `server.error.include-message` / `include-binding-errors` *(C6)*: By default Spring Boot hides error text from the JSON reply. With `always`, Flutter gets messages like `"User must be at least 18 years old to use MatchMate."`. This is a **development** convenience; for production you would replace it with a proper global error handler (`@RestControllerAdvice`, planned for the testing milestone).
> - `spring.jpa.hibernate.ddl-auto: validate`: Hibernate does **not** create tables; it only checks that entities match the tables Flyway created. That is why the `onboardingCompleted` field (Step 2.1b) must have a matching migration or the app will not start.
> - `spring.flyway.*`: runs every SQL file in `db/migration` in order at startup.
> - `jwt.secret`: A cryptographically secure 256-bit key used to digitally sign MatchMate tokens so users cannot forge fake user IDs. `${JWT_SECRET}` means "take the value from the environment variable named JWT_SECRET".
> - `jwt.expiration-ms`: Sets tokens to expire after 24 hours (86 400 000 milliseconds).
> - `jwt.issuer`: A label stored in every token saying which system created it.
> - `firebase.credentials-path`: `${NAME:default}` means "use environment variable NAME if it exists, otherwise use the default after the colon". The `file:` prefix tells Spring to read from the normal file system (a `classpath:` prefix would read from inside the jar).

---

#### 2. Create `src/main/java/com/matcher/matchmate_backend/config/FirebaseConfig.java`
*(The file already exists as an empty stub `public class FirebaseConfig {}` — replace its whole content.)*

```java
package com.matcher.matchmate_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final ResourceLoader resourceLoader;

    @Value("${firebase.credentials-path}")
    private String credentialsPath;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        // Already initialized (e.g. after a dev-tools restart or in tests)? Just reuse it.
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseAuth.getInstance();
        }

        if (credentialsPath.isBlank()) {
            throw new IllegalStateException(
                    "firebase.credentials-path is empty. Set FIREBASE_CREDENTIALS_PATH or fix application.yml.");
        }

        Resource resource = resourceLoader.getResource(credentialsPath);
        if (!resource.exists()) {
            // Fail fast (C7): a clear message now is better than a confusing crash later.
            throw new IllegalStateException(
                    "Firebase service account file not found at '" + credentialsPath + "'. "
                    + "Put it there (see Step 2.0) or set the FIREBASE_CREDENTIALS_PATH environment variable.");
        }

        try (InputStream is = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Initialized FirebaseApp with service account: {}", credentialsPath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read Firebase service account file: " + credentialsPath, e);
        }

        return FirebaseAuth.getInstance();
    }
}
```

> **What is this file?** The single place where our server "logs in" to Firebase as an admin. Result: one ready-to-use `FirebaseAuth` object that the rest of the app can use to check tokens.
>
> **Piece by piece:**
> - `@Configuration` — tells Spring "this class contains recipes for creating objects (beans)". Spring runs it once at startup.
> - `log` (`Logger`) — writes messages to the console/log file; better than `System.out.println`.
> - `ResourceLoader resourceLoader` (constructor-injected) — Spring's helper that can open a path like `file:./secrets/x.json` **or** `classpath:x.json` using the same code.
> - `@Value("${firebase.credentials-path}") credentialsPath` — copies the `firebase.credentials-path` value from `application.yml` into this field.
> - `firebaseAuth()` with `@Bean` — the recipe. Spring calls it once and keeps the returned `FirebaseAuth` so `AuthService` can receive it through its constructor. Steps inside:
>   1. **Already initialized?** `FirebaseApp.getApps()` lists Firebase apps in this JVM. Firebase allows only one "default" app, so if it exists we reuse it (this avoids the "FirebaseApp name [DEFAULT] already exists" error on restarts/tests).
>   2. **Blank path / missing file?** Throw `IllegalStateException` with an instruction on how to fix it. The app stops at startup — that is intentional (**fix C7**). The original guide tried a "mock mode" that never really worked and ended in a confusing crash.
>   3. **Read the JSON** (`try-with-resources` closes the file automatically), turn it into `GoogleCredentials`, build `FirebaseOptions`, and call `FirebaseApp.initializeApp(options)`.
>   4. **Return** `FirebaseAuth.getInstance()` — the object with the `verifyIdToken(...)` method.

---

### Step 2.1b: `onboarding_completed` Column *(fixes C4)*

**The problem in plain words:** `app_users.gender` and `app_users.date_of_birth` are `NOT NULL`. When someone logs in for the first time we must insert a row, but we don't know their gender or birthday yet, so we insert *placeholder* values. If the app then says "profile complete = gender and DOB are not null", it is always true — even before the user has filled anything. So we add a proper yes/no column that starts as `false` and becomes `true` only after the onboarding form.

#### 1. Create `src/main/resources/db/migration/V3__add_onboarding_completed.sql`

```sql
ALTER TABLE app_users
    ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;
```

> - `V3__...` — Flyway runs migrations in number order and remembers which ones already ran. **Never edit `V1`/`V2`**; always add a new file.
> - `NOT NULL DEFAULT FALSE` — every existing and future row automatically starts as "not onboarded".
> - It is applied automatically the next time the app starts (or `docker-compose` DB is up and you run `bootRun`).

#### 2. Edit `model/entity/AppUser.java`
Add this field **right after the `isActive` field** (before `createdAt`):

```java
    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;
```

> - `@Column(name = "onboarding_completed")` — maps this Java field to the new column.
> - `@Builder.Default` — when we create users with `AppUser.builder()...build()` and don't set this field, it defaults to `false` (without this annotation, Lombok's builder would leave it `null`).
> - It is `Boolean` (capital B) like `isActive`, so Lombok generates `getOnboardingCompleted()` / `setOnboardingCompleted(...)` — these exact names are used in `AuthService` and `UserService` below.
> - If you forget this field, the app starts fine; if you forget the **migration** but add the field, startup fails with `Schema-validation: missing column [onboarding_completed]`.

---

### Step 2.2: Stateless JWT Security Infrastructure

#### 1. Create `src/main/java/com/matcher/matchmate_backend/security/AuthenticatedUserPrincipal.java`

```java
package com.matcher.matchmate_backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class AuthenticatedUserPrincipal implements UserDetails {

    private final Long userId;
    private final String firebaseUid;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUserPrincipal(Long userId, String firebaseUid) {
        this.userId = userId;
        this.firebaseUid = firebaseUid;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public Long getUserId() {
        return userId;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // Stateless token auth does not use database passwords
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

> **What is this class?** A small "ID card" object describing the logged-in user for the duration of one request.
>
> **Why it exists:** Spring Security needs *some* object that represents the current user. It expects it to implement `UserDetails`. When a controller needs to know who is calling (e.g. `PUT /api/v1/users/me/demographics`), Spring Security injects this principal using `@AuthenticationPrincipal AuthenticatedUserPrincipal principal`. You get `principal.getUserId()` instantly without querying the database again!
>
> **Piece by piece:**
> - Fields `userId`, `firebaseUid` — our database id and the Firebase user id, both read out of the JWT. They are `final` (cannot change after creation).
> - `authorities` — the user's *roles*. Everyone is `ROLE_USER` for now (later you could add `ROLE_ADMIN`).
> - Constructor — fills the fields and assigns the single `ROLE_USER` role.
> - `getUserId()`, `getFirebaseUid()` — **our own** getters; controllers use these.
> - `getAuthorities()` — Spring Security asks "what may this user do?"; we answer with the roles.
> - `getPassword()` — returns `null` because nobody logs in with a password here (Firebase handled the login).
> - `getUsername()` — Spring requires a "username"; we simply use the user id as text.
> - `isAccountNonExpired()`, `isAccountNonLocked()`, `isCredentialsNonExpired()`, `isEnabled()` — Spring's account-status checks. We always return `true` because a valid, unexpired JWT is already our proof. (If you later want to ban users, `isEnabled()` is where that logic could go.)

---

#### 2. Create `src/main/java/com/matcher/matchmate_backend/security/JwtTokenProvider.java`

```java
package com.matcher.matchmate_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.issuer}") String issuer) {
        // Modern JJWT 0.13: create HMAC-SHA key from secret bytes
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    /**
     * Generates a stateless JWT token containing the user's internal ID and Firebase UID.
     */
    public String generateToken(Long userId, String firebaseUid) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("firebaseUid", firebaseUid)
                .claim("role", "ROLE_USER")
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts internal User ID from token subject.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extracts Firebase UID claim.
     */
    public String getFirebaseUidFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("firebaseUid", String.class);
    }

    /**
     * Validates whether the token signature is authentic and not expired.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

> **What is this class?** The "token factory + token checker". It is the only class that knows how to create and read our JWTs.
>
> **Why JJWT 0.13.0 modern syntax?** Older JJWT versions used deprecated `parseClaimsJws()` and `setSigningKey()`. JJWT 0.13 uses `verifyWith(key).build().parseSignedClaims(token).getPayload()`, which is faster and thread-safe.
>
> **Piece by piece:**
> - `@Component` — Spring creates exactly one `JwtTokenProvider` and injects it wherever needed (`AuthService`, `JwtAuthenticationFilter`).
> - `signingKey` — the secret turned into a proper cryptographic key object. Created once in the constructor.
> - Constructor — the three `@Value(...)` parameters are read from `application.yml`. `Keys.hmacShaKeyFor(bytes)` builds the key; it **throws `WeakKeyException` if the secret is shorter than 32 bytes**. With a 44-byte secret JJWT automatically uses HS256.
> - `generateToken(userId, firebaseUid)` — builds a signed token. What goes inside (the *claims*):
>   - `subject` (`sub`) = our user id (the standard place for "who is this token about").
>   - `firebaseUid` = custom claim, handy for debugging/linking to Firebase.
>   - `role` = `ROLE_USER` (custom claim; the filter currently hard-codes the role, but it is there for the future).
>   - `issuer` (`iss`) = `matchmate-backend`, `issuedAt` (`iat`) = now, `expiration` (`exp`) = now + 24 h.
>   - `signWith(signingKey)` adds the signature, `compact()` turns everything into the final `xxxxx.yyyyy.zzzzz` string.
> - `getUserIdFromToken(token)` — reads `sub` and converts it back to a `Long`.
> - `getFirebaseUidFromToken(token)` — reads the custom `firebaseUid` claim as a `String`.
> - `validateToken(token)` — returns `true`/`false`. Parsing **verifies the signature and the expiry automatically**; any problem (wrong signature, expired, garbage text) throws a `JwtException`, which we catch and turn into `false`. `parseSignedClaims` also **rejects tokens that have no signature**, which blocks a classic attack.
> - `getExpirationMs()` — lets `AuthService` tell Flutter how long the token lives.
> - `getClaims(token)` (private) — the shared "verify + open the token" helper used by the two getters.
>
> **Small note:** each request verifies the signature about 3 times (validate + 2 getters). HMAC is extremely cheap (microseconds), so this is fine for now.

---

#### 3. Create `src/main/java/com/matcher/matchmate_backend/security/JwtAuthenticationFilter.java`

```java
package com.matcher.matchmate_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String jwt = extractJwtFromRequest(request);

        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Long userId = tokenProvider.getUserIdFromToken(jwt);
            String firebaseUid = tokenProvider.getFirebaseUidFromToken(jwt);

            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId, firebaseUid);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set user in Spring Security Context for this request thread
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

> **What is this class?** "The Bouncer" standing at the door of every request. It looks at the `Authorization` header and, if the token is genuine, tells Spring "this request comes from user #N".
>
> **Why `OncePerRequestFilter`?** Guarantees this filter is executed exactly once per incoming HTTP call. (Because the class is also a `@Component`, Spring Boot registers it as a normal servlet filter *in addition to* our security chain; `OncePerRequestFilter` makes sure the second registration does nothing.)
>
> **Piece by piece:**
> - Constructor — receives `JwtTokenProvider` from Spring (constructor injection).
> - `doFilterInternal(request, response, filterChain)` — the method Spring calls for each request:
>   1. `extractJwtFromRequest` gets the raw token string (or `null`).
>   2. `if (hasText(jwt) && validateToken(jwt))` — only continue when a token exists **and** its signature/expiry are valid.
>   3. Read `userId` and `firebaseUid` out of the token and build an `AuthenticatedUserPrincipal`.
>   4. Wrap it in a `UsernamePasswordAuthenticationToken(principal, null, authorities)`. The **3-argument constructor marks it as "already authenticated"** (the 2-argument one would mean "not yet checked"). The `null` is the password — we have none.
>   5. `setDetails(...)` attaches request info (client IP etc.) to the authentication — optional, useful for logs.
>   6. `SecurityContextHolder.getContext().setAuthentication(authentication)` — puts the user on Spring's per-request notepad.
>   7. **`filterChain.doFilter(request, response)` always runs**, valid token or not. The filter never rejects anything itself; it only *identifies* people. If no user was recorded, `SecurityConfig` will reject the request later with 401.
> - `extractJwtFromRequest` — reads the header `Authorization: Bearer eyJ...`; if it starts with `"Bearer "` (7 characters) it returns the text after that, otherwise `null`.
>
> **Handy side effect:** an *expired* token sent to a public route like `/api/v1/auth/verify-token` is simply ignored (no user set, route is public anyway), so the user can always log in again.

---

#### 4. Create `src/main/java/com/matcher/matchmate_backend/config/SecurityConfig.java` *(fixes C5)*

```java
package com.matcher.matchmate_backend.config;

import com.matcher.matchmate_backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF (Stateless REST APIs using Bearer tokens do not need CSRF protection)
            .csrf(AbstractHttpConfigurer::disable)
            // 2. Enable CORS for Flutter app
            .cors(Customizer.withDefaults())
            // 3. Make session management STATELESS (no server-side HTTP sessions)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 4. NEW (C5): not logged in / bad token -> 401 Unauthorized (Spring's default would be 403)
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            // 5. Configure Endpoint Route Access
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll() // Public login handshake
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()                  // All other APIs require Bearer JWT
            )
            // 6. Add our JWT filter before the standard username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

> **What is this class?** The rulebook for the whole API: *which URLs are public, which need a login, and how the login is checked.*
>
> **Piece by piece:**
> - `@Configuration` + `@EnableWebSecurity` — "this class configures Spring Security".
> - Constructor — receives our `JwtAuthenticationFilter` bean.
> - `securityFilterChain(HttpSecurity http)` with `@Bean` — builds the "chain" of security filters every request passes through. Rules in order:
>   1. **`csrf(disable)`** — CSRF attacks abuse browser cookies. We use no cookies, so the protection would only get in the way.
>   2. **`cors(withDefaults())`** — enables CORS handling with default (strict) settings. A native Flutter app ignores CORS anyway; it only matters if you add a web client later.
>   3. **`STATELESS`** — never create an HTTP session; every request must carry its own token.
>   4. **`exceptionHandling → HttpStatusEntryPoint(UNAUTHORIZED)`** *(C5)* — decides what to answer when an unauthenticated caller hits a protected route. Without this line Spring sends **403 Forbidden**. The correct code is **401 Unauthorized** ("I don't know who you are"); 403 means "I know who you are but you may not do this". Flutter's HTTP client will look for **401** to send the user back to the login screen.
>   5. **`authorizeHttpRequests`** — the access rules, checked top to bottom: `/api/v1/auth/**` is open to everyone (this is where people log in), `/error` is open (so Spring's own error pages/messages are visible — needed for the C6 error text), and **everything else requires a logged-in user**.
>   6. **`addFilterBefore(jwt filter, UsernamePasswordAuthenticationFilter)`** — inserts our bouncer early in the chain, so by the time the rules in step 5 are checked, our filter has already recorded who the user is.
> - `http.build()` — finishes and returns the configured chain.

---

### Step 2.3: Token Verification & User Auto-Provisioning Flow

#### 1. Create DTOs in `dto/request/` and `dto/response/`

> **A quick note on the Lombok annotations you will see on every DTO:**
> - `@Data` — generates getters, setters, `toString`, `equals`, `hashCode`.
> - `@NoArgsConstructor` — an empty constructor. **Jackson (the JSON library) needs this** to create the object when reading JSON.
> - `@AllArgsConstructor` — a constructor with every field. `@Builder` needs it.
> - `@Builder` — lets you write `AuthTokenResponse.builder().accessToken("...").userId(1L).build()` instead of a long constructor.

##### `src/main/java/com/matcher/matchmate_backend/dto/request/TokenVerifyRequest.java`
```java
package com.matcher.matchmate_backend.dto.request;

import com.matcher.matchmate_backend.model.enums.auth.PrimaryAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerifyRequest {

    @NotBlank(message = "Firebase ID token is required")
    private String idToken;

    @NotNull(message = "Primary auth provider is required")
    private PrimaryAuthProvider provider;
}
```

> **What/why:** the JSON body Flutter sends to log in: `{ "idToken": "<from Firebase>", "provider": "GOOGLE" }`.
> - `idToken` — the Firebase ID token. `@NotBlank` = must be present and not just spaces, else Spring answers `400` automatically.
> - `provider` — `GOOGLE` or `PHONE` (your `PrimaryAuthProvider` enum). `@NotNull` = required. A value that is not in the enum (e.g. `"FACEBOOK"`) also produces a `400`.

##### `src/main/java/com/matcher/matchmate_backend/dto/response/PrefilledUserData.java`
```java
package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrefilledUserData {
    private String fullName;
    private String avatarUrl;
    private String email;
}
```

> **What/why:** a small "sub-object" inside the login response. Google already knows the person's name and photo, so we send them back and the Flutter onboarding screen can **pre-fill** the form ("Is this you?"). `email` is `null` for phone logins.

##### `src/main/java/com/matcher/matchmate_backend/dto/response/AuthTokenResponse.java`
```java
package com.matcher.matchmate_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponse {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private Boolean isProfileComplete;
    private PrefilledUserData prefilledData;
}
```

> **What/why:** everything Flutter needs after a successful login.
> - `accessToken` — our JWT. Flutter stores it in secure storage and sends it on every later call.
> - `tokenType` — always `"Bearer"` (tells the client how to send it).
> - `expiresIn` — token lifetime in **seconds** (86 400 = 24 h).
> - `userId` — the user's id in our database.
> - `isProfileComplete` — `false` → Flutter shows the onboarding screen; `true` → go straight to the feed. (Now backed by the real `onboarding_completed` column — C4.)
> - `prefilledData` — the name/avatar/email described above.
>
> **Keep `Boolean` with a capital B.** With a primitive `boolean`, Lombok would generate `isProfileComplete()` and Jackson would rename the JSON field to `profileComplete`, breaking the contract with Flutter.

**Example response**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "isProfileComplete": false,
  "prefilledData": { "fullName": "Jane Doe", "avatarUrl": "https://...", "email": "jane@example.com" }
}
```

---

#### 2. Create `src/main/java/com/matcher/matchmate_backend/service/AuthService.java` *(fixes C3, C4)*

```java
package com.matcher.matchmate_backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.matcher.matchmate_backend.dto.request.TokenVerifyRequest;
import com.matcher.matchmate_backend.dto.response.AuthTokenResponse;
import com.matcher.matchmate_backend.dto.response.PrefilledUserData;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.model.enums.auth.Gender;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import com.matcher.matchmate_backend.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final FirebaseAuth firebaseAuth;
    private final AppUserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public AuthService(FirebaseAuth firebaseAuth, AppUserRepository userRepository, JwtTokenProvider tokenProvider) {
        this.firebaseAuth = firebaseAuth;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthTokenResponse verifyAndAuthenticate(TokenVerifyRequest request) {
        FirebaseToken decodedToken;
        try {
            decodedToken = firebaseAuth.verifyIdToken(request.getIdToken());
        } catch (Exception e) {
            log.error("Firebase token verification failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired Firebase ID token");
        }

        String firebaseUid = decodedToken.getUid();
        String name = decodedToken.getName() != null ? decodedToken.getName() : "New Roommate";
        String picture = decodedToken.getPicture();
        String email = decodedToken.getEmail();

        // Check if user already exists in PostgreSQL
        AppUser user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> autoProvisionUser(firebaseUid, request, name, picture));

        // Generate custom MatchMate session JWT
        String accessToken = tokenProvider.generateToken(user.getId(), user.getFirebaseUid());

        // CHANGED (C4): use the real flag, NOT "gender/DOB are not null" (placeholders make that always true)
        boolean isProfileComplete = Boolean.TRUE.equals(user.getOnboardingCompleted());

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .userId(user.getId())
                .isProfileComplete(isProfileComplete)
                .prefilledData(PrefilledUserData.builder()
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .email(email)
                        .build())
                .build();
    }

    private AppUser autoProvisionUser(String firebaseUid, TokenVerifyRequest request, String name, String picture) {
        log.info("Auto-provisioning new user with Firebase UID: {}", firebaseUid);
        AppUser newUser = AppUser.builder()
                .firebaseUid(firebaseUid)
                .primaryAuthProvider(request.getProvider())
                .fullName(name)
                .avatarUrl(picture)
                // Placeholders: the DB columns are NOT NULL. They are replaced when the user submits the
                // onboarding form. Until then onboardingCompleted stays false (C4).
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.OTHERS)                 // CHANGED (C3): the enum value is OTHERS, not OTHER
                .onboardingCompleted(false)
                .isActive(true)
                .build();

        return userRepository.save(newUser);
    }
}
```

> **What is this class?** The heart of login: "prove you are who Firebase says you are, make sure we have a database row for you, and give you our own token".
>
> **Piece by piece:**
> - `@Service` — marks it as a business-logic bean.
> - Constructor — Spring hands in the three helpers it needs: `FirebaseAuth` (from `FirebaseConfig`), `AppUserRepository` (database), `JwtTokenProvider` (token maker).
> - `verifyAndAuthenticate(request)` — the whole login flow. `@Transactional` = all database changes inside succeed together or are rolled back together.
>   1. **Verify with Firebase.** `firebaseAuth.verifyIdToken(...)` checks Google's signature, expiry and that the token belongs to *our* Firebase project. If anything is wrong it throws; we catch it and answer **401** with a clear message via `ResponseStatusException` (Spring turns that into an HTTP error automatically).
>   2. **Read facts from the verified token:** `uid` (the permanent Firebase user id), `name`, `picture`, `email`. If Google gave no name (common for phone login) we use `"New Roommate"`.
>   3. **Find or create the user.** `findByFirebaseUid(uid)` returns an `Optional`. `.orElseGet(() -> autoProvisionUser(...))` runs the creation code **only if no user was found** (that is the difference from `orElse`, which would always run it).
>   4. **Create our JWT** for that user (`generateToken(id, firebaseUid)`).
>   5. **Decide `isProfileComplete`** from the `onboarding_completed` column (**C4**). `Boolean.TRUE.equals(x)` is a null-safe way to say "is x true?".
>   6. **Build the response** (`AuthTokenResponse`). `expiresIn` converts milliseconds → seconds (`/ 1000`).
> - `autoProvisionUser(...)` (private) — inserts the very first database row for a new person:
>   - copies Firebase uid, provider (from the request), name and avatar;
>   - **placeholders** for `dateOfBirth` (2000-01-01) and `gender` (`OTHERS` — **C3**: the guide said `OTHER`, which does not exist in your enum and would not compile) because those columns are `NOT NULL`;
>   - `onboardingCompleted(false)` — *this* is what says "not really finished yet" (**C4**);
>   - `isActive(true)`;
>   - `save(...)` inserts the row and returns it with its new `id`.
>
> **Order of things for a brand-new user:** first `verify-token` → row created with placeholders, `isProfileComplete = false` → Flutter shows onboarding → `PUT /users/me/demographics` replaces placeholders and sets `onboarding_completed = true` → next login returns `isProfileComplete = true`.

---

#### 3. Create `src/main/java/com/matcher/matchmate_backend/controller/AuthController.java`

```java
package com.matcher.matchmate_backend.controller;

import com.matcher.matchmate_backend.dto.request.TokenVerifyRequest;
import com.matcher.matchmate_backend.dto.response.AuthTokenResponse;
import com.matcher.matchmate_backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/verify-token")
    public ResponseEntity<AuthTokenResponse> verifyToken(@Valid @RequestBody TokenVerifyRequest request) {
        AuthTokenResponse response = authService.verifyAndAuthenticate(request);
        return ResponseEntity.ok(response);
    }
}
```

> **What is this class?** The "front desk" for login: it receives the HTTP request, hands the work to `AuthService`, and returns the answer. **No business logic lives here** — controllers should stay thin.
>
> **Piece by piece:**
> - `@RestController` — every method returns data (JSON), not an HTML page.
> - `@RequestMapping("/api/v1/auth")` — common URL prefix for the class. (`/api/v1/auth/**` is the public path in `SecurityConfig`.)
> - `@PostMapping("/verify-token")` — this method answers `POST /api/v1/auth/verify-token`.
> - `@RequestBody` — convert the JSON body into a `TokenVerifyRequest` object.
> - `@Valid` — run the validation rules (`@NotBlank`, `@NotNull`) first; if they fail, Spring returns `400` **before** our method runs.
> - `ResponseEntity.ok(response)` — HTTP `200 OK` with the response as JSON.

---

### Step 2.4: Demographics Onboarding API

#### 1. Create DTOs in `dto/request/` and `dto/response/`

##### `src/main/java/com/matcher/matchmate_backend/dto/request/DemographicsRequest.java`
```java
package com.matcher.matchmate_backend.dto.request;

import com.matcher.matchmate_backend.model.enums.auth.Gender;
import com.matcher.matchmate_backend.model.enums.auth.OccupationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemographicsRequest {

    @NotBlank(message = "Full name cannot be blank")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private String avatarUrl;

    private OccupationCategory occupationCategory;

    @Size(max = 100, message = "Occupation title cannot exceed 100 characters")
    private String occupationTitle;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;
}
```

> **What/why:** the JSON body of the onboarding form. Validation annotations reject bad input *before* it reaches the service:
> - `fullName` — required, not blank, at most 100 characters (matches the `VARCHAR(100)` column).
> - `gender` — required; one of `MALE`, `FEMALE`, `NON_BINARY`, `OTHERS`.
> - `dateOfBirth` — required, `@Past` = must be earlier than today. Sent as `"2001-05-20"`.
> - `avatarUrl`, `occupationCategory` (`STUDENT` / `WORKING_PROFESSIONAL`), `occupationTitle`, `bio` — optional; length limits match the columns (100 / 500).
> - **Why isn't "18+" here?** Bean Validation has no built-in "at least 18 years old" rule (it would need a custom annotation), so `UserService` checks it.

**Example request**
```json
{
  "fullName": "Jane Doe",
  "gender": "FEMALE",
  "dateOfBirth": "2001-05-20",
  "avatarUrl": null,
  "occupationCategory": "STUDENT",
  "occupationTitle": "MSc Student",
  "bio": "Early riser, love cooking."
}
```

##### `src/main/java/com/matcher/matchmate_backend/dto/response/UserDemographicsResponse.java`
```java
package com.matcher.matchmate_backend.dto.response;

import com.matcher.matchmate_backend.model.enums.auth.Gender;
import com.matcher.matchmate_backend.model.enums.auth.OccupationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDemographicsResponse {
    private Long userId;
    private String fullName;
    private Gender gender;
    private LocalDate dateOfBirth;
    private Integer age;
    private String avatarUrl;
    private OccupationCategory occupationCategory;
    private String occupationTitle;
    private String bio;
    private Integer profileStrength;
    private Boolean isProfileComplete;
}
```

> **What/why:** the saved profile echoed back to Flutter so it can update its screen without another request.
> - `age` — calculated by the server from the date of birth (the database only stores the birth date).
> - `profileStrength` — 0–100 gamification meter. Right now it is `40` after onboarding; Milestone 3.2 will raise it as lifestyle questions are answered.
> - `isProfileComplete` — `true` (the `Boolean` capital-B rule from `AuthTokenResponse` applies here too).

---

#### 2. Create `src/main/java/com/matcher/matchmate_backend/service/UserService.java` *(fixes C4)*

```java
package com.matcher.matchmate_backend.service;

import com.matcher.matchmate_backend.dto.request.DemographicsRequest;
import com.matcher.matchmate_backend.dto.response.UserDemographicsResponse;
import com.matcher.matchmate_backend.model.entity.AppUser;
import com.matcher.matchmate_backend.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;

@Service
public class UserService {

    private final AppUserRepository userRepository;

    public UserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserDemographicsResponse updateDemographics(Long userId, DemographicsRequest request) {
        // Enforce Core Product Principle: Age >= 18 validation
        int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be at least 18 years old to use MatchMate.");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setFullName(request.getFullName());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        user.setOccupationCategory(request.getOccupationCategory());
        user.setOccupationTitle(request.getOccupationTitle());
        user.setBio(request.getBio());

        // NEW (C4): the placeholders are now replaced by real values -> onboarding is really done
        user.setOnboardingCompleted(true);

        AppUser updated = userRepository.save(user);

        // Core onboarding completes the bare essentials -> 40% profile strength
        int profileStrength = 40;

        return UserDemographicsResponse.builder()
                .userId(updated.getId())
                .fullName(updated.getFullName())
                .gender(updated.getGender())
                .dateOfBirth(updated.getDateOfBirth())
                .age(age)
                .avatarUrl(updated.getAvatarUrl())
                .occupationCategory(updated.getOccupationCategory())
                .occupationTitle(updated.getOccupationTitle())
                .bio(updated.getBio())
                .profileStrength(profileStrength)
                .isProfileComplete(true)
                .build();
    }
}
```

> **What is this class?** The business rules for the user's profile. Today it has one job: save the onboarding form.
>
> **Piece by piece:**
> - Constructor — receives `AppUserRepository`.
> - `updateDemographics(userId, request)` — `@Transactional` so the update is all-or-nothing:
>   1. **Age check first.** `Period.between(dob, today).getYears()` gives completed years. Under 18 → throw `ResponseStatusException(400, "User must be at least 18 years old…")`. It runs *before* touching the database, so bad input fails fast. (The message reaches Flutter thanks to `include-message: always`, **C6**.)
>   2. **Load the user** by id (`findById`). `orElseThrow(...)` → `404 User not found` if the id in the token has no row (for example the database was reset).
>   3. **Copy the form values** onto the entity with the setters. The avatar is only replaced if the request contains one (so the Google photo is kept by default).
>   4. **`setOnboardingCompleted(true)`** *(C4)* — the moment the placeholder gender/DOB become real.
>   5. `save(user)` — writes the changes.
>   6. **Build the response.** `profileStrength` is a fixed `40` for now; the age is the value calculated in step 1.
>
> **Where does `userId` come from?** From the JWT (via `UserController`), **never from the URL or body** — so a user can only ever edit *their own* profile.

---

#### 3. Create `src/main/java/com/matcher/matchmate_backend/controller/UserController.java`

```java
package com.matcher.matchmate_backend.controller;

import com.matcher.matchmate_backend.dto.request.DemographicsRequest;
import com.matcher.matchmate_backend.dto.response.UserDemographicsResponse;
import com.matcher.matchmate_backend.security.AuthenticatedUserPrincipal;
import com.matcher.matchmate_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/me/demographics")
    public ResponseEntity<UserDemographicsResponse> updateDemographics(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody DemographicsRequest request) {
        UserDemographicsResponse response = userService.updateDemographics(principal.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
```

> **What is this class?** The HTTP entry point for user-profile endpoints (more will be added in Milestone 3.2).
>
> **Piece by piece:**
> - `@RequestMapping("/api/v1/users")` + `@PutMapping("/me/demographics")` → `PUT /api/v1/users/me/demographics`. **`/me`** means "the person holding the token" — the id is not in the URL.
> - `@AuthenticationPrincipal AuthenticatedUserPrincipal principal` — Spring gives us the object that `JwtAuthenticationFilter` stored in the `SecurityContext`. That is how we know the caller's id with **zero database queries**.
> - `@Valid @RequestBody DemographicsRequest request` — JSON → object, then validation (bad data → `400`).
> - Calls `userService.updateDemographics(principal.getUserId(), request)` and returns `200 OK` with the saved profile.
> - No token → `SecurityConfig` answers `401` before this method is ever reached.

---

### Step 2.5: Build, Run & Test

#### Build & run

1. Make sure `JWT_SECRET` is set and the Firebase JSON is at `secrets/firebase-service-account.json` (Step 2.0).
2. Compile:
   ```powershell
   .\gradlew.bat compileJava
   ```
   Expect `BUILD SUCCESSFUL`.
3. Start the database and the backend:
   ```powershell
   docker-compose up -d
   .\gradlew.bat bootRun
   ```
   In the log you should see the Flyway migration `V3` being applied once and `Initialized FirebaseApp with service account: ...`.

> Note: the existing test `MatchmateBackendApplicationTests` also starts the whole app, so it too needs the database running and `JWT_SECRET` set. A separate test configuration is planned for Milestone 10.

#### Manual tests (PowerShell)

**Test 1 — protected route without a token → expect `401`** (this proves fix C5):
```powershell
try { Invoke-RestMethod http://localhost:8080/api/v1/users/me/demographics -Method Put } catch { $_.Exception.Response.StatusCode.value__ }
```

**Test 2 — fake Firebase token → expect `401` and the message `Invalid or expired Firebase ID token`:**
```powershell
$body = '{"idToken":"fake","provider":"GOOGLE"}'
try { Invoke-RestMethod http://localhost:8080/api/v1/auth/verify-token -Method Post -ContentType 'application/json' -Body $body }
catch { "$($_.Exception.Response.StatusCode.value__) $($_.ErrorDetails.Message)" }
```

**Test 3 — real end-to-end without the Flutter app.** Flutter's login screen is Milestone 6.3, so until then get a real Firebase ID token like this:
1. Firebase Console → *Authentication* → *Sign-in method* → enable **Email/Password** (and also enable **Google** and **Phone** for the real app).
2. *Authentication* → *Users* → *Add user* (any test e-mail + password).
3. Firebase Console → ⚙ Project settings → *General* → copy the **Web API Key**.
4. Request a token:
   ```powershell
   $key = "YOUR_WEB_API_KEY"
   $r = Invoke-RestMethod "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$key" -Method Post -ContentType 'application/json' -Body '{"email":"test@example.com","password":"YourTestPassword","returnSecureToken":true}'
   $firebaseToken = $r.idToken
   ```
5. Exchange it for a MatchMate token (`provider` must be `GOOGLE` or `PHONE` — use `GOOGLE` for this test):
   ```powershell
   $login = Invoke-RestMethod http://localhost:8080/api/v1/auth/verify-token -Method Post -ContentType 'application/json' -Body (@{ idToken = $firebaseToken; provider = "GOOGLE" } | ConvertTo-Json)
   $login   # expect isProfileComplete = False for a brand-new user
   ```
6. Submit onboarding with the MatchMate token:
   ```powershell
   $h = @{ Authorization = "Bearer $($login.accessToken)" }
   $form = '{"fullName":"Test User","gender":"FEMALE","dateOfBirth":"2001-05-20"}'
   Invoke-RestMethod http://localhost:8080/api/v1/users/me/demographics -Method Put -Headers $h -ContentType 'application/json' -Body $form
   ```
   Expect `profileStrength = 40`, `isProfileComplete = True`.
7. Log in again (step 5) → now `isProfileComplete = True`.
8. Repeat step 6 with `"dateOfBirth":"2015-01-01"` → expect `400` with `User must be at least 18 years old to use MatchMate.`

#### Common errors and what they mean

| What you see | Meaning | Fix |
| :--- | :--- | :--- |
| `Could not resolve placeholder 'JWT_SECRET'` | The environment variable is not visible to the app. | Set it (Step 2.0 A) and **restart IntelliJ/terminal**. |
| `Firebase service account file not found at 'file:./secrets/...'` | Wrong path, or the app started from another folder. | Check `secrets/firebase-service-account.json`, or set `FIREBASE_CREDENTIALS_PATH` to an absolute `file:D:/...` path. |
| `WeakKeyException … key byte array is N bits` | The secret is shorter than 32 bytes. | Use the 44-character value from Step 2.0 A. |
| `Schema-validation: missing column [onboarding_completed]` | The entity has the field but the migration did not run. | Create/check `V3__add_onboarding_completed.sql` in `db/migration`. |
| `cannot find symbol … Gender.OTHER` | Old guide code. | Use `Gender.OTHERS` (C3). |
| `Schema validation: missing table [app_users]` (and no `Flyway` lines in the log) | Spring Boot 4 does not run Flyway with only `flyway-core`. | In `build.gradle` replace `flyway-core` with `org.springframework.boot:spring-boot-starter-flyway` (keep `flyway-database-postgresql`). See `learning/11-startup-troubleshooting.md`. |
| `Connection refused … localhost:5432` | PostgreSQL container is not running. | `docker-compose up -d`. |
| `401` on a request that has a token | Header must be exactly `Authorization: Bearer <token>`; or the token expired (24 h) or was signed with an older secret. | Log in again via `verify-token`. |
| `400` with no message in the body | `server.error.include-message` missing. | Add the `server.error` block (C6). |

---

## 🚧 Known Limitations (safe to postpone, but good to know)

- **`provider` is taken from the request body.** The verified Firebase token itself contains the real sign-in method (`decodedToken.getClaims()` → `firebase` → `sign_in_provider`). A later hardening step can cross-check it so a client cannot claim `PHONE` while using Google.
- **A JWT cannot be revoked before it expires (24 h).** "Logout" simply means Flutter deletes the stored token. If you ever need instant bans or refresh tokens, that is a later feature.
- **`JwtTokenProvider.validateToken` does not check the `iss` (issuer) claim.** Optional hardening: add `.requireIssuer(issuer)` to the parser builder.
- **Development-only settings:** `server.error.include-message: always`, `show-sql: true`, and the database password in `application.yml`. Before production: use a global `@RestControllerAdvice` for errors, turn off SQL logging, and move DB credentials to environment variables.
- **Placeholder users:** anyone with `onboarding_completed = false` has fake gender/date-of-birth. Matching and discovery (Milestones 3–4) must ignore them.
