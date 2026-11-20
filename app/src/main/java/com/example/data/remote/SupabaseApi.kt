package com.example.data.remote

import com.example.data.model.BookingDto
import com.example.data.model.CreateBookingRequest
import com.example.data.model.CreateVehicleRequest
import com.example.data.model.UpdateBookingStatusRequest
import com.example.data.model.UpdateVehicleStatusRequest
import com.example.data.model.UpdateVehicleLifecycleRequest
import com.example.data.model.VehicleDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {

    @GET("rest/v1/vehicles")
    suspend fun getVehicles(
        @Query("select") select: String = "*",
        @Query("order") order: String = "updated_at.desc",
    ): Response<List<VehicleDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/vehicles")
    suspend fun createVehicle(
        @Body request: CreateVehicleRequest,
    ): Response<List<VehicleDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/vehicles")
    suspend fun createVehiclesBatch(
        @Body requests: List<CreateVehicleRequest>,
    ): Response<List<VehicleDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/vehicles")
    suspend fun updateVehicle(
        @Query("id") idFilter: String,
        @Body request: CreateVehicleRequest,
    ): Response<List<VehicleDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/vehicles")
    suspend fun updateVehicleStatus(
        @Query("id") idFilter: String,
        @Body request: UpdateVehicleStatusRequest,
    ): Response<List<VehicleDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/vehicles")
    suspend fun updateVehicleLifecycle(
        @Query("id") idFilter: String,
        @Body request: UpdateVehicleLifecycleRequest,
    ): Response<List<VehicleDto>>

    @DELETE("rest/v1/vehicles")
    suspend fun deleteVehicle(
        @Query("id") idFilter: String,
    ): Response<Unit>

    @GET("rest/v1/bookings")
    suspend fun getBookings(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
    ): Response<List<BookingDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/bookings")
    suspend fun createBooking(
        @Body request: CreateBookingRequest,
    ): Response<List<BookingDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/bookings")
    suspend fun updateBookingStatus(
        @Query("id") idFilter: String,
        @Body request: UpdateBookingStatusRequest,
    ): Response<List<BookingDto>>
}
