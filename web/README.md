# Idle Fleet Web

Responsive browser version of the Idle Fleet Android app. It uses the **existing** Supabase project and the same `public.vehicles` and `public.bookings` tables.

## Features
- Fleet overview with status counts, search and status filters
- Add, edit, delete and update vehicle status
- Reservation catalogue with category and availability filters
- Create reservations and track bookings
- Confirm pickup, check in returns and cancel bookings
- Supabase Realtime subscriptions with manual refresh fallback

## Supabase configuration
The app connects to the existing project URL:
`https://wxqsjmilzpxaiptkahmq.supabase.co`

On first load, paste the project's **publishable key** (legacy anon key is also accepted) when prompted. It is stored in this browser's local storage. Never enter a `service_role` or secret key.

No database tables or policies are created or modified by this web app. It expects the same columns used by the Android app:
- `vehicles`: id, make, model, year, license_plate, category, daily_rate, status, seats, transmission, fuel_type, mileage, current_renter, return_due, maintenance_note, image_url, updated_at
- `bookings`: id, booking_code, vehicle_id, vehicle_name, license_plate, customer_name, customer_phone, customer_email, pickup_date, return_date, days, total_price, status, notes, created_at

## Run locally
Open `index.html` through a local static server (for example VS Code Live Server). Do not open it as a `file://` URL because browser module imports may be blocked.

## Deploy on Vercel
1. Import `nihal470/idle` into Vercel.
2. Select branch `web-dashboard`.
3. Set the Root Directory to `web`.
4. Choose Other / Static site (no build command; output directory `.`).
5. Deploy. The browser asks for the Supabase publishable key on first visit.

You can also deploy the `web` folder using Netlify Drop or any static host.

## Important security note
This version intentionally has no staff sign-in, matching the current Android app's anonymous-key access pattern. A publishable/anon key is designed to be visible in browser code; **RLS is the actual security boundary**. The SQL setup currently present in the Android source defines permissive `anon` policies for all operations on both tables. That means anyone who obtains the public site URL and project key may be able to read or modify fleet and customer booking records. Do not use this setup with real customer information in production until access is restricted (ideally staff authentication plus role-based RLS). Do not add the service-role key to this site.
