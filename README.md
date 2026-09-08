# Fleetline â€” Fleet Management System

A runnable React + Java Spring Boot + SQL MVP for managing vehicles, drivers, trips and maintenance. The production React app is packaged inside the Java server so the complete system runs at **http://localhost:8080**.

## Database on this computer

The local app now uses MySQL and is viewable in MySQL Workbench. See [MySQL instructions](database/MYSQL.md). The portable ZIP defaults to H2 because local credentials are excluded.

## Project structure

```text
fleet-management-system/
â”œâ”€â”€ frontend/                  React + TypeScript + Vite
â”‚   â”œâ”€â”€ app/                   Dashboard, management screens, shared styles
â”‚   â”œâ”€â”€ components/ui/         Accessible Shadcn / Base UI components
â”‚   â”œâ”€â”€ lib/fleet.ts           Fields, statuses and API client
â”‚   â”œâ”€â”€ tests/                 React interaction tests
â”‚   â””â”€â”€ dist/                  Built frontend
â”œâ”€â”€ backend/                   Java 17 / Spring Boot / JDBC / Flyway
â”‚   â”œâ”€â”€ src/main/              REST API, validation, configuration
â”‚   â”œâ”€â”€ src/test/              API and file persistence tests
â”‚   â”œâ”€â”€ data/                  Local persistent H2 database (generated, ignored)
â”‚   â””â”€â”€ target/fleet-api-1.0.0.jar  Ready-to-run app, including React
â”œâ”€â”€ database/
â”‚   â”œâ”€â”€ migrations/            Canonical SQL schema and demo seed migrations
â”‚   â””â”€â”€ README.md              Migration and data behavior
â”œâ”€â”€ scripts/download-maven.mjs Checksum-verified Maven downloader
â”œâ”€â”€ .env.example               Optional PostgreSQL settings; no real secrets
â”œâ”€â”€ compose.yaml               Optional local PostgreSQL 17 container
â”œâ”€â”€ setup.ps1                  Download Maven, install, test and build
â”œâ”€â”€ build.ps1                  Rebuild frontend and backend with tests
â”œâ”€â”€ start.ps1                  Run the packaged system; optional -Postgres
â”œâ”€â”€ start.cmd                  Double-click launcher on Windows
â””â”€â”€ README.md
```

## Run the included build

Requires Java 17 or newer. No Node, Maven, Docker or separately installed database is needed to **run the included JAR**.

On Windows, double-click `start.cmd`, or run from this folder:

```powershell
.\start.ps1
```

On any platform:

```sh
cd backend
java -jar target/fleet-api-1.0.0.jar
```

Open **http://localhost:8080**. Stop with Ctrl+C. If a copy is already running on port 8080, use that copy or stop it before starting another one. The server binds to the local computer only. Keep the working directory as `backend` so the local database remains at `backend/data/fleet.mv.db`.

The default SQL database is **file-backed H2 in PostgreSQL compatibility mode**. Changes survive restarts. The first launch applies the schema and fictional demo data; later launches preserve existing data without reseeding. Do not delete `backend/data` unless you intend to discard all local records. Environment variables such as `DB_URL`, if already set in your shell, override these defaults.

## Database console

Open **http://localhost:8080/database/** to browse the local H2 tables and run SQL.
Choose **Generic H2 (Embedded)** and enter:

```text
Driver Class: org.h2.Driver
JDBC URL: jdbc:h2:file:./data/fleet;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
User Name: sa
Password: (leave empty)
```

Click **Connect**, then expand the tables on the left. For example:

```sql
SELECT * FROM vehicles;
SELECT * FROM drivers;
SELECT * FROM trips;
SELECT * FROM maintenance;
```

SQL edits change the same database used by the app. The console accepts connections from this computer only. Set `H2_CONSOLE_ENABLED=false` to disable it. This console is for the default H2 database; use a PostgreSQL client when running the PostgreSQL configuration.

## Features

- Overview with saved-data metrics, dispatch board, maintenance queue, completed distance and vehicle fuel readings.
- Add, view, edit and delete vehicles, drivers, trips and maintenance records.
- Search and status filtering, responsive navigation, keyboard-accessible forms and delete confirmations.
- Required fields, valid dates/statuses, number limits, unique registration/license numbers and SQL foreign keys.
- Inclusive date overlap prevention for scheduled/in-progress trips sharing a vehicle or driver, with database row locks for assignment checks.
- Driver license expiry and availability checks. Assigned drivers/vehicles cannot be made unavailable until active trips are completed or cancelled.
- Clear server validation errors, retry on failed loads, loading and empty states.

Distances and odometer readings use km. Fuel is a manually maintained percentage; maintenance costs use INR. Completing a trip contributes its distance to the dashboard but does not automatically alter the vehicle odometer. Maintenance status and vehicle availability are edited separately. This is a trusted-local MVP with no login, GPS/telematics integration or multi-user authorization; add those controls before any shared production deployment.

## Build prerequisites and setup

- Node.js **22.13+** and npm.
- **JDK 17+**; set `JAVA_HOME` to the JDK directory.
- **Maven 3.9+**, or let `setup.ps1` download Maven 3.9.9 into `.tools` with SHA-512 verification.
- Network access for the first dependency install.

On Windows:

```powershell
.\setup.ps1
.\start.ps1
```

If PowerShell script execution is restricted by your organization's policy, use the manual commands below or have your administrator approve these local scripts. No execution policy change is required for `start.cmd`.

Manual cross-platform build, starting in the project root:

```sh
cd frontend
npm ci --ignore-scripts
npm run build
npm test
cd ../backend
mvn package
java -jar target/fleet-api-1.0.0.jar
```

The frontend must be built before `mvn package` so its latest `dist` assets are embedded in the JAR. `--ignore-scripts` avoids unnecessary starter dependency lifecycle scripts; the active Vite build uses installed native packages directly. Vite uses native configuration loading, and the test configuration uses in-process TypeScript compilation to support Windows environments that restrict child processes.

If npm's script launcher is restricted, equivalent direct commands are:

```sh
node node_modules/typescript/bin/tsc --noEmit
node node_modules/vitest/vitest.mjs run --configLoader native
node node_modules/vite/bin/vite.js build --configLoader native
```

## Development: separate frontend and backend

Terminal 1:

```sh
cd backend
mvn spring-boot:run
```

Terminal 2:

```sh
cd frontend
npm run dev
```

- React dev server: **http://localhost:3000**.
- Java backend: **http://localhost:8080**.
- Health check: **http://localhost:8080/api/health**.
- The Vite dev server proxies `/api` to the Java backend; production assets use the same origin. Set `API_PROXY_TARGET` in the frontend process environment if the backend port changes. `PORT` configures the Java port.

## PostgreSQL configuration

PostgreSQL is supported by the JDBC driver and Flyway migrations. The local demonstrated runtime uses H2 because PostgreSQL and Docker were not available in this environment.

1. Copy `.env.example` to `.env` and replace both password placeholders with the same strong local password. Keep `.env` out of source control.
2. With Docker installed, run `docker compose up -d postgres`. Alternatively create an empty `fleet` database and a `fleet` login on your PostgreSQL server.
3. Stop the H2 app and run `./start.ps1 -Postgres`. This launcher reads `.env` as literal key/value settings; Spring Boot does not automatically read that file when run directly.

For a direct launch, export these variables in the process environment:

```text
DB_URL=jdbc:postgresql://localhost:5432/fleet
DB_USER=fleet
DB_PASSWORD=<your password>
```

Then run the same JAR from `backend`. Flyway initializes the PostgreSQL database automatically. Switching database connections does **not** transfer records from H2; each database has independent data. The demo migration uses dates relative to its first application. See `database/README.md` for details and how to omit demo data before a new installation.

## API

Resources: `vehicles`, `drivers`, `trips`, `maintenance`.

| Method | Path | Behavior |
|---|---|---|
| GET | `/api/health` | Application and SQL connectivity |
| GET | `/api/{resource}` | List records |
| GET | `/api/{resource}/{id}` | Retrieve one record |
| POST | `/api/{resource}` | Create; returns 201 and saved record |
| PUT | `/api/{resource}/{id}` | Replace editable fields; returns saved record |
| DELETE | `/api/{resource}/{id}` | Delete; returns 204 |

JSON uses SQL column names, e.g. `vehicle_id`, `license_expiry`, `start_date`. Dates use `YYYY-MM-DD`. Validation failures return 400, missing records/resources return 404, and conflicting assignments/unique values/linked deletion return 409, with a readable `message`. See `frontend/lib/fleet.ts` for fields and allowed statuses. No endpoint accepts table names outside the four-resource whitelist.

## Verification

Verified on Windows with JDK 17 and Node 22:

- Production React build and TypeScript compilation passed.
- Five React interaction tests passed: loading/search, create form, retaining input after server validation failure, confirmed deletion, and API connection errors.
- Seven Spring Boot API integration tests passed with real SQL migrations: CRUD for all resources, uniqueness, invalid values/dates, overlap, unavailable/expired drivers, linked deletion protection, health and malformed JSON.
- Page and live API returned HTTP 200; seeded counts were six vehicles and five trips.

The restart-persistence test also passed, bringing the total to **13 automated tests**. Live HTTP CRUD and overlap-rejection checks passed, and their temporary records were removed. Full results are recorded in `VERIFICATION.md`. PostgreSQL/Docker execution, browser visual QA, and internet hosting were not tested. Tests use real H2 SQL on the backend and mocked API responses for React component interactions. Sites hosting runs JavaScript Workers and cannot host this requested Java process; the complete system is delivered locally instead of publishing an incomplete frontend.


