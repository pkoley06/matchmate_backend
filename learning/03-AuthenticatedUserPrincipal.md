# 03 — `AuthenticatedUserPrincipal` explained line by line

File: `src/main/java/com/matcher/matchmate_backend/security/AuthenticatedUserPrincipal.java`
Related: [`01-system-design.md`](01-system-design.md) · [`02-JwtTokenProvider.md`](02-JwtTokenProvider.md)

## 1. What is it?
A tiny **ID card object** that says "the person making *this* request is user #5 (Firebase uid
abc)". It holds no logic — just data — but Spring Security requires the current user to be
represented by an object of a specific shape (`UserDetails`), so we wrote this class to fit.

**Principal** = "the person acting right now". Simple word, simple meaning.

## 2. Who creates it, who uses it
| Step | Who | What |
| :--- | :--- | :--- |
| Create | `JwtAuthenticationFilter` (to be written) | `new AuthenticatedUserPrincipal(userId, firebaseUid)` after the token is validated |
| Store | `SecurityContextHolder` | kept for the duration of that one request |
| Use | Controllers | `@AuthenticationPrincipal AuthenticatedUserPrincipal me` → `me.getUserId()` |

It is **not** a bean (no `@Component`). A fresh object per request, because each request has a
different user.

---

## 3. Code walk-through

### `public class AuthenticatedUserPrincipal implements UserDetails`
- `implements UserDetails` — `UserDetails` is a Spring Security **interface** (a list of methods
  a class promises to have). We *must* write all of them, which is why there are so many
  small methods below. Spring calls them; we just answer.

### Fields and `@Getter`
```java
@Getter private final Long userId;
@Getter private final String firebaseUid;
private final Collection<? extends GrantedAuthority> authorities;
```
- `@Getter` (Lombok) → generates `getUserId()` and `getFirebaseUid()` at compile time.
  Same as the guide's hand-written getters.
- `final` → cannot change after the constructor → nobody can swap the user mid-request.
- `Long` (capital L, object) matches the database id type.
- `Collection<? extends GrantedAuthority>` — "a list of things that are some kind of permission".
  `? extends` = "any subclass of `GrantedAuthority` is fine". You can treat it as "list of roles".

### Constructor
```java
this.authorities = Collections.singletonList(new SimpleGrantedAuthority("USER"));
```
- `SimpleGrantedAuthority("USER")` — one permission label.
- `Collections.singletonList(x)` — a read-only list containing exactly one item.
- So every user has exactly one role for now.

> ⚠️ **Mismatch:** the guide uses `"ROLE_USER"`, you wrote `"USER"`. Spring has two ways to check:
> - `hasRole("USER")` → internally looks for **`ROLE_USER`** (adds the prefix itself)
> - `hasAuthority("USER")` → looks for exactly **`USER`**
>
> Nothing checks roles yet, so nothing breaks today. But the JWT claim says `"ROLE_USER"`, and
> later `hasRole("USER")` would fail with your `"USER"`. Recommended: change to `"ROLE_USER"`.

### Methods with `@Override`
`@Override` = "I'm replacing/implementing a method from the parent/interface". The compiler
then warns you if you misspell the method name.

| Method | Spring asks… | We answer | Why |
| :--- | :--- | :--- | :--- |
| `getAuthorities()` | what may this user do? | the roles list | authorization checks |
| `getPassword()` | what's the password? | `null` | no passwords; Firebase did the login |
| `getUsername()` | what's the username? | `String.valueOf(userId)` | Spring insists on a String; the id is our unique key. `String.valueOf(5L)` → `"5"` |
| `isAccountNonExpired()` | account expired? | `true` (= not expired) | JWT expiry already handles time limits |
| `isAccountNonLocked()` | locked? | `true` (= not locked) | no lock feature yet |
| `isCredentialsNonExpired()` | password expired? | `true` | no passwords |
| `isEnabled()` | active? | `true` | later: return `false` for banned users |

⚠️ Read the names carefully: **`true` means "everything is fine"**. `isAccountNonLocked() == true`
means the account is *not* locked.

---

## 4. Real example
Token for user 5 arrives → filter runs:
```java
var p = new AuthenticatedUserPrincipal(5L, "fbUid123");
p.getUserId();      // 5
p.getFirebaseUid(); // "fbUid123"
p.getUsername();    // "5"
p.getPassword();    // null
p.getAuthorities(); // [USER]   (or [ROLE_USER] after the fix)
```
In a controller later:
```java
@PutMapping("/me/demographics")
public ... update(@AuthenticationPrincipal AuthenticatedUserPrincipal me, ...) {
    Long id = me.getUserId();   // no DB query needed to know who is calling
}
```

## 5. Mismatch checklist: your code vs guide
| Item | Yours | Guide | Action |
| :--- | :--- | :--- | :--- |
| Authority text | `"USER"` | `"ROLE_USER"` | change to `"ROLE_USER"` |
| Getters | Lombok `@Getter` | hand-written | equivalent, fine |
| `@Override` methods | all 7 present | same | ✅ |

## 6. Why not just pass `userId` around?
Spring Security only understands "an authenticated `Authentication` object with a principal".
Using its standard shape gives you `@AuthenticationPrincipal`, `hasRole(...)`, and testing
tools for free.

## 7. Quiz
1. Why is `getPassword()` `null`?
2. Is this class a Spring bean? Why or why not?
3. What does `isEnabled()` returning `true` mean?
4. What's the difference between `hasRole("USER")` and `hasAuthority("USER")`?

<details><summary>Answers</summary>

1. Login is done by Firebase; our JWT replaces the password afterwards.
2. No — it's created with `new` per request because it stores that request's user.
3. The account is active/allowed. Return `false` to block a user.
4. `hasRole` adds the `ROLE_` prefix (looks for `ROLE_USER`); `hasAuthority` matches the exact text.
</details>
