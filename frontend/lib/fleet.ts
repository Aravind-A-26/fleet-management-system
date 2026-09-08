export type Entity = 'vehicles' | 'drivers' | 'trips' | 'maintenance';
export type Row = Record<string, string | number> & {id: number};
export type Field = {key: string; label: string; type?: string; options?: string[]; source?: Entity; optional?: boolean; max?: number};
export const statuses: Record<Entity,string[]> = {
  vehicles: ['Available','In service','Inactive'], drivers: ['Available','Off duty','Inactive'],
  trips: ['Scheduled','In progress','Completed','Cancelled'], maintenance: ['Scheduled','In progress','Completed'],
};
export const fields: Record<Entity,Field[]> = {
  vehicles: [{key:'plate',label:'Registration',max:40},{key:'model',label:'Make & model',max:120},{key:'type',label:'Vehicle type',options:['Truck','Van','Car']},{key:'status',label:'Availability',options:statuses.vehicles},{key:'odometer',label:'Odometer (km)',type:'number'},{key:'fuel',label:'Fuel (%)',type:'number',max:100}],
  drivers: [{key:'name',label:'Full name',max:120},{key:'phone',label:'Phone number',type:'tel',max:40},{key:'license',label:'License number',max:60},{key:'license_expiry',label:'License expiry',type:'date'},{key:'status',label:'Availability',options:statuses.drivers}],
  trips: [{key:'vehicle_id',label:'Vehicle',source:'vehicles'},{key:'driver_id',label:'Driver',source:'drivers'},{key:'origin',label:'Origin',max:120},{key:'destination',label:'Destination',max:120},{key:'start_date',label:'Start date',type:'date'},{key:'end_date',label:'End date',type:'date'},{key:'distance',label:'Distance (km)',type:'number'},{key:'status',label:'Trip status',options:statuses.trips}],
  maintenance: [{key:'vehicle_id',label:'Vehicle',source:'vehicles'},{key:'service',label:'Service',max:160},{key:'due_date',label:'Due date',type:'date'},{key:'cost',label:'Estimated / actual cost (₹)',type:'number'},{key:'status',label:'Service status',options:statuses.maintenance},{key:'notes',label:'Notes',type:'textarea',optional:true,max:1000}],
};
export const titles: Record<Entity,string> = {vehicles:'Vehicles',drivers:'Drivers',trips:'Trips',maintenance:'Maintenance'};
export const singular: Record<Entity,string> = {vehicles:'vehicle',drivers:'driver',trips:'trip',maintenance:'service'};
export const today = () => { const d=new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
export const number = (v: unknown) => Number(v || 0).toLocaleString('en-IN');
export const date = (v: unknown) => v ? new Date(String(v)+'T12:00:00').toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'}) : '—';
export async function request(path: string, method='GET', body?: unknown) {
  let response: Response;
  try { response=await fetch('/api/'+path,{method,headers:{'Content-Type':'application/json'},body:body ? JSON.stringify(body):undefined,signal:AbortSignal.timeout(15000)}); }
  catch { throw new Error('Cannot reach the fleet server. Check that the Java backend is running and retry.'); }
  if(!response.ok) { const error=await response.json().catch(()=>({})) as {message?:string}; throw new Error(error.message || `Request failed (${response.status}). Please retry.`); }
  return response.status===204 ? null : response.json();
}
