# 04 — `JwtAuthenticationFilter` explained line by line

File: `security/JwtAuthenticationFilter.java`
Related: [`01-system-design.md`](01-system-design.md) (Flow B) · [`02-JwtTokenProvider.md`](02-JwtTokenProvider.md) · [`03-AuthenticatedUserPrincipal.md`](03-AuthenticatedUserPrincipal.md) · [`05-SecurityConfig.md`](05-SecurityConfig.md)

## 1. What is it?
The **guard at the door**. For every HTTP request it looks at the `Authorization` header. If there
is a valid MatchMate token, it writes "this request belongs to user #5" on Spring Security's
clipboard (`SecurityContextHolder`). If there is no token or a bad one, it writes **nothing** and
lets the request continue. It never says "no" itself — the "no" (401) comes from `SecurityConfig`.

**Filter** = code that runs *before* your controller, for every request (like airport security
before the gate).

## 2. Who calls it, what it calls
| | |
| :--- | :--- |
| Called by | The Spring Security filter chain, automatically, on every request (needs `SecurityConfig` to place it in the chain) |
| Calls | `JwtTokenProvider` (validate + read token), creates `AuthenticatedUserPrincipal`, writes to `SecurityContextHolder` |
| When | Every request (login and all later calls) |

## 3. Code walk-through

### `@Component`
Tells Spring "create **one** object of this class at startup and keep it as a bean". Needed so it
can be injected into `SecurityConfig`.

### `extends OncePerRequestFilter`
A Spring base class that guarantees `doFilterInternal` runs **exactly once per request** (some
requests get forwarded internally and would otherwise run filters twice). You only fill in one method.

### Constructor injection
```java
private final JwtTokenProvider jwtTokenProvider;
public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) { this.jwtTokenProvider = jwtTokenProvider; }
```
Spring sees the constructor needs a `JwtTokenProvider`, finds that bean, and passes it in.
This is **dependency injection**: you don't write `new JwtTokenProvider()`, Spring hands it to you.
(`final` = cannot be replaced later; one constructor = no `@Autowired` needed.)

### `doFilterInternal(request, response, filterChain)`
The method Spring calls per request.
- `HttpServletRequest request` — everything about the incoming call (headers, URL...).
- `HttpServletResponse response` — what we send back (not used here).
- `FilterChain filterChain` — "the rest of the line". Calling `filterChain.doFilter(...)` passes the
  request on to the next filter and finally the controller. **If you forget this line, the request
  stops here and nothing is returned.**
- `throws ServletException, IOException` — required by the parent method signature.
- `@Override` — we replace the parent's empty method; compiler checks the name is correct.

### Step by step inside
```java
String jwt = extractJwtFromRequest(request);
```
Get the raw token text or `null`.

```java
if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
```
- `StringUtils.hasText` — true if not null, not empty, not only spaces.
- `&&` short-circuits: if there is no token, `validateToken` is never called.
- `validateToken` — checks signature and expiry (note 02).

```java
Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
String firebaseUid = jwtTokenProvider.getFirebaseUidFromToken(jwt);
AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(userId, firebaseUid);
```
Read the two facts from the token and build the "ID card" (note 03). `new` each time — it is not a bean.

```java
UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
```
Spring Security wants an `Authentication` object. This class is the standard one. The 3 arguments:
**who** (principal), **credentials** (`null` — no password), **roles** (authorities). Using the
3-argument constructor marks it as *already authenticated*.

```java
authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
```
Attaches extra info (client IP, session id) to the object. Optional, harmless; useful for logging.

```java
SecurityContextHolder.getContext().setAuthentication(authentication);
```
**The key line.** Stores the authentication for the current request thread. Later,
`@AuthenticationPrincipal` in a controller reads it from here.

```java
filterChain.doFilter(request, response);
```
Always runs — with or without a user. Continue to the next step.

### `extractJwtFromRequest` (private helper)
```java
String bearerToken = request.getHeader("Authorization");   // "Bearer eyJhbGci..."
if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
    return bearerToken.substring(7);                        // drop "Bearer " (7 characters incl. the space)
}
return null;
```
`"Bearer "` is 7 characters, so `substring(7)` leaves only the token.

## 4. Real example
| Request | `jwt` | Valid? | Result |
| :--- | :--- | :--- | :--- |
| `PUT /me/demographics` + `Authorization: Bearer <good>` | token text | yes | clipboard = user 5 → controller runs |
| same, no header | `null` | — | clipboard empty → `SecurityConfig` answers **401** |
| same, expired token | token text | no | clipboard empty → **401** |

## 5. Mismatch checklist: your code vs guide
| Item | Status |
| :--- | :--- |
| Logic (extract → validate → principal → context → continue) | ✅ matches guide |
| Unused import `WebAuthenticationDetails` | harmless; delete it (only `WebAuthenticationDetailsSource` is used) |
| Spaces after `if(` / `){` | style only |

## 6. Common errors
| Symptom | Reason |
| :--- | :--- |
| Every request is 401 even with a good token | `SecurityConfig` is not registering the filter/chain (see note 05 — missing `@Bean`) |
| Request hangs / empty response | missing `filterChain.doFilter(...)` |
| `Authorization` header ignored | must start with exactly `Bearer ` (capital B, one space) |

## 7. Quiz
1. Why doesn't the filter return 401 itself when the token is bad?
2. What does `substring(7)` remove?
3. Why is `AuthenticatedUserPrincipal` created with `new` here instead of injected?
4. What happens if you delete the last line `filterChain.doFilter(...)`?

<details><summary>Answers</summary>

1. Its job is only to *identify* the user. Which URLs need a user is decided in `SecurityConfig`, which answers 401.
2. The 7 characters `Bearer ` (including the space).
3. It holds one request's user; a shared bean would mix users up.
4. The request never reaches the controller; the client gets an empty response.
</details>
