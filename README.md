# Fleet Management System

This is a web application for managing vehicles, drivers, trips, and vehicle maintenance.

## Technologies Used

- Frontend: React, TypeScript, and CSS
- Backend: Java and Spring Boot
- Database: MySQL
- Database tool: MySQL Workbench

H2 is also included so the project can run without setting up MySQL first.

## Features

- Add, view, update, and delete vehicles.
- Maintain driver details and license expiry dates.
- Assign a vehicle and driver to a trip.
- Prevent overlapping trips for the same vehicle or driver.
- Track maintenance dates, costs, and status.
- Search records and view a dashboard summary.

## Project Folders

| Folder | Contents |
| --- | --- |
| frontend | React pages, forms, styles, and frontend tests |
| backend | Java API, validation, and backend tests |
| database | SQL table definitions and sample data |
| scripts | Setup helper and application check scripts |

## How It Works

The user enters details in the React frontend. The Spring Boot backend checks the details and saves them in the database. MySQL Workbench can display the same data stored in MySQL.

## Requirements

- JDK 17 or newer
- Node.js 22.13 or newer and npm
- Maven 3.9 or newer
- MySQL Server and MySQL Workbench, if using MySQL

## Run the Project

Build the frontend first:

```sh
cd frontend
npm ci --ignore-scripts
npm run build
```

Build and start the backend:

```sh
cd ../backend
mvn package
java -jar target/fleet-api-1.0.0.jar
```

Open http://localhost:8080 in a browser.

This first run uses H2 and creates sample records. The data is saved in the backend/data folder. Build files are not included in this repository; the commands above create them.

On Windows, setup.ps1 can install the build dependencies and build the project. After building, start.cmd or start.ps1 starts the application.

## Use MySQL

Create a database named fleet_management and a local database user in MySQL. Give that user access to this database. In the main project folder, create a file named .env.mysql:

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/fleet_management?serverTimezone=Asia/Kolkata
DB_USER=your_mysql_username
DB_PASSWORD=your_mysql_password
SPRING_PROFILES_ACTIVE=mysql
```

Start the project from PowerShell:

```powershell
.\start.ps1 -MySql
```

The backend creates the tables automatically. A new MySQL database starts empty; H2 records are not copied automatically.

In MySQL Workbench, connect to your local server and refresh Schemas. Open fleet_management, then Tables. You can also run database/view-fleet.sql to view the vehicles, drivers, trips, and maintenance records.

Passwords and local database files are excluded from GitHub.

## Testing

Frontend tests:

```sh
cd frontend
npm test
```

Backend tests:

```sh
cd backend
mvn test
```

The project has 5 frontend tests and 8 backend tests. The MySQL version was also checked for creating, updating, deleting, and saving records after restarting the application.

## Current Scope

The application runs locally. It does not include user login, live GPS tracking, or online deployment.
