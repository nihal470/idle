package com.example.data.repository

import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CreateBookingRequest
import com.example.data.model.CreateVehicleRequest
import com.example.data.model.UpdateBookingStatusRequest
import com.example.data.model.UpdateVehicleStatusRequest
import com.example.data.model.UpdateVehicleLifecycleRequest
import com.example.data.model.Vehicle
import com.example.data.model.VehicleStatus
import com.example.data.remote.SupabaseApi
import com.example.data.remote.SupabaseConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class FleetSnapshot(
    val vehicles: List<Vehicle>,
    val bookings: List<Booking>,
)

sealed interface RepoResult<out T> {
    data class Success<T>(val data: T) : RepoResult<T>
    data object MissingApiKey : RepoResult<Nothing>
    data class SchemaMissing(val detail: String) : RepoResult<Nothing>
    data class Error(val message: String) : RepoResult<Nothing>
}

class FleetRepository(
    private val api: SupabaseApi,
) {
    private fun isoNowUtc(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    suspend fun fetchFleetSnapshot(): RepoResult<FleetSnapshot> {
        if (!SupabaseConfig.isApiKeyConfigured) {
            return RepoResult.MissingApiKey
        }
        return try {
            coroutineScope {
                val vehiclesDeferred = async { api.getVehicles() }
                val bookingsDeferred = async { api.getBookings() }

                val vehiclesResp = vehiclesDeferred.await()
                val bookingsResp = bookingsDeferred.await()

                classifyError(vehiclesResp)?.let { return@coroutineScope it }
                classifyError(bookingsResp)?.let { return@coroutineScope it }

                val vehicles = vehiclesResp.body().orEmpty().map { it.toDomain() }
                val bookings = bookingsResp.body().orEmpty().map { it.toDomain() }
                RepoResult.Success(FleetSnapshot(vehicles = vehicles, bookings = bookings))
            }
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Unable to reach Supabase")
        }
    }

    suspend fun createVehicle(request: CreateVehicleRequest): RepoResult<Vehicle> {
        if (!SupabaseConfig.isApiKeyConfigured) return RepoResult.MissingApiKey
        return try {
            val response = api.createVehicle(request)
            classifyError(response)?.let { return it }
            val created = response.body()?.firstOrNull()?.toDomain()
                ?: return RepoResult.Error("Supabase did not return the created vehicle.")
            RepoResult.Success(created)
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Failed to create vehicle")
        }
    }

    suspend fun updateVehicle(id: String, request: CreateVehicleRequest): RepoResult<Vehicle> {
        if (!SupabaseConfig.isApiKeyConfigured) return RepoResult.MissingApiKey
        return try {
            val response = api.updateVehicle(idFilter = "eq.$id", request = request)
            classifyError(response)?.let { return it }
            val updated = response.body()?.firstOrNull()?.toDomain()
                ?: return RepoResult.Error("Vehicle update returned empty response.")
            RepoResult.Success(updated)
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Failed to update vehicle")
        }
    }

    suspend fun updateVehicleStatus(
        id: String,
        status: VehicleStatus,
        currentRenter: String? = null,
        returnDue: String? = null,
        maintenanceNote: String? = null,
    ): RepoResult<Vehicle> {
        if (!SupabaseConfig.isApiKeyConfigured) return RepoResult.MissingApiKey
        return try {
            val payload = UpdateVehicleStatusRequest(
                status = status.wireValue,
                currentRenter = if (status == VehicleStatus.ON_RENT) currentRenter else null,
                returnDue = if (status == VehicleStatus.ON_RENT) returnDue else null,
                maintenanceNote = if (status == VehicleStatus.MAINTENANCE) maintenanceNote else null,
                updatedAt = isoNowUtc(),
            )
            val response = api.updateVehicleStatus(idFilter = "eq.$id", request = payload)
            classifyError(response)?.let { return it }
            val updated = response.body()?.firstOrNull()?.toDomain()
                ?: return RepoResult.Error("Status update returned empty response.")
            RepoResult.Success(updated)
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Failed to update vehicle status")
        }
    }

    suspend fun returnVehicle(
        id: String,
        returnDate: String,
        reason: String,
        remarks: String?,
    ): RepoResult<Vehicle> {
        if (!SupabaseConfig.isApiKeyConfigured) return RepoResult.MissingApiKey
        return try {
            val payload = UpdateVehicleLifecycleRequest(
                fleetStatus = "RETURNED",
                fleetReturnDate = returnDate,
                fleetReturnReason = reason,
                fleetReturnRemarks = remarks?.trim()?.takeIf { it.isNotBlank() } ?: "",
                status = VehicleStatus.AVAILABLE.wireValue,
                currentRenter = "",
                returnDue = "",
                updatedAt = isoNowUtc(),
            )
            val response = api.updateVehicleLifecycle(idFilter = "eq.$id", request = payload)
            classifyError(response)?.let { return it }
            val updated = response.body()?.firstOrNull()?.toDomain()
                ?: return RepoResult.Error("Vehicle return update returned an empty response.")
            RepoResult.Success(updated)
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Failed to return vehicle")
        }
    }

    suspend fun reactivateVehicle(id: String): RepoResult<Vehicle> {
        if (!SupabaseConfig.isApiKeyConfigured) return RepoResult.MissingApiKey
        return try {
            val payload = UpdateVehicleLifecycleRequest(
                fleetStatus = "ACTIVE",
                updatedAt = isoNowUtc(),
            )
            val response = api.updateVehicleLifecycle(idFilter = "eq.$id", request = payload)
            classifyError(response)?.let { return it }
            val updated = response.body()?.firstOrNull()?.toDomain()
                ?: return RepoResult.Error("Vehicle reactivation returned an empty response.")
            RepoResult.Success(updated)
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Failed to reactivate vehicle")
        }
    }

    suspend fun deleteVehicle(id: String): RepoResult<Unit> {
        if (!SupabaseConfig.isApiKeyConfigured) return RepoResult.MissingApiKey
        return try {
            val response = api.deleteVehicle(idFilter = "eq.$id")
            classifyError(response)?.let { return it }
            RepoResult.Success(Unit)
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Failed to delete vehicle")
        }
    }

    suspend fun createReservation(
        vehicle: Vehicle,
        customerName: String,
        customerPhone: String?,
        customerEmail: String?,
        pickupDate: String,
        returnDate: String,
        days: Int,
        notes: String?,
        immediatePickup: Boolean,
    ): RepoResult<Booking> {
        if (!SupabaseConfig.isApiKeyConfigured) return RepoResult.MissingApiKey
        if (vehicle.isReturned) return RepoResult.Error("This vehicle has been returned and cannot be reserved.")
        return try {
            val codeNum = (1000..9999).random()
            val bookingStatus = if (immediatePickup) BookingStatus.ACTIVE else BookingStatus.PENDING
            val request = CreateBookingRequest(
                bookingCode = "IDL-$codeNum",
                vehicleId = vehicle.id,
                vehicleName = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                licensePlate = vehicle.licensePlate,
                customerName = customerName.trim(),
                customerPhone = customerPhone?.trim()?.takeIf { it.isNotBlank() },
                customerEmail = customerEmail?.trim()?.takeIf { it.isNotBlank() },
                pickupDate = pickupDate,
                returnDate = returnDate,
                days = days,
                totalPrice = vehicle.dailyRate * days,
                status = bookingStatus.wireValue,
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
            )
            val bookingResp = api.createBooking(request)
            classifyError(bookingResp)?.let { return it }
            val created = bookingResp.body()?.firstOrNull()?.toDomain()
                ?: return RepoResult.Error("Booking creation returned empty response.")

            // Synchronize vehicle inventory status to ON_RENT
            updateVehicleStatus(
                id = vehicle.id,
                status = VehicleStatus.ON_RENT,
                currentRenter = customerName.trim(),
                returnDue = returnDate,
            )

            RepoResult.Success(created)
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Failed to submit reservation")
        }
    }

    suspend fun transitionBooking(
        booking: Booking,
        newStatus: BookingStatus,
        targetVehicleStatus: VehicleStatus,
        maintenanceNote: String? = null,
    ): RepoResult<Booking> {
        if (!SupabaseConfig.isApiKeyConfigured) return RepoResult.MissingApiKey
        return try {
            val bookingResp = api.updateBookingStatus(
                idFilter = "eq.${booking.id}",
                request = UpdateBookingStatusRequest(status = newStatus.wireValue),
            )
            classifyError(bookingResp)?.let { return it }
            val updatedBooking = bookingResp.body()?.firstOrNull()?.toDomain()
                ?: return RepoResult.Error("Booking status update failed.")

            updateVehicleStatus(
                id = booking.vehicleId,
                status = targetVehicleStatus,
                currentRenter = if (targetVehicleStatus == VehicleStatus.ON_RENT) booking.customerName else null,
                returnDue = if (targetVehicleStatus == VehicleStatus.ON_RENT) booking.returnDate else null,
                maintenanceNote = maintenanceNote,
            )

            RepoResult.Success(updatedBooking)
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Failed to update booking")
        }
    }

    suspend fun seedStarterFleet(): RepoResult<List<Vehicle>> {
        if (!SupabaseConfig.isApiKeyConfigured) return RepoResult.MissingApiKey
        val starter = listOf(
            CreateVehicleRequest(
                make = "Toyota",
                model = "Camry Hybrid XSE",
                year = 2025,
                licensePlate = "IDL-104A",
                category = "Sedan",
                dailyRate = 74.0,
                status = VehicleStatus.AVAILABLE.wireValue,
                seats = 5,
                transmission = "Automatic",
                fuelType = "Hybrid",
                mileage = 11240,
            ),
            CreateVehicleRequest(
                make = "Mazda",
                model = "CX-50 Turbo",
                year = 2025,
                licensePlate = "IDL-208B",
                category = "SUV",
                dailyRate = 92.0,
                status = VehicleStatus.AVAILABLE.wireValue,
                seats = 5,
                transmission = "AWD Auto",
                fuelType = "Petrol",
                mileage = 8430,
            ),
            CreateVehicleRequest(
                make = "Hyundai",
                model = "IONIQ 5 Long Range",
                year = 2026,
                licensePlate = "IDL-312E",
                category = "EV",
                dailyRate = 98.0,
                status = VehicleStatus.AVAILABLE.wireValue,
                seats = 5,
                transmission = "Single-Speed",
                fuelType = "Electric",
                mileage = 4190,
            ),
            CreateVehicleRequest(
                make = "BMW",
                model = "530i Executive",
                year = 2025,
                licensePlate = "IDL-419L",
                category = "Luxury",
                dailyRate = 145.0,
                status = VehicleStatus.ON_RENT.wireValue,
                seats = 5,
                transmission = "Automatic",
                fuelType = "Hybrid",
                mileage = 15600,
                currentRenter = "Elena Rostova",
                returnDue = "2026-11-22",
            ),
            CreateVehicleRequest(
                make = "Volvo",
                model = "XC60 Recharge",
                year = 2025,
                licensePlate = "IDL-522S",
                category = "SUV",
                dailyRate = 118.0,
                status = VehicleStatus.MAINTENANCE.wireValue,
                seats = 5,
                transmission = "AWD Auto",
                fuelType = "Plug-In Hybrid",
                mileage = 22410,
                maintenanceNote = "Scheduled 20k brake inspection & tire rotation",
            ),
            CreateVehicleRequest(
                make = "Volkswagen",
                model = "ID.4 Pro",
                year = 2025,
                licensePlate = "IDL-630C",
                category = "Compact",
                dailyRate = 68.0,
                status = VehicleStatus.AVAILABLE.wireValue,
                seats = 5,
                transmission = "Single-Speed",
                fuelType = "Electric",
                mileage = 9870,
            ),
        )
        return try {
            val response = api.createVehiclesBatch(starter)
            classifyError(response)?.let { return it }
            RepoResult.Success(response.body().orEmpty().map { it.toDomain() })
        } catch (e: Exception) {
            RepoResult.Error(e.localizedMessage ?: "Failed to seed starter fleet")
        }
    }

    private fun <T> classifyError(response: Response<T>): RepoResult<Nothing>? {
        if (response.isSuccessful) return null
        val code = response.code()
        val rawBody = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()

        return when {
            code == 401 || code == 403 -> {
                if (rawBody.contains("JWT", ignoreCase = true) ||
                    rawBody.contains("apikey", ignoreCase = true) ||
                    rawBody.contains("Invalid API key", ignoreCase = true)
                ) {
                    RepoResult.Error("Supabase rejected the API key ($code). Check SUPABASE_PUBLISHABLE_KEY in AI Studio Secrets.")
                } else {
                    RepoResult.SchemaMissing("Row-Level Security blocked access ($code). Run the SQL policy script in Supabase SQL Editor.")
                }
            }
            code == 404 || rawBody.contains("42P01") || rawBody.contains("PGRST205") ||
                rawBody.contains("Could not find the table", ignoreCase = true) -> {
                RepoResult.SchemaMissing("Tables 'vehicles' / 'bookings' not found in Supabase project '${SupabaseConfig.projectName}'.")
            }
            else -> {
                RepoResult.Error("Supabase error ($code): ${rawBody.take(140).ifBlank { response.message() }}")
            }
        }
    }
}
