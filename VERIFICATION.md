# Verification — 8 September 2026

The complete application was built and launched on Windows using Java 17.0.12 and Node 22.14.0.

| Check | Result |
|---|---|
| TypeScript compilation | Passed |
| Vite production build | Passed; final assets embedded in the Java JAR |
| React interaction tests | 5 passed, 0 failed |
| Spring Boot SQL/API integration tests | 7 passed, 0 failed |
| File database restart test | 1 passed, 0 failed |
| Packaged Java application | Started successfully with `start.ps1` |
| App page and compiled assets over HTTP | 200 OK |
| Live `/api/health` | Application UP, database UP |
| Live CRUD for vehicles, drivers, trips, maintenance | Passed create, retrieve, update and delete |
| Live overlapping trip assignment | Rejected with HTTP 409 |
| Smoke-test cleanup | Temporary records removed |
| PostgreSQL server / Docker | Not executed; configuration and driver included |
| Browser visual / manual interaction QA | Not performed; component interaction tests used |
| Public/private internet hosting | Not deployed; required Java runtime is unsupported by Sites hosting |

The persistence test creates a temporary file-backed H2 database, starts an application context, saves an extra vehicle, closes the context, starts a second context on the same database, and verifies the vehicle remains, the seed vehicle count is not duplicated, and exactly two versioned migrations are recorded.

The seven API tests use Spring's HTTP test framework against real H2 SQL and Flyway migrations. They exercise all four CRUD resources, input validation, uniqueness, reference protection, assignment overlap, availability, license expiry, health, missing resources and malformed JSON. The five frontend tests render the actual React components in JSDOM with mocked API responses and exercise search, create, server validation errors, confirmed deletion and connection errors.

The separately executed live smoke test uses the real packaged server over HTTP, verifies the page and asset URLs, performs CRUD for all resources, and confirms overlap rejection. It removes only records it creates. To run it again while the app is running:

```sh
node scripts/smoke-test.mjs
```

The application was left running at **http://localhost:8080** at delivery. If the process has since stopped, use `start.cmd` or `start.ps1` from the project root. H2 data lives at `backend/data/fleet.mv.db` and is excluded from the downloadable archive; the archive initializes a fresh fictional demo fleet on its first launch.

## MySQL migration (2026-09-08)
MySQL 8.0 connection and Flyway schema creation passed. Imported 6 vehicles, 4 drivers, 6 trips and 4 maintenance rows, preserving IDs and verifying every field. All 8 backend tests passed. Live MySQL CRUD for all four resources and overlap rejection passed, with temporary rows removed. Packaged app restarted successfully using the mysql profile on port 8080.

