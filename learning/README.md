# MatchMate Backend — Learning Folder

This folder is your **study notebook**. `BACKEND_PROGRESS.md` tells you *what code to write*.
This folder tells you *what that code means, when it runs, and who calls it*.

## How the files connect

```
README.md (you are here: progress + index)
   │
   ├── 01-system-design.md        ← the BIG PICTURE: which class calls which, and when
   │        │
   │        ├── 02-JwtTokenProvider.md            ← detail of one box in the picture
   │        └── 03-AuthenticatedUserPrincipal.md  ← detail of another box
   │
   └── AI_CONTEXT_PROMPT.md       ← paste this into a new AI chat to continue learning
```

Rule: every new class you learn gets **(a)** its own `NN-ClassName.md`, **(b)** a row in the
"Call Map" table in `01-system-design.md`, **(c)** a row in the progress table below.

---

## Progress Tracker

Legend: ⬜ not started · 🟨 reading / half understood · ✅ understood (you can explain it to someone else)

| # | Topic | Note file | Code written by you? | Understood? |
| :-: | :--- | :--- | :-: | :-: |
| 1 | Big picture: login flow & request flow | `01-system-design.md` | — | ⬜ |
| 2 | `JwtTokenProvider` | `02-JwtTokenProvider.md` | ✅ (has 1 bug, see file) | ⬜ |
| 3 | `AuthenticatedUserPrincipal` | `03-AuthenticatedUserPrincipal.md` | ✅ (1 small mismatch) | ⬜ |
| 4 | `JwtAuthenticationFilter` | `04-JwtAuthenticationFilter.md` | ✅ | ⬜ |
| 5 | `SecurityConfig` | `05-SecurityConfig.md` | ✅ (🚨 missing `@Bean`) | ⬜ |
| 6 | `FirebaseConfig` | `06-FirebaseConfig.md` | ✅ | ⬜ |
| 7 | DTOs (`TokenVerifyRequest`, `AuthTokenResponse`, `PreFilledUserData`) | `07-AuthDTOs.md` | ✅ | ⬜ |
| 8 | `AuthService` | `08-AuthService.md` | ✅ | ⬜ |
| 9 | `AuthController` | `09-AuthController.md` | ✅ | ⬜ |
| 10 | `DemographicsRequest` / `UserDemographicsResponse` / `UserService` / `UserController` | `10-UserFlow.md` | ✅ | ⬜ |
| 11 | First `bootRun` error: `missing table [app_users]` (Flyway + Spring Boot 4) | `11-startup-troubleshooting.md` | — | ⬜ |

> **Status 2026-10-01:** all Milestone 2 files are written and compile. `./gradlew bootRun`
> fails at startup. Three fixes needed (see notes 11 and 05): swap `flyway-core` for `spring-boot-starter-flyway` in
> `build.gradle`, add `V3__add_onboarding_completed.sql`, and add `@Bean` to `SecurityConfig.securityFilterChain`.

**How to mark understood:** close the note, and explain the class out loud (or in writing) in 3
sentences: *what it is, who calls it, what it returns.* If you can, change ⬜ to ✅.

### Self-check quiz (do after each note)
Each note ends with a short quiz. Answers are at the bottom of the same note.

---

## Suggested learning order
1. `01-system-design.md` (10 min — just the pictures)
2. `03-AuthenticatedUserPrincipal.md` (simplest class)
3. `02-JwtTokenProvider.md`
4. `04-JwtAuthenticationFilter.md` → `05-SecurityConfig.md` (the guard and the rulebook)
5. `06-FirebaseConfig.md` → `07-AuthDTOs.md` → `08-AuthService.md` → `09-AuthController.md` (login flow, in order)
6. `10-UserFlow.md` (onboarding), then `11-startup-troubleshooting.md` if the app will not start

## Where to go next in the project
1. Read `11-startup-troubleshooting.md` and `05-SecurityConfig.md` (top box) and apply the 3 fixes so the app starts.
2. Run the 3 tests at the end of `BACKEND_PROGRESS.md` (Step 2.4).
3. Explain each class aloud, then mark ✅ in the "Understood?" column.
