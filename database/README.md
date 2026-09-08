# Database

`migrations/` is the single source of truth. Maven packages these files into the backend's `db/migration/` classpath; there is no second editable copy. Flyway applies them automatically at startup and records versions in `flyway_schema_history`.

- `V1__fleet_schema.sql`: vehicles, drivers, trips, maintenance; primary/foreign keys, unique registration/license numbers, indexes and checks.
- `V2__demo_data.sql`: clearly fictional demo fleet with dates relative to the first database initialization. Runs once, never on every restart. It creates six vehicles, four drivers, five trips and four services.

Default local database: `backend/data/fleet.mv.db` (H2 in PostgreSQL compatibility mode). It survives restarts. Stop the application before backing up that file. For PostgreSQL, use `pg_dump` and your normal database backup procedure.

PostgreSQL setup is documented in the root README. Do not run these migrations manually on a database already managed by Flyway. Never edit a migration that has been applied; add a new version instead. To start without demo data on a new installation, exclude V2 before the first build/start. Do not remove V2 from an already migrated installation.

Trip assignment overlaps use inclusive whole-day dates. Scheduled and in-progress trips reserve both driver and vehicle. Completed and cancelled trips do not reserve resources. Record locks serialize assignment checks for the same resource. Vehicle availability is separate from trip progress: a vehicle can be marked Available while assigned; the trip calendar determines whether it can accept another trip. Maintenance records do not automatically change vehicle availability. Mark a vehicle In service explicitly after resolving active assignments.
