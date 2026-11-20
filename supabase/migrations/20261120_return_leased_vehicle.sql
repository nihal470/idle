-- Travz / Idle Fleet: safe vehicle lifecycle migration
-- Run only after exporting a backup of the existing Supabase database.
-- This migration adds lifecycle metadata; it does not delete vehicles,
-- modify bookings, alter foreign keys, or change RLS/auth policies.

alter table public.vehicles
  add column if not exists fleet_status text not null default 'ACTIVE',
  add column if not exists fleet_return_date date,
  add column if not exists fleet_return_reason text,
  add column if not exists fleet_return_remarks text;

alter table public.vehicles
  drop constraint if exists vehicles_fleet_status_check;

alter table public.vehicles
  add constraint vehicles_fleet_status_check
  check (fleet_status in ('ACTIVE', 'RETURNED'));

create index if not exists vehicles_fleet_status_idx
  on public.vehicles (fleet_status);

comment on column public.vehicles.fleet_status is
  'Fleet lifecycle: ACTIVE or RETURNED. Separate from operational status.';
comment on column public.vehicles.fleet_return_date is
  'Date the vehicle was returned to its owner or leasing company.';
comment on column public.vehicles.fleet_return_reason is
  'Reason for returning a leased vehicle.';
comment on column public.vehicles.fleet_return_remarks is
  'Optional notes recorded at the time of return.';
