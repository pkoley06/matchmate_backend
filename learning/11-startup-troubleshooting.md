# 11 — First `bootRun`: "missing table [app_users]" and how to fix it

> Related: [`README.md`](README.md) · [`01-system-design.md`](01-system-design.md) · `BACKEND_PROGRESS.md` (Step 2.1b)

## What you saw (2026-10-01)
```
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException:
           Schema validation: missing table [app_users]
> Task :bootRun FAILED
```
Your Java code compiles and Spring starts. It dies while Hibernate checks the database.

## Real-life analogy
Hibernate is a **building inspector** (`ddl-auto: validate`). It walks in and checks: "the blueprint
(your `AppUser` entity) says there is a room called `app_users` — where is it?"
**Flyway** is the **builder** who is supposed to build the rooms from your SQL files (`V1`, `V2`, ...)
*before* the inspector arrives. Your builder never showed up, so the building is empty.

## Why the builder did not show up (cause 1 — the real one)
In **Spring Boot 4** the auto-configuration was split into small modules. Having
`flyway-core` on the classpath is **no longer enough** to make Spring Boot run Flyway.
You need the Boot starter that contains the glue code.

Proof from your log: there is **no** line containing `Flyway` / `Migrating schema` — Flyway never ran.

### Fix (edit `build.gradle`, `dependencies { ... }`)
Replace the two Flyway lines:
```gradle
	implementation 'org.flywaydb:flyway-core'
	implementation 'org.flywaydb:flyway-database-postgresql'
```
with:
```gradle
	implementation 'org.springframework.boot:spring-boot-starter-flyway'
	implementation 'org.flywaydb:flyway-database-postgresql'
```
(Keep `flyway-database-postgresql`; the starter already brings `flyway-core`.)
The starter exists for 4.1.1 (checked on Maven Central).

## The next error you will hit (cause 2 — missing `V3`)
After Flyway runs, `V1` and `V2` create the tables. Then the inspector checks `AppUser` again and finds
a field the table does not have:
```
Schema-validation: missing column [onboarding_completed] in table [app_users]
```
Your entity already has `onboardingCompleted`, but `src/main/resources/db/migration/` only
contains `V1` and `V2`. Create **`V3__add_onboarding_completed.sql`** in that folder:
```sql
ALTER TABLE app_users
    ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;
```
(Exact copy of Step 2.1b in `BACKEND_PROGRESS.md`. Never edit `V1`/`V2` once they have run.)

## The third problem (cause 3 — appears when you call the API, not at startup)
`SecurityConfig.securityFilterChain` has no `@Bean` annotation, so Spring ignores your security
rules and uses its default ("everything needs a login"). Login returns 401/403 and the log says
`Using generated security password`. Add `@Bean` above the method. Full explanation:
[`05-SecurityConfig.md`](05-SecurityConfig.md).

## Checklist after all three fixes
1. `docker ps` → container `matchmate-db` is `Up` (it is, as of today).
2. `./gradlew bootRun`
3. In the log you should now see, in this order:
   - `Successfully applied 3 migrations to schema "public"`  ← Flyway
   - Hibernate lines (no `SchemaManagementException`)
   - `Started MatchmateBackendApplication in ... seconds`
4. If you see a **different** error, check the "Common errors" table in `BACKEND_PROGRESS.md`.

## If Flyway complains about the existing database
`baseline-on-migrate: true` is set in `application.yml`. If the DB had leftover tables from an
earlier experiment and Flyway behaves strangely, reset the dev database (this **deletes all dev data**):
```powershell
docker-compose down -v
docker-compose up -d
```

## Other things I noticed (not blocking startup)
- `application.yml` still contains the **public sample JWT secret** (`404E6352...`) and the Firebase
  JSON path points at `src/main/resources/firebase/...`. These are fixes **C1** and **C2** (Step 2.0 /
  2.1). The app will run, but do them before sharing the code or pushing to git.
- `src/main/resources/firebase/*.json` is a private key inside the jar's resources folder.

## Mini quiz
1. What does `ddl-auto: validate` do, and what does it NOT do?
2. Which tool creates the tables, and in what order does it run its files?
3. Why is `flyway-core` alone not enough in Spring Boot 4?
4. Why must you add `V3` instead of editing `V1`?

<details><summary>Answers</summary>

1. It only *checks* entities against the existing tables. It never creates or changes tables.
2. Flyway; files run in version order (`V1`, `V2`, `V3`...) and each runs only once.
3. Boot 4 moved Flyway auto-configuration into `spring-boot-starter-flyway`; without it nothing triggers the migrations.
4. Flyway stores a checksum of every applied file; editing an applied one makes startup fail. A new file is the safe way to change the schema.
</details>
