# 10 — Onboarding flow: `DemographicsRequest`, `UserDemographicsResponse`, `UserService`, `UserController`

Files: `dto/request/DemographicsRequest.java`, `dto/response/UserDemographicsResponse.java`, `service/UserService.java`, `controller/UserController.java`
Related: [`07-AuthDTOs.md`](07-AuthDTOs.md) (Lombok + validation basics) · [`08-AuthService.md`](08-AuthService.md) · [`03-AuthenticatedUserPrincipal.md`](03-AuthenticatedUserPrincipal.md) · [`01-system-design.md`](01-system-design.md) (Flow B)

## 1. What is this part?
After login, a new user fills the onboarding form (name, gender, date of birth, ...). Flutter sends
`PUT /api/v1/users/me/demographics` with the MatchMate token. The server checks age ≥ 18, saves the
data, and flips `onboarding_completed` to `true`.

This is **Flow B**: the request is authenticated by `JwtAuthenticationFilter` before it gets here.

## 2. `DemographicsRequest` (Flutter → server)
Same Lombok set as note 07. Validation annotations:
| Field | Annotations | Meaning |
| :--- | :--- | :--- |
| `fullName` | `@NotBlank`, `@Size(max=100)` | required, ≤ 100 chars |
| `gender` | `@NotNull` | required; enum `Gender` (`MALE`, `FEMALE`, `OTHERS`...) |
| `dateOfBirth` | `@NotNull`, `@Past` | required; `LocalDate` (JSON `"2001-05-20"`); must be before today |
| `avatarUrl` | none | optional |
| `occupationCategory` | none | optional enum |
| `occupationTitle` | `@Size(max=100)` | optional |
| `bio` | `@Size(max=500)` | optional |

`@Size` limits length; `@Past` rejects today/future dates. `@Size` on a `null` value passes (null means "not provided").

## 3. `UserDemographicsResponse` (server → Flutter)
Echoes the saved profile plus computed fields: `age`, `profileStrength` (40 after core onboarding) and
`isProfileComplete` (true). Returning the saved state lets Flutter update its screen without another call.

## 4. `UserController`
```java
@RestController
@RequestMapping("/api/v1/users")
...
@PutMapping("/me/demographics")
public ResponseEntity<UserDemographicsResponse> updateDemographics(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
        @Valid @RequestBody DemographicsRequest request)
```
- URL = `/api/v1/users` + `/me/demographics`. **`me`** means "the logged-in user" — the client never
  sends a user id, so nobody can edit someone else by changing a number.
- `@PutMapping` — PUT = "replace this resource's data" (idempotent: sending twice = same result).
- `@AuthenticationPrincipal` — Spring takes the object that `JwtAuthenticationFilter` put on the
  clipboard (`AuthenticatedUserPrincipal`, note 03) and passes it in. No DB query to know who calls.
- `@Valid @RequestBody` — as in note 09.
- `principal.getUserId()` is given to the service.

If there's no/invalid token, this method **never runs** — `SecurityConfig` returns 401 first.

## 5. `UserService.updateDemographics(userId, request)`
`@Service`, constructor-injected `AppUserRepository`, `@Transactional` (all-or-nothing DB changes).

Step by step:
1. **Age rule (core product principle):**
   `Period.between(dob, LocalDate.now()).getYears()` → whole years between two dates.
   `< 18` → `ResponseStatusException(400, ...)`.
2. **Load user:** `userRepository.findById(userId)` returns `Optional<AppUser>`;
   `.orElseThrow(() -> new ResponseStatusException(404, "User not found"))` = "give me the user or fail".
3. **Update fields** with setters. `avatarUrl` only if provided (null-check) so you don't erase the Google photo.
4. **`setOnboardingCompleted(true)`** — the flag that makes the next login return `isProfileComplete=true`.
5. `userRepository.save(user)` — UPDATE (entity already has an id). Inside a transaction JPA would also
   auto-save changes at commit; calling `save` is explicit and fine.
6. Build and return `UserDemographicsResponse` with `profileStrength = 40`, `isProfileComplete = true`.

## 6. Real example
```
PUT /api/v1/users/me/demographics   Authorization: Bearer <token>
{"fullName":"Test User","gender":"FEMALE","dateOfBirth":"2001-05-20"}
→ 200 {"userId":5,"age":25,"profileStrength":40,"isProfileComplete":true,...}

dateOfBirth "2015-01-01" → 400
```

## 7. Mismatch checklist
| Item | Yours | Guide / TASKS | Action |
| :--- | :--- | :--- | :--- |
| Age error message | `"User must be at least 18 years old"` | `"User must be at least 18 years old to use MatchMate."` | Test 8 in `BACKEND_PROGRESS.md` expects the longer text; add ` to use MatchMate.` |
| `@Transactional` import | `jakarta.transaction` | Spring's | works; optional switch |
| Blank `fullName` overwrite | validated `@NotBlank` | ✅ | — |
| Error text visible | needs `server.error.include-message: always` (C6, missing in `application.yml`) | | add it |

## 8. Common errors
| Symptom | Reason |
| :--- | :--- |
| 401 | missing/expired token, or `SecurityConfig` bean not active (note 05) |
| `principal` is `null` → NPE | request wasn't authenticated; `@AuthenticationPrincipal` gets `null` |
| 400 "JSON parse error" | `dateOfBirth` not `yyyy-MM-dd`, or invalid `gender` value |
| 404 "User not found" | token's user id doesn't exist in DB (DB was reset after token issued) |

## 9. Quiz
1. Why is the URL `/me/...` instead of `/users/{id}/...`?
2. What does `@AuthenticationPrincipal` give you, and who put it there?
3. What does `orElseThrow` do?
4. Why is `avatarUrl` only set when not null?

<details><summary>Answers</summary>

1. The user is identified by the token, so a client can't change another user's data by guessing ids.
2. The `AuthenticatedUserPrincipal` stored in `SecurityContextHolder` by `JwtAuthenticationFilter`.
3. Returns the value inside the `Optional`, or throws the supplied exception if empty.
4. To keep the existing (e.g. Google) photo when the form doesn't send one.
</details>
