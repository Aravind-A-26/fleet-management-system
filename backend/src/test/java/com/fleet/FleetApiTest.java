package com.fleet;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:fleettest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@Transactional
class FleetApiTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 Map<String,Object> vehicle(){return new HashMap<>(Map.of("plate","TEST-901","model","Test Van","type","Van","status","Available","odometer",120,"fuel",70));}
 Map<String,Object> driver(){return new HashMap<>(Map.of("name","Test Driver","phone","+91 90000 99999","license","TEST-LICENSE","license_expiry","2031-01-01","status","Available"));}
 long create(String entity,Map<String,Object> body) throws Exception {
   String text=mvc.perform(post("/api/"+entity).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
   return json.readTree(text).get("id").asLong();
 }
 Map<String,Object> trip(long v,long d){return new HashMap<>(Map.of("vehicle_id",v,"driver_id",d,"origin","Pune","destination","Mumbai","start_date","2030-01-10","end_date","2030-01-11","distance",148,"status","Scheduled"));}
 @Test void crudAllResourcesAndForeignKeyProtection() throws Exception {
   long v=create("vehicles",vehicle()),d=create("drivers",driver());
   long t=create("trips",trip(v,d));
   var service=new HashMap<String,Object>(Map.of("vehicle_id",v,"service","Inspection","due_date","2030-02-01","cost",1250,"status","Scheduled","notes","Test record"));
   long m=create("maintenance",service);
   mvc.perform(delete("/api/vehicles/"+v)).andExpect(status().isConflict());
   mvc.perform(delete("/api/drivers/"+d)).andExpect(status().isConflict());
   var updated=vehicle();updated.put("fuel",85);
   mvc.perform(put("/api/vehicles/"+v).contentType("application/json").content(json.writeValueAsString(updated))).andExpect(status().isOk()).andExpect(jsonPath("$.fuel").value(85));
   var updatedDriver=driver();updatedDriver.put("phone","99999");
   mvc.perform(put("/api/drivers/"+d).contentType("application/json").content(json.writeValueAsString(updatedDriver))).andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("99999"));
   var updatedTrip=trip(v,d);updatedTrip.put("status","Completed");
   mvc.perform(put("/api/trips/"+t).contentType("application/json").content(json.writeValueAsString(updatedTrip))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Completed"));
   service.put("status","Completed");service.put("cost",1500);
   mvc.perform(put("/api/maintenance/"+m).contentType("application/json").content(json.writeValueAsString(service))).andExpect(status().isOk()).andExpect(jsonPath("$.cost").value(1500));
   for(var entry:Map.of("trips",t,"maintenance",m,"vehicles",v,"drivers",d).entrySet()) mvc.perform(get("/api/"+entry.getKey()+"/"+entry.getValue())).andExpect(status().isOk());
   mvc.perform(delete("/api/trips/"+t)).andExpect(status().isNoContent());
   mvc.perform(delete("/api/maintenance/"+m)).andExpect(status().isNoContent());
   mvc.perform(delete("/api/vehicles/"+v)).andExpect(status().isNoContent());
   mvc.perform(delete("/api/drivers/"+d)).andExpect(status().isNoContent());
   mvc.perform(get("/api/vehicles/"+v)).andExpect(status().isNotFound());
 }
 @Test void duplicateAndInvalidValuesGiveUsefulErrors() throws Exception {
   create("vehicles",vehicle());
   mvc.perform(post("/api/vehicles").contentType("application/json").content(json.writeValueAsString(vehicle()))).andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
   var bad=vehicle();bad.put("fuel",101);
   mvc.perform(post("/api/vehicles").contentType("application/json").content(json.writeValueAsString(bad))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("fuel is too large"));
   bad.put("fuel",50);bad.put("plate","");
   mvc.perform(post("/api/vehicles").contentType("application/json").content(json.writeValueAsString(bad))).andExpect(status().isBadRequest());
 }
 @Test void rejectsOverlappingAssignmentsButAllowsCompletedHistory() throws Exception {
   long v=create("vehicles",vehicle()),d=create("drivers",driver()); var t=trip(v,d);create("trips",t);
   mvc.perform(post("/api/trips").contentType("application/json").content(json.writeValueAsString(t))).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Vehicle or driver already has a trip on these dates"));
   t.put("status","Completed");create("trips",t);
   t.put("status","Scheduled");t.put("start_date","2030-01-12");t.put("end_date","2030-01-13");create("trips",t);
 }
 @Test void rejectsUnavailableDriverAndExpiredLicense() throws Exception {
   long v=create("vehicles",vehicle());var d=driver();d.put("status","Off duty");long id=create("drivers",d);
   mvc.perform(post("/api/trips").contentType("application/json").content(json.writeValueAsString(trip(v,id)))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("This driver is not available for assignment"));
   d.put("status","Available");d.put("license_expiry","2029-12-31");mvc.perform(put("/api/drivers/"+id).contentType("application/json").content(json.writeValueAsString(d))).andExpect(status().isOk());
   mvc.perform(post("/api/trips").contentType("application/json").content(json.writeValueAsString(trip(v,id)))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Driver's license expires before the trip ends"));
 }
 @Test void preventsAvailabilityChangeWithAssignedTrip() throws Exception {
   var v=vehicle();long id=create("vehicles",v),d=create("drivers",driver());create("trips",trip(id,d));v.put("status","In service");
   mvc.perform(put("/api/vehicles/"+id).contentType("application/json").content(json.writeValueAsString(v))).andExpect(status().isBadRequest());
 }
 @Test void rejectsInvalidDatesAndMissingReferences() throws Exception {
   var t=trip(99999,99999);t.put("end_date","2029-01-01");
   mvc.perform(post("/api/trips").contentType("application/json").content(json.writeValueAsString(t))).andExpect(status().isBadRequest());
   t.put("end_date","2030-01-12");mvc.perform(post("/api/trips").contentType("application/json").content(json.writeValueAsString(t))).andExpect(status().isBadRequest());
 }
 @Test void healthUnknownResourceAndMalformedBody() throws Exception {
   mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.database").value("UP"));
   mvc.perform(get("/api/not_a_table")).andExpect(status().isNotFound());
   mvc.perform(post("/api/vehicles").contentType("application/json").content("{bad")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Provide a valid JSON record"));
 }
}
