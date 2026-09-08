package com.fleet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api")
public class FleetController {
    private final JdbcTemplate db;
    private static final Map<String,List<String>> FIELDS = Map.of(
        "vehicles",List.of("plate","model","type","status","odometer","fuel"),
        "drivers",List.of("name","phone","license","license_expiry","status"),
        "trips",List.of("vehicle_id","driver_id","origin","destination","start_date","end_date","distance","status"),
        "maintenance",List.of("vehicle_id","service","due_date","cost","status","notes"));
    private static final Map<String,List<String>> STATUSES = Map.of(
        "vehicles",List.of("Available","In service","Inactive"),
        "drivers",List.of("Available","Off duty","Inactive"),
        "trips",List.of("Scheduled","In progress","Completed","Cancelled"),
        "maintenance",List.of("Scheduled","In progress","Completed"));
    public FleetController(JdbcTemplate db) { this.db=db; }
    private List<String> fields(String entity) {
        if (!FIELDS.containsKey(entity)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Unknown resource");
        return FIELDS.get(entity);
    }
    @GetMapping("/health") public Map<String,Object> health() {
        return Map.of("status", "UP", "database", db.queryForObject("SELECT 1",Integer.class)==1 ? "UP":"DOWN");
    }
    @GetMapping("/{entity}") public List<Map<String,Object>> list(@PathVariable String entity) {
        fields(entity); return db.queryForList("SELECT * FROM "+entity+" ORDER BY id DESC");
    }
    @GetMapping("/{entity}/{id}") public Map<String,Object> get(@PathVariable String entity,@PathVariable long id) {
        fields(entity);
        var result=db.queryForList("SELECT * FROM "+entity+" WHERE id=?",id);
        if(result.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found");
        return result.get(0);
    }
    @PostMapping("/{entity}") @ResponseStatus(HttpStatus.CREATED) @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Map<String,Object> create(@PathVariable String entity,@RequestBody Map<String,Object> body) {
        var keys=fields(entity); var values=validate(entity,body,0);
        var holder=new org.springframework.jdbc.support.GeneratedKeyHolder();
        db.update(connection->{
            var statement=connection.prepareStatement("INSERT INTO "+entity+" ("+String.join(",",keys)+") VALUES ("+String.join(",",Collections.nCopies(keys.size(),"?"))+")",new String[]{"id"});
            for(int i=0;i<values.size();i++) statement.setObject(i+1,values.get(i));
            return statement;
        },holder);
        return get(entity,Objects.requireNonNull(holder.getKey()).longValue());
    }
    @PutMapping("/{entity}/{id}") @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Map<String,Object> update(@PathVariable String entity,@PathVariable long id,@RequestBody Map<String,Object> body) {
        get(entity,id);
        var keys=fields(entity); var values=validate(entity,body,id); values.add(id);
        db.update("UPDATE "+entity+" SET "+String.join(",",keys.stream().map(k->k+"=?").toList())+" WHERE id=?",values.toArray());
        return get(entity,id);
    }
    @DeleteMapping("/{entity}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void delete(@PathVariable String entity,@PathVariable long id) {
        fields(entity);
        if(db.update("DELETE FROM "+entity+" WHERE id=?",id)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found");
    }
    private ArrayList<Object> validate(String entity,Map<String,Object> input,long id) {
        var result=new ArrayList<Object>(); var clean=new HashMap<String,Object>();
        for(String key:fields(entity)) {
            String value=Objects.toString(input.get(key),"").trim();
            if(value.isEmpty() && !key.equals("notes")) fail(label(key)+" is required");
            Object parsed=value;
            if(key.endsWith("_id")) {
                try { parsed=Long.parseLong(value); } catch(Exception e) { fail(label(key)+" must be a valid selection"); }
                if((Long)parsed<=0) fail(label(key)+" must be a valid selection");
            } else if(key.endsWith("_date") || key.equals("license_expiry")) {
                try { parsed=LocalDate.parse(value); } catch(Exception e) { fail(label(key)+" must be a valid date"); }
            } else if(List.of("odometer","fuel","distance","cost").contains(key)) {
                try { parsed=new BigDecimal(value); } catch(Exception e) { fail(label(key)+" must be a number"); }
                BigDecimal number=(BigDecimal)parsed;
                if(number.signum()<0 || (key.equals("distance") && number.signum()==0)) fail(label(key)+" must be "+(key.equals("distance")?"greater than zero":"zero or more"));
                if(number.compareTo(new BigDecimal(key.equals("fuel")?"100":"999999999"))>0) fail(label(key)+" is too large");
                if(number.scale()>(key.equals("cost")?2:1)) fail(label(key)+" has too many decimal places");
            } else {
                int max=switch(key) {case "plate","phone"->40;case "license"->60;case "type","status"->30;case "service"->160;case "notes"->1000;default->120;};
                if(value.length()>max) fail(label(key)+" must be at most "+max+" characters");
                if(key.equals("plate") || key.equals("license")) parsed=value.toUpperCase(Locale.ROOT);
            }
            clean.put(key,parsed); result.add(parsed);
        }
        if(!STATUSES.get(entity).contains(clean.get("status"))) fail("Choose a valid status");
        if(entity.equals("vehicles") && !List.of("Truck","Van","Car").contains(clean.get("type"))) fail("Choose a valid vehicle type");
        if(entity.equals("trips")) {
            LocalDate start=(LocalDate)clean.get("start_date"), end=(LocalDate)clean.get("end_date");
            if(end.isBefore(start)) fail("End date cannot be before start date");
            var vehicle=lock("vehicles",(long)clean.get("vehicle_id"));
            var driver=lock("drivers",(long)clean.get("driver_id"));
            if(List.of("Scheduled","In progress").contains(clean.get("status"))) {
                if(!vehicle.get("status").equals("Available")) fail("This vehicle is not available for assignment");
                if(!driver.get("status").equals("Available")) fail("This driver is not available for assignment");
                if(LocalDate.parse(driver.get("license_expiry").toString()).isBefore(end)) fail("Driver's license expires before the trip ends");
                long overlap=db.queryForObject("SELECT COUNT(*) FROM trips WHERE id<>? AND status IN ('Scheduled','In progress') AND (vehicle_id=? OR driver_id=?) AND start_date<=? AND end_date>=?",Long.class,id,clean.get("vehicle_id"),clean.get("driver_id"),end,start);
                if(overlap>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Vehicle or driver already has a trip on these dates");
            }
        } else if(entity.equals("maintenance")) { get("vehicles",(long)clean.get("vehicle_id")); }
        else if(id>0 && (entity.equals("vehicles") || entity.equals("drivers"))) {
            lock(entity,id);
            String column=entity.equals("vehicles")?"vehicle_id":"driver_id";
            if(!clean.get("status").equals("Available") && db.queryForObject("SELECT COUNT(*) FROM trips WHERE "+column+"=? AND status IN ('Scheduled','In progress')",Long.class,id)>0)
                fail("Finish or cancel assigned trips before changing availability");
            if(entity.equals("drivers") && db.queryForObject("SELECT COUNT(*) FROM trips WHERE driver_id=? AND status IN ('Scheduled','In progress') AND end_date>?",Long.class,id,clean.get("license_expiry"))>0)
                fail("License expiry must cover all assigned trips");
        }
        return result;
    }
    private Map<String,Object> lock(String entity,long id) {
        var rows=db.queryForList("SELECT * FROM "+entity+" WHERE id=? FOR UPDATE",id);
        if(rows.isEmpty()) fail("Selected "+(entity.equals("vehicles")?"vehicle":"driver")+" no longer exists");
        return rows.get(0);
    }
    private String label(String key) { return key.replace('_',' '); }
    private void fail(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String,String>> statusError(ResponseStatusException ex) { return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",Objects.toString(ex.getReason(),"Request failed"))); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String,String>> integrityError(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(Map.of("message","Cannot save or delete: registration/license must be unique, and vehicles or drivers referenced by trips or service records cannot be deleted."));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<Map<String,String>> jsonError(Exception ex) { return ResponseEntity.badRequest().body(Map.of("message","Provide a valid JSON record")); }
}
