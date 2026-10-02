# 09 — `AuthController` explained line by line

File: `controller/AuthController.java`
Related: [`08-AuthService.md`](08-AuthService.md) · [`07-AuthDTOs.md`](07-AuthDTOs.md) · [`05-SecurityConfig.md`](05-SecurityConfig.md)

## 1. What is it?
The **front desk**. It receives the HTTP call `POST /api/v1/auth/verify-token`, turns the JSON into
a Java object, validates it, hands it to `AuthService`, and returns the result as JSON.
It contains **no business logic** on purpose.

## 2. Who calls it
Flutter (over HTTP). `SecurityConfig` lets `/api/v1/auth/**` through without a token.

## 3. Code walk-through

### `@RestController`
= `@Controller` + `@ResponseBody`. It marks the class as a web endpoint holder **and** says "whatever
a method returns, write it as JSON into the response" (instead of looking for an HTML page).

### `@RequestMapping("/api/v1/auth")`
A **prefix** for every method in the class. `v1` = API version, so a future `v2` won't break old apps.

### Constructor injection
Gets the `AuthService` bean. No `@Autowired` needed with a single constructor.

### `@PostMapping("/verify-token")`
"Run this method for HTTP **POST** on `<prefix>/verify-token`" → full URL `/api/v1/auth/verify-token`.
POST because the client sends data and the call has a side effect (may create a user).
(Others: `@GetMapping` read, `@PutMapping` replace/update, `@DeleteMapping` delete.)

### The method
```java
public ResponseEntity<AuthTokenResponse> verifyToken(@Valid @RequestBody TokenVerifyRequest request)
```
- `@RequestBody` — take the JSON body and convert it into a `TokenVerifyRequest` (Jackson does this; needs the no-args constructor + setters from note 07).
- `@Valid` — run the `@NotBlank`/`@NotNull` checks on that object. On failure Spring answers **400** before your method body runs.
- `ResponseEntity<AuthTokenResponse>` — the response **plus** control of status/headers. `T` is the body type.

```java
AuthTokenResponse response = authService.verifyAndAuthenticate(request);
return ResponseEntity.ok(response);     // HTTP 200 + JSON body
```

## 4. Real example
```
POST /api/v1/auth/verify-token
{"idToken":"eyJ...","provider":"GOOGLE"}
→ 200 {"accessToken":"...","tokenType":"Bearer","expiresIn":86400,"userId":5,"isProfileComplete":false,...}
```
Bad token → `AuthService` throws `ResponseStatusException(401)` → 401. Blank `idToken` → 400.

## 5. Mismatch checklist
| Item | Status |
| :--- | :--- |
| Mapping, validation, service call | ✅ matches guide |
| Error text visible in the response | needs `server.error.include-message: always` in `application.yml` (fix **C6** — not in your file yet) |

## 6. Common errors
| Symptom | Reason |
| :--- | :--- |
| 401/403 on this URL with no token | `SecurityConfig` not active (missing `@Bean`, note 05) |
| 404 | URL typo; prefix + method path must match exactly |
| 415 Unsupported Media Type | client forgot `Content-Type: application/json` |
| 400, body empty | `include-message` not enabled (C6) |
| 400 "JSON parse error" | `provider` isn't a valid enum value or JSON malformed |

## 7. Quiz
1. What do `@RestController` and `@Controller` differ in?
2. Which annotation turns JSON into a Java object? Which checks the rules?
3. Why is the login endpoint `POST` and not `GET`?
4. Why put business rules in the service, not here?

<details><summary>Answers</summary>

1. `@RestController` adds `@ResponseBody`: return values become JSON.
2. `@RequestBody` converts; `@Valid` validates.
3. It sends a secret in a body (not in a URL/logs) and can create data.
4. The controller only handles HTTP; the service can be reused and tested without HTTP.
</details>
