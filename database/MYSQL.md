# MySQL Workbench and Fleetline

The local app now uses MySQL Server at 127.0.0.1:3306, database `fleet_management`. MySQL Workbench is the desktop interface for viewing those same records.

Migration verified on 2026-09-08: 6 vehicles, 4 drivers, 6 trips, and 4 maintenance records. Original IDs and every field were checked. The original H2 database remains in backend/data as a pre-migration backup; it does not receive new changes.

Open the existing `localhost` connection in Workbench. Refresh Schemas, expand `fleet_management` then Tables. Right-click a table and choose Select Rows, or open `database/view-fleet.sql` and execute it to see all four result grids.

Both start.cmd and start.ps1 select MySQL when `.mysql-ready` exists. The private `.env.mysql` file contains the dedicated local login; its permissions apply only to this database. Keep this file private. The ZIP excludes credentials, database files and the machine-specific marker, so a fresh download defaults to H2.

Explicit launch modes:

```powershell
.\start.ps1 -MySql
.\start.ps1 -H2
```

The H2 mode opens the old backup data; it is a separate database. MySQL tables are created by the MySQL-specific Flyway migration in database/mysql. A new MySQL installation requires an administrator to create the database and a dedicated login, then configure .env.mysql; it starts empty.
