# 02 — `JwtTokenProvider` explained line by line

File: `src/main/java/com/matcher/matchmate_backend/security/JwtTokenProvider.java`
Related: [`01-system-design.md`](01-system-design.md) · [`03-AuthenticatedUserPrincipal.md`](03-AuthenticatedUserPrincipal.md)

## 1. What is it? (one paragraph)
The **token factory + token checker**. It is the only class that knows how to (a) **create** our
JWT after login and (b) **open / verify** a JWT that arrives with a request. Nobody else touches
the secret key.

## 2. What is a JWT? (needed to understand the class)
A string of 3 parts separated by dots: `HEADER.PAYLOAD.SIGNATURE`
- **Header:** which algorithm signed it (here HS256).
- **Payload:** small facts called *claims*, e.g. `{"sub":"5","firebaseUid":"abc","exp":1790000000}`.
  **Anyone can read it** (it's only Base64, not encrypted). Never put passwords in it.
- **Signature:** `HMAC-SHA256(header+payload, secret)`. If someone edits the payload (e.g. changes
  `sub` from 5 to 1), the signature no longer matches → we reject. Only our server knows the secret.

## 3. Who calls it and when
| Method | Called by | When |
| :--- | :--- | :--- |
| constructor | Spring | once at startup |
| `generateToken` | `AuthService` (to be written) | end of login |
| `validateToken` | `JwtAuthenticationFilter` (to be written) | every request |
| `getUserIdFromToken` | `JwtAuthenticationFilter` | every valid request |
| `getExpirationMs` | `AuthService` | to tell Flutter the lifetime |

---

## 4. Code walk-through

### Imports (top of the file)
| Import | Meaning |
| :--- | :--- |
| `StandardCharsets` | says "use UTF-8" when turning text into bytes |
| `Date` | Java's old time object; JJWT wants `Date` for issued/expiry time |
| `SecretKey` | a cryptographic key object (not just a string) |
| `Claims` | JJWT's object holding the payload facts |
| `JwtException` | parent of *all* JWT problems (expired, tampered, malformed…) |
| `Jwts` | JJWT's entry point: `Jwts.builder()` to create, `Jwts.parser()` to read |
| `Keys` | helper to build a proper key from bytes |
| `Logger`, `LoggerFactory` | SLF4J logging (better than `System.out.println`) |
| `@Value` | Spring annotation: copy a value from `application.yml` |
| `@Component` | Spring annotation: "make one object of this class and manage it" |
| `@Getter` (Lombok) | auto-writes a getter method at compile time |

### Annotations
- **`@Component`** on the class — At startup Spring finds this class, creates **one** instance
  (a *bean*), and gives that same instance to anyone who asks in their constructor
  (*dependency injection*). That's why `AuthService` will just write
  `JwtTokenProvider tokenProvider` in its constructor and never `new` it.
- **`@Value("${jwt.secret}")`** on a constructor parameter — "take `jwt.secret` from
  `application.yml` and pass it here". `${JWT_SECRET}` in the yml means it comes from your
  Windows environment variable.
- **`@Getter`** on `expirationMs` — Lombok generates `public long getExpirationMs()` for you.
  (Lombok works at compile time; you won't see the method in the file. IntelliJ needs the Lombok
  plugin/annotation processing on to show it.)

### Fields
```java
private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
```
`static final` = one shared logger for the whole class, never reassigned. Used in `validateToken`.

```java
private final SecretKey signingKey;
@Getter private final long expirationMs;
private final String issuer;
```
`final` = set once in the constructor, never changed → safe for many requests at the same time
(thread-safe). All three are read-only settings.

### Constructor
```java
public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                        @Value("${jwt.expiration-ms}") long expirationMs,
                        @Value("${jwt.issuer}") String issuer)
```
1. Spring calls this once at startup, filling the 3 values from `application.yml`.
2. `secret.getBytes(StandardCharsets.UTF_8)` — text → bytes.
3. `Keys.hmacShaKeyFor(bytes)` — bytes → `SecretKey`. **Throws `WeakKeyException` if fewer than
   32 bytes** (256 bits). If your app refuses to start with that error, your secret is too short.
   JJWT picks HS256 / HS384 / HS512 automatically from the key length.
4. Stores expiry and issuer.

### `generateToken(Long userId, String firebaseUid)`
```java
Date now = new Date();
Date expiryDate = new Date(now.getTime() + expirationMs);
```
`now.getTime()` = milliseconds since 1970. Add 86,400,000 ms (24 h) → the expiry moment.

```java
return Jwts.builder()
    .subject(String.valueOf(userId))     // "sub": who the token is about (must be a String)
    .claim("firebaseUid", firebaseUid)   // custom fact
    .claim("role", "ROLE_USER")          // custom fact (not used yet)
    .issuer(issuer)                      // "iss": who created it
    .issuedAt(now)                       // "iat"
    .expiration(expiryDate)              // "exp": after this time the token is dead
    .signWith(signingKey)                // adds the signature
    .compact();                          // builds the final "xxx.yyy.zzz" string
```
It's the *builder pattern*: chain calls, finish with a "build" call (`compact()` here).

### `getClaimsFromToken(String token)` (private helper)
```java
Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
```
- `parser()` → create a reader; `verifyWith(key)` → "check signatures with this key";
  `build()` → finish the reader; `parseSignedClaims(token)` → **verify signature + expiry, then
  open the token** (throws `JwtException` if anything is wrong); `getPayload()` → the `Claims`.
- `private` = only used inside this class, as a shared helper.

### `getUserIdFromToken(String token)`  ⚠️ **contains a bug**
Your code:
```java
public String getUserIdFromToken(String token){
    Claims claims = getClaimsFromToken(token);
    return claims.get("firebaseUid", String.class);   // ← WRONG claim
}
```
The name and comment say "user ID", but it returns the **Firebase UID**. The user id is stored in
`subject`. **Fix (guide version):**
```java
public Long getUserIdFromToken(String token){
    Claims claims = getClaimsFromToken(token);
    return Long.parseLong(claims.getSubject());
}

public String getFirebaseUidFromToken(String token){
    Claims claims = getClaimsFromToken(token);
    return claims.get("firebaseUid", String.class);
}
```
Why it matters: `AuthenticatedUserPrincipal` needs a `Long userId` and a `String firebaseUid`.
With the bug, the filter would put the Firebase UID where the DB id belongs, and the database
lookups by user id would break. Also the return type should be `Long`, not `String`
(`claims.get("x", String.class)` = "read custom claim x as a String").

### `validateToken(String token)`
```java
try { Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token); return true; }
catch (JwtException | IllegalArgumentException e) { log.error(...); return false; }
```
- Same parse as the helper, but we only care "did it throw?".
- `catch (A | B e)` — one catch block for two exception types.
  - `JwtException` family: `ExpiredJwtException` (too old), `SignatureException`
    (tampered / wrong secret), `MalformedJwtException` (garbage text), unsigned tokens rejected.
  - `IllegalArgumentException`: token was `null` or empty.
- `log.error("... {}", msg)` — `{}` is replaced by the message (efficient logging).
- Returns `false` instead of crashing, so the filter can simply say "not logged in".

---

## 5. Mismatch checklist: your code vs `BACKEND_PROGRESS.md`
| Item | Yours | Guide | Action |
| :--- | :--- | :--- | :--- |
| `getUserIdFromToken` | returns `String`, reads `firebaseUid` | `Long`, reads `subject` | **fix (bug)** |
| `getFirebaseUidFromToken` | missing | exists | **add** — filter needs it |
| helper name | `getClaimsFromToken` | `getClaims` | fine, just a name |
| `getExpirationMs` | `@Getter` | manual method | equivalent, fine |

## 6. Common errors you may hit
| Error | Cause |
| :--- | :--- |
| `Could not resolve placeholder 'JWT_SECRET'` | env var not set / IDE not restarted |
| `WeakKeyException` | secret shorter than 32 bytes |
| every request is 401 right after login | token validated with a different secret than it was signed with |

## 7. Quiz
1. Why is the payload not safe for secrets?
2. What does `final` on `signingKey` buy us?
3. What is wrong with `getUserIdFromToken` right now?
4. Why does `validateToken` return `false` instead of throwing?

<details><summary>Answers</summary>

1. It's only Base64-encoded; anyone can decode it. The signature prevents *changes*, not *reading*.
2. Set once, never modified → safe when many requests use it at once.
3. It reads the `firebaseUid` claim, not `subject`, and returns `String` instead of `Long`.
4. So the caller (filter) can treat an invalid token as "anonymous" and let `SecurityConfig` decide.
</details>
