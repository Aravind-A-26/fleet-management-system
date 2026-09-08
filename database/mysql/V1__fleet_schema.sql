CREATE TABLE vehicles (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 plate VARCHAR(40) NOT NULL UNIQUE,
 model VARCHAR(120) NOT NULL,
 type VARCHAR(30) NOT NULL CHECK (type IN ('Truck','Van','Car')),
 status VARCHAR(30) NOT NULL CHECK (status IN ('Available','In service','Inactive')),
 odometer NUMERIC(12,1) NOT NULL CHECK (odometer >= 0),
 fuel NUMERIC(5,1) NOT NULL CHECK (fuel >= 0 AND fuel <= 100)
);
CREATE TABLE drivers (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(120) NOT NULL,
 phone VARCHAR(40) NOT NULL,
 license VARCHAR(60) NOT NULL UNIQUE,
 license_expiry DATE NOT NULL,
 status VARCHAR(30) NOT NULL CHECK (status IN ('Available','Off duty','Inactive'))
);
CREATE TABLE trips (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 vehicle_id BIGINT NOT NULL REFERENCES vehicles(id),
 driver_id BIGINT NOT NULL REFERENCES drivers(id),
 origin VARCHAR(120) NOT NULL,
 destination VARCHAR(120) NOT NULL,
 start_date DATE NOT NULL,
 end_date DATE NOT NULL,
 distance NUMERIC(12,1) NOT NULL CHECK (distance > 0),
 status VARCHAR(30) NOT NULL CHECK (status IN ('Scheduled','In progress','Completed','Cancelled')),
 CHECK (end_date >= start_date)
);
CREATE TABLE maintenance (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 vehicle_id BIGINT NOT NULL REFERENCES vehicles(id),
 service VARCHAR(160) NOT NULL,
 due_date DATE NOT NULL,
 cost NUMERIC(12,2) NOT NULL CHECK (cost >= 0),
 status VARCHAR(30) NOT NULL CHECK (status IN ('Scheduled','In progress','Completed')),
 notes VARCHAR(1000) NOT NULL DEFAULT ''
);
CREATE INDEX trips_vehicle_dates ON trips(vehicle_id,start_date,end_date);
CREATE INDEX trips_driver_dates ON trips(driver_id,start_date,end_date);
CREATE INDEX maintenance_due ON maintenance(due_date,status);

