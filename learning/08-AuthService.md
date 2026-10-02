# 08 — `AuthService` explained line by line

File: `service/AuthService.java`
Related: [`06-FirebaseConfig.md`](06-FirebaseConfig.md) · [`02-JwtTokenProvider.md`](02-JwtTokenProvider.md) · [`07-AuthDTOs.md`](07-AuthDTOs.md) · [`09-AuthController.md`](09-AuthController.md) · [`01-system-design.md`](01-system-design.md) (Flow A)

## 1. What is it?
The **brain of login** (Flow A in note 01). Given a Firebase token it: (1) checks it's real,
(2) finds the user in our DB or creates them, (3) makes our own JWT, (4) tells Flutter whether
onboarding is still needed.

**Service** = the layer for business rules. Controllers stay thin; services decide.

## 2. Who calls it / what it calls
| | |
| :--- | :--- |
| Called by | `AuthController.verifyToken` |
| Calls | `FirebaseAuth` (bean from note 06), `AppUserRepository`, `JwtTokenProvider` |
| When | Only at login |

## 3. Code walk-through

### `@Service`
Like `@Component`, but says "business logic". Spring creates one bean.

### Fields + constructor
Three dependencies injected through the constructor: `FirebaseAuth`, `AppUserRepository`,
`JwtTokenProvider`. Spring finds each bean and passes it in. `private final` = set once.

### `@Transactional` on `verifyAndAuthenticate`
All database work inside the method is **one transaction**: all succeed together or all roll back.
Needed here so "find user → create user" is atomic.
> ⚠️ You import `jakarta.transaction.Transactional`. It works, but Spring's own
> `org.springframework.transaction.annotation.Transactional` is the usual choice in Spring apps
> (more options like `readOnly`). Optional change.

### Step 1 — verify the Firebase token
```java
try { decodedToken = firebaseAuth.verifyIdToken(tokenVerifyRequest.getIdToken()); }
catch (Exception e) { log.error(...); throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired Firebase ID token"); }
```
- `verifyIdToken` contacts/uses Google's public keys to check signature and expiry; returns a `FirebaseToken`.
- Any failure → `ResponseStatusException(401, message)`: a Spring exception that makes the HTTP response have that status and message. Flutter gets 401.
- `catch (Exception e)` is broad (also catches your own bugs); the guide's narrower form catches `FirebaseAuthException`. Acceptable while learning.

### Step 2 — read facts from the token
```java
String firebaseUid = decodedToken.getUid();                         // Firebase's unique id for this person
String name = decodedToken.getName() == null ? "New Roommate" : decodedToken.getName();
String email = decodedToken.getEmail();
String picture = decodedToken.getPicture();
```
`a == null ? x : y` is the **ternary operator**: "if a is null use x, else y". Phone logins have no name, hence the fallback.

### Step 3 — find or create
```java
AppUser user = userRepository.findByFirebaseUid(firebaseUid)
        .orElseGet(() -> autoProvisionUser(firebaseUid, tokenVerifyRequest, name, picture));
```
- `findByFirebaseUid` returns an `Optional<AppUser>` ("maybe a user").
- `.orElseGet(() -> ...)` — if empty, **run the lambda** to create the user; if present, skip it.
  (`orElseGet` is lazy; the creation only runs when needed.)

### Step 4 — our own JWT
`jwtTokenProvider.generateToken(user.getId(), user.getFirebaseUid())` → the `accessToken` string.

### Step 5 — onboarding flag
```java
boolean isProfileComplete = Boolean.TRUE.equals(user.getOnboardingCompleted());
```
`Boolean.TRUE.equals(x)` is null-safe: true only if `x` is exactly `true`. Uses the real flag
`onboarding_completed` (fix C4) — not "gender/DOB not null", which placeholders always satisfy.

### Step 6 — build the response
```java
AuthTokenResponse.builder()
    .accessToken(...).tokenType("Bearer")
    .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)   // ms → seconds
    .userId(...).isProfileComplete(...)
    .preFilledUserData(PreFilledUserData.builder()...build())
    .build();
```
Builder pattern from note 07.

### `autoProvisionUser` (private)
Creates a new row with `AppUser.builder()`:
- real data: firebase uid, provider (GOOGLE/PHONE), name, avatar.
- **placeholders**: `dateOfBirth = 2000-01-01`, `gender = OTHERS` — because those DB columns are `NOT NULL`.
- `onboardingCompleted(false)` — marks the placeholders as "not real yet".
- `userRepository.save(newUser)` → INSERT; returns the saved entity **with its generated id**.

## 4. Real example
First login of a new Google user → no row → insert → token returned with `isProfileComplete=false`.
Second login → row found → no insert → same user id.

## 5. Mismatch checklist
| Item | Status |
| :--- | :--- |
| `Gender.OTHERS` (fix C3) | ✅ |
| Real `onboardingCompleted` flag (C4) | ✅ |
| `PreFilledUserData.fullName` for existing users | uses DB `fullName` (could be a placeholder `New Roommate` for phone users) — fine |
| Race: two simultaneous first logins | could try to insert the same `firebaseUid`; DB unique constraint would reject one. Edge case, ignore for now. |

## 6. Common errors
| Symptom | Reason |
| :--- | :--- |
| 401 "Invalid or expired Firebase ID token" | token fake/expired, or wrong Firebase project's key in the service account |
| Startup: `No qualifying bean of type FirebaseAuth` | `FirebaseConfig` failed or isn't scanned |
| 500 + DB `null value in column "gender"` | placeholder removed from `autoProvisionUser` |

## 7. Quiz
1. Why do new users get `2000-01-01` and `OTHERS`?
2. What's the difference between `orElse` and `orElseGet`?
3. What does `@Transactional` give you?
4. Why compute `expiresIn` as `ms / 1000`?

<details><summary>Answers</summary>

1. The columns are `NOT NULL`; the `onboardingCompleted=false` flag marks them as fake.
2. `orElseGet` runs its function only when empty (lazy); `orElse` always evaluates its argument.
3. All DB changes in the method commit together or roll back together.
4. JWT/OAuth convention: `expires_in` is in seconds; config stores milliseconds.
</details>
