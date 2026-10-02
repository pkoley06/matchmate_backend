# 05 — `SecurityConfig` explained line by line

File: `security/SecurityConfig.java`
Related: [`04-JwtAuthenticationFilter.md`](04-JwtAuthenticationFilter.md) · [`01-system-design.md`](01-system-design.md)

## 🚨 BUG in your current file — read this first
Your method has **no `@Bean` annotation**:
```java
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {   // ← missing @Bean above
```
Without `@Bean`, Spring treats it as an ordinary method and **never calls it**. Your rules
(permit `/api/v1/auth/**`, stateless, 401, your JWT filter placement) are all ignored and Spring
Boot's **default security** applies: every URL needs a login, including `/api/v1/auth/verify-token`.
Symptoms: login endpoint returns 401/403, a "Using generated security password" line in the log.

**Fix:** add `@Bean` and its import:
```java
import org.springframework.context.annotation.Bean;
...
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
```

## 1. What is it?
The **rulebook**. It answers: which URLs are public? which need a logged-in user? do we use
sessions? what do we answer when nobody is logged in? where does our JWT filter sit?

## 2. Who calls it
Spring, once, **at startup**. It runs the `@Bean` method, gets a `SecurityFilterChain`, and uses
that for every request afterwards. You never call it yourself.

## 3. Code walk-through

### Class annotations
- `@Configuration` — "this class creates beans". Spring reads it at startup.
- `@EnableWebSecurity` — turns on Spring Security's web support and lets you define your own chain.

### Constructor injection
Gets the `JwtAuthenticationFilter` bean (note 04) so we can put it in the chain.

### `@Bean` + `SecurityFilterChain securityFilterChain(HttpSecurity http)`
- `@Bean` — "run this method once, keep what it returns as a bean".
- `HttpSecurity http` — a builder Spring passes in; you describe your rules on it.
- `SecurityFilterChain` — the finished ordered list of security filters.
- `throws Exception` — `http.build()` declares it.

Each rule in order:

| Code | Meaning | Why |
| :--- | :--- | :--- |
| `.csrf(AbstractHttpConfigurer::disable)` | turn CSRF protection off | CSRF abuses browser **cookies**. We send a token in a header, no cookies. `AbstractHttpConfigurer::disable` is a method reference = "call `disable` on the config". |
| `.cors(Customizer.withDefaults())` | enable CORS with defaults | Only matters for browsers (web frontends). Flutter mobile ignores it. |
| `.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))` | never create a server session | Each request proves itself with the JWT. The `s -> ...` is a **lambda** (a tiny inline function). |
| `.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED)))` | not logged in → **401** | Spring's default is 403. `TASKS.md` requires 401 (fix C5). "Entry point" = "what to do with someone who isn't authenticated". |
| `.requestMatchers("/api/v1/auth/**").permitAll()` | public | Login must work without a token. `**` = anything below this path. |
| `.requestMatchers("/error").permitAll()` | public | Spring forwards failures to `/error`; blocking it would hide your real error messages. |
| `.anyRequest().authenticated()` | everything else needs a user | "Authenticated" = the filter put someone on the clipboard. **Order matters**: first matching rule wins, so this catch-all is last. |
| `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` | put our guard in the chain | Must run before Spring's own login filter so the user is already identified. It is an *anchor position*, not that we use that filter. |
| `return http.build();` | finish and hand the chain to Spring | |

## 4. Real example
| Request | Rule hit | Result |
| :--- | :--- | :--- |
| `POST /api/v1/auth/verify-token` | `permitAll` | reaches controller |
| `PUT /api/v1/users/me/demographics` + good token | filter sets user → `authenticated()` | reaches controller |
| same, no token | clipboard empty → entry point | **401** |

## 5. Mismatch checklist
| Item | Yours | Needed | Action |
| :--- | :--- | :--- | :--- |
| `@Bean` on `securityFilterChain` | ❌ missing | required | add it (see top) |
| Rest of the rules | ✅ | matches guide | — |
| Unneeded: `@Component` on the filter + addFilterBefore | fine | Boot may also auto-register the filter as a plain servlet filter; `OncePerRequestFilter` prevents double runs | no action |

## 6. Common errors
| Symptom | Reason |
| :--- | :--- |
| Log: `Using generated security password: ...` | Your chain is not used → missing `@Bean` |
| Login endpoint gives 401/403 | same |
| 403 instead of 401 for no token | `exceptionHandling` line missing/not applied |
| Error text never appears | `/error` not permitted, or `server.error.include-message` not set (C6, still missing in your `application.yml`) |

## 7. Quiz
1. What does `@Bean` do to a method?
2. Why is `anyRequest().authenticated()` last?
3. Why can CSRF be disabled for this API?
4. Which class actually decides *who* the user is, and which decides *what is allowed*?

<details><summary>Answers</summary>

1. Spring runs it at startup and keeps the returned object as a managed bean (here, the security rules).
2. Rules match top to bottom; the catch-all would swallow the specific ones.
3. No cookies are used; the token travels in a header the browser doesn't add automatically.
4. `JwtAuthenticationFilter` identifies; `SecurityConfig` decides.
</details>
