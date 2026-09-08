import React from 'react';
import {afterEach,beforeEach,describe,it,expect,vi} from 'vitest';
import {render,screen,waitFor,cleanup,within,fireEvent} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from '../app/page';

const fixtures={vehicles:[{id:1,plate:'TEST-101',model:'Tata Test',type:'Truck',status:'Available',odometer:1234,fuel:75}],drivers:[{id:1,name:'Test Driver',phone:'12345',license:'LIC-101',license_expiry:'2031-01-01',status:'Available'}],trips:[],maintenance:[]};
let saved:Record<string,unknown>;
beforeEach(()=>{
 saved={};
 vi.stubGlobal('fetch',vi.fn(async(url:string,options:RequestInit)=>{
  if(options?.method==='POST'){saved=JSON.parse(String(options.body));return new Response(JSON.stringify({id:2,...saved}),{status:201});}
  if(options?.method==='DELETE')return new Response(null,{status:204});
  return new Response(JSON.stringify(fixtures[url.split('/')[2] as keyof typeof fixtures]??[]),{status:200});
 }));
});
afterEach(()=>{cleanup();vi.unstubAllGlobals();});
describe('Fleet workflows',()=>{
 it('loads saved data and filters the vehicle table',async()=>{
  const user=userEvent.setup();render(<App/>);
  await screen.findByText('1 available for assignment');
  await user.click(screen.getByRole('button',{name:/^Vehicles/}));
  expect(await screen.findByText('TEST-101')).toBeTruthy();
  await user.type(screen.getByRole('textbox',{name:'Search Vehicles'}),'no-match');
  expect(screen.getByText('No matching records')).toBeTruthy();
  await user.clear(screen.getByRole('textbox',{name:'Search Vehicles'}));
  expect(screen.getByText('TEST-101')).toBeTruthy();
 });
 it('creates a vehicle through the form',async()=>{
  const user=userEvent.setup();render(<App/>);await screen.findByText('1 available for assignment');
  await user.click(screen.getByRole('button',{name:/^Vehicles/}));
  await user.click(screen.getByRole('button',{name:'Add vehicle'}));
  const dialog=await screen.findByRole('dialog');
  await user.type(within(dialog).getByLabelText('Registration *'),'NEW-202');
  await user.type(within(dialog).getByLabelText('Make & model *'),'Test Van');
  await user.click(within(dialog).getByRole('button',{name:'Save vehicle'}));
  await screen.findByText('Record saved');
  expect(saved.plate).toBe('NEW-202');expect(saved.type).toBe('Truck');expect(saved.status).toBe('Available');
 });
 it('retains form input and shows backend validation errors',async()=>{
  const user=userEvent.setup();render(<App/>);await screen.findByText('1 available for assignment');
  await user.click(screen.getByRole('button',{name:/^Vehicles/}));
  await user.click(screen.getByRole('button',{name:'Edit vehicle 1'}));
  vi.mocked(fetch).mockResolvedValueOnce(new Response(JSON.stringify({message:'Registration must be unique'}),{status:409}));
  await user.click(screen.getByRole('button',{name:'Save vehicle'}));
  expect(await screen.findByRole('alert')).toHaveProperty('textContent','Registration must be unique');
  expect(screen.getByLabelText('Registration *')).toHaveProperty('value','TEST-101');
 });
 it('requires confirmation before deleting',async()=>{
  const user=userEvent.setup();render(<App/>);await screen.findByText('1 available for assignment');
  await user.click(screen.getByRole('button',{name:/^Vehicles/}));
  await user.click(screen.getByRole('button',{name:'Delete vehicle 1'}));
  expect(await screen.findByRole('alertdialog')).toBeTruthy();
  expect(vi.mocked(fetch).mock.calls.some(c=>c[1]?.method==='DELETE')).toBe(false);
  await user.click(screen.getByRole('button',{name:'Delete record'}));
  await screen.findByText('Record deleted');
  expect(vi.mocked(fetch).mock.calls.some(c=>c[0]==='/api/vehicles/1'&&c[1]?.method==='DELETE')).toBe(true);
 });
 it('offers retry when the API is unavailable',async()=>{
  vi.mocked(fetch).mockRejectedValue(new Error('offline'));render(<App/>);
  expect((await screen.findByRole('alert')).textContent).toContain('Cannot reach the fleet server');
  expect(screen.getByRole('button',{name:'Retry'})).toBeTruthy();
 });
});
