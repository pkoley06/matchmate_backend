# 01 — System Design: Who Calls Whom, and When

> Related: [`02-JwtTokenProvider.md`](02-JwtTokenProvider.md) · [`03-AuthenticatedUserPrincipal.md`](03-AuthenticatedUserPrincipal.md) · [`README.md`](README.md)

## The one-sentence idea
**Firebase proves who the person is (Google / phone login). Our server then gives them its *own*
small signed ticket (a JWT). Every later request shows that ticket, so we never ask Google again.**

## Real-life analogy
| Real life (cinema) | MatchMate |
| :--- | :--- |
| You show your ID at the box office | Flutter shows the **Firebase ID token** to `AuthService` |
| Box office checks the ID is real | `FirebaseAuth.verifyIdToken(...)` |
| Box office prints a **ticket with a stamp** | `JwtTokenProvider.generateToken(...)` |
| The stamp can't be copied (only the cinema owns it) | `jwt.secret` (HMAC key) |
| Guard at the hall door looks at your ticket | `JwtAuthenticationFilter` |
| Guard writes "Seat 12, valid" on his clipboard | `SecurityContextHolder` |
| The clipboard entry is a small card about you | `AuthenticatedUserPrincipal` |
| Rules: "Hall doors need a ticket, box office doesn't" | `SecurityConfig` |

---

## Flow A — Login (happens once, when the user opens the app / logs in)

```
Flutter ──(1) POST /api/v1/auth/verify-token {idToken, provider}──►
   SecurityConfig       "/api/v1/auth/** is public" → let through
        ▼
   AuthController       receives JSON → calls service
        ▼
   AuthService          the "brain" of login
        ├─(2) FirebaseAuth.verifyIdToken(idToken)     → is this Google ticket real?
        ├─(3) AppUserRepository.findByFirebaseUid()   → user exists in DB? if not, INSERT
        └─(4) JwtTokenProvider.generateToken(id, uid) ← OUR CLASS: makes our own JWT
        ▼
Flutter ◄──(5) { accessToken, userId, isProfileComplete, ... }
        Flutter saves accessToken in secure storage.
```

**Where `JwtTokenProvider` appears:** step (4) — `generateToken`.

## Flow B — Every later request (happens on every API call)

```
Flutter ──► GET/PUT ...   Header:  Authorization: Bearer <accessToken>
        ▼
   JwtAuthenticationFilter   (runs BEFORE the controller)
        ├─(1) read header, cut off "Bearer "
        ├─(2) JwtTokenProvider.validateToken(token)         ← signature OK? not expired?
        ├─(3) JwtTokenProvider.getUserIdFromToken(token)    ← who is it? (user id)
        ├─(3b)JwtTokenProvider.getFirebaseUidFromToken(...) ← (guide version; see note 02)
        ├─(4) new AuthenticatedUserPrincipal(userId, firebaseUid)   ← OUR CLASS: the "card"
        └─(5) SecurityContextHolder.setAuthentication(...)  ← put the card on the clipboard
        ▼
   SecurityConfig rules   "anyRequest().authenticated()" → clipboard has a user? yes → allow
        ▼
   Controller  method(@AuthenticationPrincipal AuthenticatedUserPrincipal me)
        │            Spring takes the card from the clipboard and hands it to you
        ▼
   Service → Repository → Database
        ▼
Flutter ◄── 200 OK
```

**Where `AuthenticatedUserPrincipal` appears:** created in step (4), read in the controller.

If the token is missing/bad: filter sets nothing → `SecurityConfig` sees "no user" → **401**.

---

## Call Map (keep this table updated as you add classes)

| Class | Called by | Calls | When | Status |
| :--- | :--- | :--- | :--- | :--- |
| `JwtTokenProvider` | `AuthService` (generate), `JwtAuthenticationFilter` (validate/read) | JJWT library | login + every request | written (bug, see 02) |
| `AuthenticatedUserPrincipal` | `JwtAuthenticationFilter` creates it; controllers receive it | nothing (plain data holder) | every authenticated request | written |
| `JwtAuthenticationFilter` | Spring Security chain (auto) | `JwtTokenProvider`, `AuthenticatedUserPrincipal` | every request | written, see 04 |
| `SecurityConfig` | Spring at startup | registers the filter + rules | startup | written, missing `@Bean` (see 05) |
| `FirebaseConfig` | Spring at startup | Google SDK | startup (creates `FirebaseAuth` bean) | written, see 06 |
| `AuthService` | `AuthController` | `FirebaseAuth`, `AppUserRepository`, `JwtTokenProvider` | login only | written, see 08 |
| `AuthController` | Flutter (HTTP) | `AuthService` | login only | written, see 09 |
| `UserController` / `UserService` | Flutter / controller | `AppUserRepository` | onboarding | written, see 10 |
| DTOs (`TokenVerifyRequest`, `AuthTokenResponse`, `PreFilledUserData`, `DemographicsRequest`, `UserDemographicsResponse`) | controllers / services | nothing (data shapes) | every API call | written, see 07 and 10 |

## Startup vs request time (important beginner idea)
- **Startup (once):** Spring scans for `@Component`, `@Configuration`, `@Bean`. It creates **one
  object** of each and wires them together. `JwtTokenProvider` is built here — that's why its
  constructor reads `jwt.secret` from `application.yml`.
- **`AuthenticatedUserPrincipal` is NOT a Spring bean** (no `@Component`). It is created fresh with
  `new` on **every request** because it holds *that request's user*. Sharing one would mix users up.

## Layer cheat-sheet
`Controller` (HTTP in/out) → `Service` (business rules) → `Repository` (database) → `Entity` (table row).
`security/` package = "who are you?" plumbing. `config/` package = wiring/rules. `dto/` = JSON shapes.

## Quiz
1. Which class is created at startup, and which one on every request?
2. In Flow B, what happens if the token is expired? Which class finally answers 401?
3. Why doesn't the filter reject bad tokens itself?

<details><summary>Answers</summary>

1. `JwtTokenProvider` (a bean) at startup; `AuthenticatedUserPrincipal` per request.
2. `validateToken` returns false → filter sets no user → `SecurityConfig`'s entry point answers 401.
3. Its job is only to *identify*; `SecurityConfig` owns the *rules* (which URLs are public). One job each.
</details>
