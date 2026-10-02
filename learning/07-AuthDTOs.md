# 07 — Auth DTOs: `TokenVerifyRequest`, `AuthTokenResponse`, `PreFilledUserData`

Files: `dto/request/TokenVerifyRequest.java`, `dto/response/AuthTokenResponse.java`, `dto/response/PreFilledUserData.java`
Related: [`08-AuthService.md`](08-AuthService.md) · [`09-AuthController.md`](09-AuthController.md) · [`10-UserFlow.md`](10-UserFlow.md) (the demographics DTOs use the same annotations)

## 1. What is a DTO?
**D**ata **T**ransfer **O**bject = a plain class that describes the **shape of JSON** going in or out
of the API. It is *not* a database table (that is an entity) and has no logic.
Why separate? So you never expose DB columns by accident and can validate input in one place.

Analogy: a form. `TokenVerifyRequest` = the form Flutter fills in. `AuthTokenResponse` = the receipt it gets back.

## 2. Lombok annotations used (same on every DTO)
Lombok writes boring code for you **at compile time** (you don't see it in the file).

| Annotation | Generates | Needed because |
| :--- | :--- | :--- |
| `@Data` | getters, setters, `toString`, `equals`, `hashCode` | Jackson (JSON library) reads/writes fields through them |
| `@NoArgsConstructor` | `new X()` with no arguments | Jackson creates the object empty, then fills it |
| `@AllArgsConstructor` | constructor with every field | needed by `@Builder` |
| `@Builder` | `X.builder().a(1).b(2).build()` | readable object creation; used in `AuthService` |
| `@Getter` (extra on `TokenVerifyRequest`) | getters | **redundant** — `@Data` already includes it; safe to delete |

## 3. `TokenVerifyRequest` — Flutter → server
```json
{ "idToken": "eyJhbGciOi...", "provider": "GOOGLE" }
```
```java
@NotBlank(message = "Firebase ID token is required") private String idToken;
@NotNull(message = "Primary auth provider is required") private PrimaryAuthProvider provider;
```
- `idToken` — the token Firebase gave the app after Google/phone login.
- `provider` — an **enum** (`PrimaryAuthProvider`: e.g. `GOOGLE`, `PHONE`). JSON text `"GOOGLE"` is converted to the enum value; any other text → 400.
- `@NotBlank` — not null, not empty, not only spaces (for Strings).
- `@NotNull` — not null (for non-String types).
- These annotations do **nothing alone**; they are checked when the controller parameter has `@Valid` (note 09). Failure → HTTP 400 with your `message`.

## 4. `AuthTokenResponse` — server → Flutter
```json
{ "accessToken":"...", "tokenType":"Bearer", "expiresIn":86400, "userId":5,
  "isProfileComplete":false, "preFilledUserData":{ "fullName":"...", "email":"...", "avatarUrl":"..." } }
```
| Field | Meaning |
| :--- | :--- |
| `accessToken` | **our** JWT; Flutter sends it as `Authorization: Bearer ...` |
| `tokenType` | always `"Bearer"` |
| `expiresIn` | seconds until it expires (`expirationMs / 1000` = 86400) |
| `userId` | the DB id |
| `isProfileComplete` | `false` → Flutter shows the onboarding form |
| `preFilledUserData` | nested object to pre-fill that form |

`Boolean` (capital B) rather than `boolean` so it can be `null` and Lombok names the getter `getIsProfileComplete`; JSON key stays `isProfileComplete`.

## 5. `PreFilledUserData`
Just three strings (`fullName`, `email`, `avatarUrl`) that Google already knows, so the user doesn't retype them.

## 6. Where they are used
`AuthController` receives `TokenVerifyRequest` → `AuthService` builds `AuthTokenResponse` (containing `PreFilledUserData`) → controller returns it.

## 7. Mismatch checklist
| Item | Status |
| :--- | :--- |
| Fields and validation messages | ✅ match guide |
| `@Getter` duplicate on `TokenVerifyRequest` | harmless, remove |
| `import lombok.*;` | works, but explicit imports are tidier |

## 8. Common errors
| Symptom | Reason |
| :--- | :--- |
| `400` on login with message about `provider` | JSON value isn't a `PrimaryAuthProvider` constant (check spelling/case) |
| `400` "Firebase ID token is required" | `idToken` missing/blank |
| Validation not triggered | forgot `@Valid` in the controller |
| `cannot find symbol builder()` | Lombok annotation processor not enabled in IntelliJ |

## 9. Quiz
1. Difference between a DTO and an entity?
2. Why does Jackson need `@NoArgsConstructor` and setters?
3. `@NotBlank` vs `@NotNull`?
4. Which annotation makes `AuthTokenResponse.builder()` possible?

<details><summary>Answers</summary>

1. DTO = JSON shape of the API; entity = database table row.
2. It creates an empty object and fills the fields one by one.
3. `@NotBlank` is for Strings and also rejects empty/space-only; `@NotNull` only rejects `null`.
4. `@Builder` (with `@AllArgsConstructor`).
</details>
