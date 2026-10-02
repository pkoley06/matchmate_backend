# 06 — `FirebaseConfig` explained line by line

File: `config/FirebaseConfig.java`
Related: [`08-AuthService.md`](08-AuthService.md) (the user of the bean) · [`01-system-design.md`](01-system-design.md)

## 1. What is it?
It **logs our server into Firebase** and hands the rest of the app a ready-to-use `FirebaseAuth`
object. `FirebaseAuth` is the tool that answers: "is this Firebase ID token real?"

**Service account JSON** = a private key file that proves "I am the MatchMate server" to Google.
Never put it in Flutter, never commit it.

## 2. Who calls it
Spring at **startup**. It runs the `@Bean` method once. `AuthService` then receives the
`FirebaseAuth` through its constructor.

## 3. Code walk-through

### `@Configuration`
"This class creates beans." Spring reads it at startup.

### Logger
```java
private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
```
`static final` = one shared constant. `log.info(...)` writes to the console. (Same thing Lombok's
`@Slf4j` would give; here written by hand.)

### `ResourceLoader` and `@Value`
```java
private final ResourceLoader resourceLoader;
@Value("${firebase.credentials-path}")
private String credentialsPath;
```
- `@Value("${...}")` — copy a setting from `application.yml` into the field. Here:
  `firebase.credentials-path`, which currently is
  `${FIREBASE_CREDENTIALS_PATH:classpath:firebase/...json}` = "use the env var if set, else this default".
- `ResourceLoader` — Spring helper that opens files by a *prefixed* path: `classpath:` (inside the
  jar's resources), `file:` (a normal disk path). That is why one setting works for both.

### `@Bean public FirebaseAuth firebaseAuth()`
`@Bean` = "run once, keep the returned `FirebaseAuth` as a bean".

Inside, in order:
1. **Reuse guard**
   ```java
   if (!FirebaseApp.getApps().isEmpty()) return FirebaseAuth.getInstance();
   ```
   `FirebaseApp` is a global singleton in Google's SDK. Initializing it twice throws an error
   (can happen on dev-tools restart/tests), so reuse if it already exists.
2. **Empty path guard** → throws `IllegalStateException` with a clear message (fail fast, fix C7).
3. **Find the file**
   ```java
   Resource resource = resourceLoader.getResource(credentialsPath);
   if (!resource.exists()) throw ...
   ```
   Clear error instead of a mysterious crash later.
4. **Read credentials and initialize**
   ```java
   try (InputStream is = resource.getInputStream()) {
       FirebaseOptions options = FirebaseOptions.builder()
               .setCredentials(GoogleCredentials.fromStream(is))
               .build();
       FirebaseApp.initializeApp(options);
   }
   ```
   - `try (...)` = **try-with-resources**: closes the stream automatically.
   - `GoogleCredentials.fromStream` parses the JSON key.
   - `FirebaseOptions.builder()...build()` — **builder pattern**: set options step by step, then `build()`.
   - `FirebaseApp.initializeApp(options)` logs in to Firebase.
5. `catch (IOException e)` → wrap in a readable `IllegalStateException`.
6. `return FirebaseAuth.getInstance();` — the bean other classes use.

## 4. Real example
Startup log on success: `Initialized FirebaseApp with service account: classpath:firebase/...json`.
Missing file: app stops with `Firebase service account file not found at '...'`. That is
intentional — better to fail at start than at the first login.

## 5. Mismatch checklist
| Item | Status |
| :--- | :--- |
| Logic | ✅ matches the guide |
| Credentials location | ⚠️ `application.yml` still points to `src/main/resources/firebase/...json` — the private key gets packed into the jar. Fix **C2**: move to `secrets/` (git-ignored) and point `FIREBASE_CREDENTIALS_PATH` there. |
| Field `@Value` + constructor injection mixed | fine; works. (Cleaner later: `@ConfigurationProperties`.) |

## 6. Common errors
| Symptom | Reason |
| :--- | :--- |
| `Firebase service account file not found at ...` | wrong path / wrong prefix (`file:` vs `classpath:`) |
| `Could not read Firebase service account file` | file is not valid JSON / is the wrong kind of key |
| `FirebaseApp with name [DEFAULT] already exists` | removed the reuse guard |

## 7. Quiz
1. Why is `FirebaseAuth` a bean instead of created inside `AuthService`?
2. What does `classpath:` mean compared with `file:`?
3. Why throw an exception when the file is missing instead of continuing?
4. Why must the JSON key never go into the Flutter app?

<details><summary>Answers</summary>

1. One shared, configured instance; easy to inject and to replace with a mock in tests.
2. `classpath:` = inside the app's resources (packed in the jar); `file:` = a real path on disk.
3. Fail fast: a clear startup error beats a confusing crash on the first login.
4. It lets anyone act as your server toward Firebase (admin power); the app only needs `google-services.json`.
</details>
