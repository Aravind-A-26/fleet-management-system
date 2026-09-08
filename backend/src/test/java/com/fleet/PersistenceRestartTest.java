package com.fleet;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceRestartTest {
 @TempDir Path directory;
 @Test void savedRecordSurvivesRestartWithoutReseeding() {
   String url="jdbc:h2:file:"+directory.resolve("fleet").toAbsolutePath().toString().replace('\\','/')+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
   String[] args={"--spring.datasource.url="+url,"--spring.main.banner-mode=off","--logging.level.root=WARN"};
   try(var first=new SpringApplicationBuilder(FleetApplication.class).web(WebApplicationType.NONE).run(args)) {
     JdbcTemplate db=first.getBean(JdbcTemplate.class);
     db.update("INSERT INTO vehicles(plate,model,type,status,odometer,fuel) VALUES ('PERSIST-001','Restart check','Van','Available',100,50)");
   }
   try(var second=new SpringApplicationBuilder(FleetApplication.class).web(WebApplicationType.NONE).run(args)) {
     JdbcTemplate db=second.getBean(JdbcTemplate.class);
     assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM vehicles WHERE plate='PERSIST-001'",Integer.class));
     assertEquals(7,db.queryForObject("SELECT COUNT(*) FROM vehicles",Integer.class));
     assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success=TRUE AND version IS NOT NULL",Integer.class));
   }
 }
}
