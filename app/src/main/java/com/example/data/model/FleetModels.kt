package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class VehicleStatus(val wireValue: String, val label: String) {
    AVAILABLE("AVAILABLE", "Available"),
    ON_RENT("ON_RENT", "On Rent"),
    MAINTENANCE("MAINTENANCE", "Maintenance");

    companion object {
        fun fromWire(value: String?): VehicleStatus {
            return when (value?.trim()?.uppercase()?.replace(" ", "_")) {
                "ON_RENT", "ONRENT", "RENTED" -> ON_RENT
                "MAINTENANCE", "MAINTANANCE", "SERVICE" -> MAINTENANCE
                else -> AVAILABLE
            }
        }
    }
}

enum class VehicleCategory(val label: String, @DrawableRes val fallbackDrawable: Int) {
    SEDAN("Sedan", R.drawable.img_car_sedan_1795124722321),
    SUV("SUV", R.drawable.img_car_suv_1795124723594),
    EV("EV", R.drawable.img_car_ev_1795124723597),
    LUXURY("Luxury", R.drawable.img_car_sedan_1795124722321),
    COMPACT("Compact", R.drawable.img_car_ev_1795124723597);

    companion object {
        fun fromLabel(value: String?): VehicleCategory {
            return entries.firstOrNull {
                it.name.equals(value?.trim(), ignoreCase = true) ||
                    it.label.equals(value?.trim(), ignoreCase = true)
            } ?: SEDAN
        }
    }
}

enum class BookingStatus(val wireValue: String, val label: String) {
    PENDING("PENDING", "Pending"),
    ACTIVE("ACTIVE", "Active"),
    COMPLETED("COMPLETED", "Completed"),
    CANCELLED("CANCELLED", "Cancelled");

    companion object {
        fun fromWire(value: String?): BookingStatus {
            return when (value?.trim()?.uppercase()) {
                "ACTIVE", "ON_RENT", "CONFIRMED" -> ACTIVE
                "COMPLETED", "RETURNED" -> COMPLETED
                "CANCELLED", "CANCELED" -> CANCELLED
                else -> PENDING
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class VehicleDto(
    @Json(name = "id") val id: String,
    @Json(name = "make") val make: String,
    @Json(name = "model") val model: String,
    @Json(name = "year") val year: Int = 2025,
    @Json(name = "license_plate") val licensePlate: String,
    @Json(name = "category") val category: String = "Sedan",
    @Json(name = "daily_rate") val dailyRate: Double = 69.0,
    @Json(name = "status") val status: String = "AVAILABLE",
    @Json(name = "seats") val seats: Int = 5,
    @Json(name = "transmission") val transmission: String = "Automatic",
    @Json(name = "fuel_type") val fuelType: String = "Hybrid",
    @Json(name = "mileage") val mileage: Int = 0,
    @Json(name = "current_renter") val currentRenter: String? = null,
    @Json(name = "return_due") val returnDue: String? = null,
    @Json(name = "maintenance_note") val maintenanceNote: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "fleet_status") val fleetStatus: String = "ACTIVE",
    @Json(name = "fleet_return_date") val fleetReturnDate: String? = null,
    @Json(name = "fleet_return_reason") val fleetReturnReason: String? = null,
    @Json(name = "fleet_return_remarks") val fleetReturnRemarks: String? = null,
) {
    fun toDomain(): Vehicle = Vehicle(
        id = id,
        make = make,
        model = model,
        year = year,
        licensePlate = licensePlate,
        category = VehicleCategory.fromLabel(category),
        dailyRate = dailyRate,
        status = VehicleStatus.fromWire(status),
        seats = seats,
        transmission = transmission,
        fuelType = fuelType,
        mileage = mileage,
        currentRenter = currentRenter?.takeIf { it.isNotBlank() },
        returnDue = returnDue?.takeIf { it.isNotBlank() },
        maintenanceNote = maintenanceNote?.takeIf { it.isNotBlank() },
        imageUrl = imageUrl?.takeIf { it.isNotBlank() },
        updatedAt = updatedAt,
        fleetStatus = fleetStatus,
        fleetReturnDate = fleetReturnDate,
        fleetReturnReason = fleetReturnReason,
        fleetReturnRemarks = fleetReturnRemarks,
    )
}

data class Vehicle(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val licensePlate: String,
    val category: VehicleCategory,
    val dailyRate: Double,
    val status: VehicleStatus,
    val seats: Int,
    val transmission: String,
    val fuelType: String,
    val mileage: Int,
    val currentRenter: String? = null,
    val returnDue: String? = null,
    val maintenanceNote: String? = null,
    val imageUrl: String? = null,
    val updatedAt: String? = null,
    val fleetStatus: String = "ACTIVE",
    val fleetReturnDate: String? = null,
    val fleetReturnReason: String? = null,
    val fleetReturnRemarks: String? = null,
) {
    val isReturned: Boolean get() = fleetStatus.equals("RETURNED", ignoreCase = true)
    val displayName: String get() = "$make $model"
}

@JsonClass(generateAdapter = true)
data class CreateVehicleRequest(
    @Json(name = "make") val make: String,
    @Json(name = "model") val model: String,
    @Json(name = "year") val year: Int,
    @Json(name = "license_plate") val licensePlate: String,
    @Json(name = "category") val category: String,
    @Json(name = "daily_rate") val dailyRate: Double,
    @Json(name = "status") val status: String,
    @Json(name = "seats") val seats: Int,
    @Json(name = "transmission") val transmission: String,
    @Json(name = "fuel_type") val fuelType: String,
    @Json(name = "mileage") val mileage: Int,
    @Json(name = "current_renter") val currentRenter: String? = null,
    @Json(name = "return_due") val returnDue: String? = null,
    @Json(name = "maintenance_note") val maintenanceNote: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class UpdateVehicleLifecycleRequest(
    @Json(name = "fleet_status") val fleetStatus: String,
    @Json(name = "fleet_return_date") val fleetReturnDate: String? = null,
    @Json(name = "fleet_return_reason") val fleetReturnReason: String? = null,
    @Json(name = "fleet_return_remarks") val fleetReturnRemarks: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "current_renter") val currentRenter: String? = null,
    @Json(name = "return_due") val returnDue: String? = null,
    @Json(name = "updated_at") val updatedAt: String,
)

@JsonClass(generateAdapter = true)
data class UpdateVehicleStatusRequest(
    @Json(name = "status") val status: String,
    @Json(name = "current_renter") val currentRenter: String? = null,
    @Json(name = "return_due") val returnDue: String? = null,
    @Json(name = "maintenance_note") val maintenanceNote: String? = null,
    @Json(name = "updated_at") val updatedAt: String,
)

@JsonClass(generateAdapter = true)
data class BookingDto(
    @Json(name = "id") val id: String,
    @Json(name = "booking_code") val bookingCode: String? = null,
    @Json(name = "vehicle_id") val vehicleId: String,
    @Json(name = "vehicle_name") val vehicleName: String? = null,
    @Json(name = "license_plate") val licensePlate: String? = null,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "customer_phone") val customerPhone: String? = null,
    @Json(name = "customer_email") val customerEmail: String? = null,
    @Json(name = "pickup_date") val pickupDate: String,
    @Json(name = "return_date") val returnDate: String,
    @Json(name = "days") val days: Int = 1,
    @Json(name = "total_price") val totalPrice: Double = 0.0,
    @Json(name = "status") val status: String = "ACTIVE",
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
) {
    fun toDomain(): Booking = Booking(
        id = id,
        bookingCode = bookingCode ?: "IDL-${id.take(6).uppercase()}",
        vehicleId = vehicleId,
        vehicleName = vehicleName ?: "Fleet Vehicle",
        licensePlate = licensePlate ?: "—",
        customerName = customerName,
        customerPhone = customerPhone?.takeIf { it.isNotBlank() },
        customerEmail = customerEmail?.takeIf { it.isNotBlank() },
        pickupDate = pickupDate,
        returnDate = returnDate,
        days = days.coerceAtLeast(1),
        totalPrice = totalPrice,
        status = BookingStatus.fromWire(status),
        notes = notes?.takeIf { it.isNotBlank() },
        createdAt = createdAt,
    )
}

data class Booking(
    val id: String,
    val bookingCode: String,
    val vehicleId: String,
    val vehicleName: String,
    val licensePlate: String,
    val customerName: String,
    val customerPhone: String?,
    val customerEmail: String?,
    val pickupDate: String,
    val returnDate: String,
    val days: Int,
    val totalPrice: Double,
    val status: BookingStatus,
    val notes: String?,
    val createdAt: String?,
)

@JsonClass(generateAdapter = true)
data class CreateBookingRequest(
    @Json(name = "booking_code") val bookingCode: String,
    @Json(name = "vehicle_id") val vehicleId: String,
    @Json(name = "vehicle_name") val vehicleName: String,
    @Json(name = "license_plate") val licensePlate: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "customer_phone") val customerPhone: String?,
    @Json(name = "customer_email") val customerEmail: String?,
    @Json(name = "pickup_date") val pickupDate: String,
    @Json(name = "return_date") val returnDate: String,
    @Json(name = "days") val days: Int,
    @Json(name = "total_price") val totalPrice: Double,
    @Json(name = "status") val status: String,
    @Json(name = "notes") val notes: String?,
)

@JsonClass(generateAdapter = true)
data class UpdateBookingStatusRequest(
    @Json(name = "status") val status: String,
)

sealed interface ConnectionHealth {
    data object Connecting : ConnectionHealth
    data object LiveRealtime : ConnectionHealth
    data object ConnectedPolling : ConnectionHealth
    data object MissingApiKey : ConnectionHealth
    data class SchemaMissing(val detail: String) : ConnectionHealth
    data class Error(val message: String) : ConnectionHealth
}

object SupabaseSchemaSql {
    val setupScript: String = """
        -- Run in Supabase SQL Editor for project: idle
        create extension if not exists "pgcrypto";

        create table if not exists public.vehicles (
          id uuid primary key default gen_random_uuid(),
          make text not null,
          model text not null,
          year int not null default 2025,
          license_plate text not null unique,
          category text not null default 'Sedan',
          daily_rate numeric(10,2) not null default 69.00,
          status text not null default 'AVAILABLE'
            check (status in ('AVAILABLE', 'ON_RENT', 'MAINTENANCE')),
          seats int not null default 5,
          transmission text not null default 'Automatic',
          fuel_type text not null default 'Hybrid',
          mileage int not null default 0,
          current_renter text,
          return_due text,
          maintenance_note text,
          image_url text,
          updated_at timestamptz not null default now()
        );

        create table if not exists public.bookings (
          id uuid primary key default gen_random_uuid(),
          booking_code text not null,
          vehicle_id uuid not null references public.vehicles(id) on delete cascade,
          vehicle_name text not null,
          license_plate text not null,
          customer_name text not null,
          customer_phone text,
          customer_email text,
          pickup_date text not null,
          return_date text not null,
          days int not null default 1,
          total_price numeric(10,2) not null default 0,
          status text not null default 'ACTIVE'
            check (status in ('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
          notes text,
          created_at timestamptz not null default now()
        );

        -- Enable RLS with policies for prototype access
        alter table public.vehicles enable row level security;
        alter table public.bookings enable row level security;

        drop policy if exists "Allow anon access vehicles" on public.vehicles;
        create policy "Allow anon access vehicles"
          on public.vehicles for all to anon, authenticated
          using (true) with check (true);

        drop policy if exists "Allow anon access bookings" on public.bookings;
        create policy "Allow anon access bookings"
          on public.bookings for all to anon, authenticated
          using (true) with check (true);

        -- Enable Realtime postgres_changes streaming
        alter publication supabase_realtime add table public.vehicles;
        alter publication supabase_realtime add table public.bookings;
    """.trimIndent()
}
