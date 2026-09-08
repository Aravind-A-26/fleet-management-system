import assert from 'node:assert/strict';
const base=process.argv[2]||'http://127.0.0.1:8080';
const created=[];
async function call(path,method='GET',body,status=200){
 const response=await fetch(base+'/api/'+path,{method,headers:{'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(10000)});
 assert.equal(response.status,status,`${method} ${path}: ${await (response.status===status?Promise.resolve(''):response.text())}`);
 return status===204?null:response.json();
}
async function add(entity,body){const record=await call(entity,'POST',body,201);created.unshift([entity,record.id]);return record;}
const unique=Date.now().toString();
try{
 const page=await fetch(base);assert.equal(page.status,200);const html=await page.text();assert.match(html,/Fleetline/);
 for(const asset of [...html.matchAll(/(?:src|href)="(\/assets\/[^\"]+)"/g)])assert.equal((await fetch(base+asset[1])).status,200);
 assert.equal((await call('health')).database,'UP');
 const v=await add('vehicles',{plate:'QA-'+unique,model:'Smoke test van',type:'Van',status:'Available',odometer:100,fuel:60});
 const d=await add('drivers',{name:'Smoke test driver',phone:'9000000000',license:'QA-'+unique,license_expiry:'2035-12-31',status:'Available'});
 const t=await add('trips',{vehicle_id:v.id,driver_id:d.id,origin:'Pune',destination:'Mumbai',start_date:'2030-06-01',end_date:'2030-06-02',distance:148,status:'Scheduled'});
 await call('trips','POST',t,409);
 const m=await add('maintenance',{vehicle_id:v.id,service:'Inspection',due_date:'2030-06-04',cost:1200,status:'Scheduled',notes:'Temporary smoke test'});
 for(const [entity,record,change] of [['vehicles',v,{fuel:80}],['drivers',d,{phone:'9111111111'}],['trips',t,{status:'Completed'}],['maintenance',m,{status:'Completed'}]]){
  const updated=await call(entity+'/'+record.id,'PUT',{...record,...change});
  for(const [key,value]of Object.entries(change))assert.equal(updated[key],value);
  const reread=await call(entity+'/'+record.id);assert.equal(reread.id,record.id);
 }
 console.log('PASS: page/assets, SQL health, live CRUD for all four resources, and overlap rejection.');
}finally{
 for(const [entity,id]of created)await call(entity+'/'+id,'DELETE',undefined,204);
 console.log('Temporary test records removed.');
}
