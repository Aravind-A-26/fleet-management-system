INSERT INTO vehicles(plate,model,type,status,odometer,fuel) VALUES
 ('MH 12 AB 2401','Tata Prima 5530','Truck','Available',42810,78),
 ('MH 12 CD 8032','Ashok Leyland Ecomet','Truck','Available',68420,56),
 ('KA 01 MN 4520','Tata Ace Gold','Van','Available',21640,92),
 ('MH 14 EF 1198','Mahindra Bolero Pickup','Van','In service',87320,34),
 ('KA 05 PQ 6621','Tata Ultra T.16','Truck','Available',35210,68),
 ('MH 12 GH 9033','Maruti Suzuki Ertiga','Car','Available',19850,85);
INSERT INTO drivers(name,phone,license,license_expiry,status) VALUES
 ('Arjun Mehta','+91 90000 10001','MH1220200043210',DATE '2028-05-12','Available'),
 ('Priya Nair','+91 90000 10002','KA0120210067321',DATE '2029-03-20','Available'),
 ('Rohan Deshmukh','+91 90000 10003','MH1420190023415',DATE '2027-11-18','Available'),
 ('Sneha Patil','+91 90000 10004','MH1220220089542',DATE '2028-07-09','Off duty');
INSERT INTO trips(vehicle_id,driver_id,origin,destination,start_date,end_date,distance,status) VALUES
 (1,1,'Pune','Mumbai',CURRENT_DATE,CURRENT_DATE,148,'In progress'),
 (2,2,'Bengaluru','Chennai',CURRENT_DATE,CURRENT_DATE + 1,347,'Scheduled'),
 (3,3,'Pune','Nashik',CURRENT_DATE + 2,CURRENT_DATE + 2,212,'Scheduled'),
 (5,1,'Mumbai','Surat',CURRENT_DATE - 3,CURRENT_DATE - 2,284,'Completed'),
 (1,2,'Pune','Hyderabad',CURRENT_DATE - 6,CURRENT_DATE - 5,560,'Completed');
INSERT INTO maintenance(vehicle_id,service,due_date,cost,status,notes) VALUES
 (4,'Brake inspection & service',CURRENT_DATE - 1,8500,'In progress','Inspect brake pads and replace worn components.'),
 (2,'Engine oil & filter change',CURRENT_DATE + 4,4200,'Scheduled','Use manufacturer recommended oil.'),
 (6,'Tyre rotation',CURRENT_DATE + 8,1200,'Scheduled','Inspect tread depth on all four tyres.'),
 (1,'Annual inspection',CURRENT_DATE - 12,6500,'Completed','Inspection completed; no defects reported.');
