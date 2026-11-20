package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.ConnectionHealth
import com.example.data.model.CreateVehicleRequest
import com.example.data.model.Vehicle
import com.example.data.model.VehicleCategory
import com.example.data.model.VehicleStatus
import com.example.data.remote.RealtimeConnectionState
import com.example.data.remote.SupabaseConfig
import com.example.data.remote.SupabaseRealtimeClient
import com.example.data.remote.SupabaseServiceFactory
import com.example.data.repository.FleetRepository
import com.example.data.repository.RepoResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FleetTab {
    IDLE_LIST,
    CUSTOMER_DASHBOARD,
    BOOKINGS,
}

data class FleetUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val connectionHealth: ConnectionHealth = ConnectionHealth.Connecting,
    val realtimeState: RealtimeConnectionState = RealtimeConnectionState.DISCONNECTED,
    val lastSyncedAt: String? = null,
    val selectedStatusFilter: VehicleStatus? = null,
    val searchQuery: String = "",
    val selectedCategoryFilter: VehicleCategory? = null,
    val customerShowBusyVehicles: Boolean = false,
    val customerTripDays: Int = 3,
    val selectedBookingFilter: BookingStatus? = null,
    val snackbarMessage: String? = null,
) {
    val activeFleetVehicles: List<Vehicle> get() = vehicles.filterNot { it.isReturned }
    val returnedVehicles: List<Vehicle> get() = vehicles.filter { it.isReturned }.sortedByDescending { it.fleetReturnDate.orEmpty() }
    val availableCount: Int get() = activeFleetVehicles.count { it.status == VehicleStatus.AVAILABLE }
    val onRentCount: Int get() = activeFleetVehicles.count { it.status == VehicleStatus.ON_RENT }
    val maintenanceCount: Int get() = activeFleetVehicles.count { it.status == VehicleStatus.MAINTENANCE }

    val activeBookingsCount: Int get() = bookings.count { it.status == BookingStatus.ACTIVE }
    val pendingBookingsCount: Int get() = bookings.count { it.status == BookingStatus.PENDING }
    val completedBookingsCount: Int get() = bookings.count { it.status == BookingStatus.COMPLETED }

    val filteredFleetVehicles: List<Vehicle>
        get() = activeFleetVehicles.filter { vehicle ->
            val matchesStatus = selectedStatusFilter == null || vehicle.status == selectedStatusFilter
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                vehicle.make.contains(q, ignoreCase = true) ||
                vehicle.model.contains(q, ignoreCase = true) ||
                vehicle.licensePlate.contains(q, ignoreCase = true) ||
                vehicle.category.label.contains(q, ignoreCase = true) ||
                (vehicle.currentRenter?.contains(q, ignoreCase = true) == true)
            matchesStatus && matchesQuery
        }.sortedWith(
            compareBy<Vehicle> {
                when (it.status) {
                    VehicleStatus.AVAILABLE -> 0
                    VehicleStatus.ON_RENT -> 1
                    VehicleStatus.MAINTENANCE -> 2
                }
            }.thenBy { it.make },
        )

    val customerVehicles: List<Vehicle>
        get() = activeFleetVehicles.filter { vehicle ->
            val matchesAvailability = customerShowBusyVehicles || vehicle.status == VehicleStatus.AVAILABLE
            val matchesCategory = selectedCategoryFilter == null || vehicle.category == selectedCategoryFilter
            matchesAvailability && matchesCategory
        }.sortedWith(
            compareBy<Vehicle> { if (it.status == VehicleStatus.AVAILABLE) 0 else 1 }
                .thenBy { it.dailyRate },
        )

    val filteredBookings: List<Booking>
        get() = bookings.filter { booking ->
            selectedBookingFilter == null || booking.status == selectedBookingFilter
        }
}

class FleetViewModel(
    private val repository: FleetRepository = FleetRepository(SupabaseServiceFactory.api),
    private val realtimeClient: SupabaseRealtimeClient = SupabaseServiceFactory.createRealtimeClient(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(FleetUiState())
    val uiState: StateFlow<FleetUiState> = _uiState.asStateFlow()

    init {
        observeRealtime()
        if (SupabaseConfig.isApiKeyConfigured) {
            realtimeClient.start()
            refreshData(showLoader = true)
            startFallbackPolling()
        } else {
            _uiState.update {
                it.copy(connectionHealth = ConnectionHealth.MissingApiKey)
            }
        }
    }

    private fun observeRealtime() {
        viewModelScope.launch {
            realtimeClient.state.collectLatest { rtState ->
                _uiState.update { current ->
                    val updatedHealth = when {
                        current.connectionHealth is ConnectionHealth.MissingApiKey -> current.connectionHealth
                        current.connectionHealth is ConnectionHealth.SchemaMissing -> current.connectionHealth
                        rtState == RealtimeConnectionState.SUBSCRIBED -> ConnectionHealth.LiveRealtime
                        current.connectionHealth is ConnectionHealth.LiveRealtime -> ConnectionHealth.ConnectedPolling
                        else -> current.connectionHealth
                    }
                    current.copy(
                        realtimeState = rtState,
                        connectionHealth = updatedHealth,
                    )
                }
            }
        }

        viewModelScope.launch {
            realtimeClient.tableChangeEvents.collect {
                // Live postgres_changes event received from Supabase Realtime
                refreshData(showLoader = false)
            }
        }
    }

    private fun startFallbackPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(20_000L)
                if (SupabaseConfig.isApiKeyConfigured &&
                    _uiState.value.realtimeState != RealtimeConnectionState.SUBSCRIBED &&
                    _uiState.value.connectionHealth !is ConnectionHealth.SchemaMissing
                ) {
                    refreshData(showLoader = false)
                }
            }
        }
    }

    fun refreshData(showLoader: Boolean = true) {
        viewModelScope.launch {
            if (showLoader) {
                _uiState.update { it.copy(isLoading = true) }
            }
            when (val result = repository.fetchFleetSnapshot()) {
                is RepoResult.Success -> {
                    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    _uiState.update { current ->
                        val health = if (current.realtimeState == RealtimeConnectionState.SUBSCRIBED) {
                            ConnectionHealth.LiveRealtime
                        } else {
                            ConnectionHealth.ConnectedPolling
                        }
                        current.copy(
                            vehicles = result.data.vehicles,
                            bookings = result.data.bookings,
                            isLoading = false,
                            connectionHealth = health,
                            lastSyncedAt = timeFmt,
                        )
                    }
                }
                is RepoResult.MissingApiKey -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            connectionHealth = ConnectionHealth.MissingApiKey,
                        )
                    }
                }
                is RepoResult.SchemaMissing -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            connectionHealth = ConnectionHealth.SchemaMissing(result.detail),
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            connectionHealth = ConnectionHealth.Error(result.message),
                        )
                    }
                }
            }
        }
    }

    fun setStatusFilter(status: VehicleStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setCategoryFilter(category: VehicleCategory?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun setCustomerShowBusy(showBusy: Boolean) {
        _uiState.update { it.copy(customerShowBusyVehicles = showBusy) }
    }

    fun setCustomerTripDays(days: Int) {
        _uiState.update { it.copy(customerTripDays = days.coerceIn(1, 30)) }
    }

    fun setBookingFilter(status: BookingStatus?) {
        _uiState.update { it.copy(selectedBookingFilter = status) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun quickUpdateVehicleStatus(
        vehicle: Vehicle,
        newStatus: VehicleStatus,
        currentRenter: String? = vehicle.currentRenter,
        returnDue: String? = vehicle.returnDue,
        maintenanceNote: String? = vehicle.maintenanceNote,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (
                val result = repository.updateVehicleStatus(
                    id = vehicle.id,
                    status = newStatus,
                    currentRenter = currentRenter,
                    returnDue = returnDue,
                    maintenanceNote = maintenanceNote,
                )
            ) {
                is RepoResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            vehicles = state.vehicles.map { if (it.id == result.data.id) result.data else it },
                            isSubmitting = false,
                            snackbarMessage = "${vehicle.displayName} marked ${newStatus.label}",
                        )
                    }
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    fun saveVehicle(
        existingId: String?,
        request: CreateVehicleRequest,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = if (existingId == null) {
                repository.createVehicle(request)
            } else {
                repository.updateVehicle(existingId, request)
            }
            when (result) {
                is RepoResult.Success -> {
                    _uiState.update { state ->
                        val updatedList = if (existingId == null) {
                            listOf(result.data) + state.vehicles
                        } else {
                            state.vehicles.map { if (it.id == existingId) result.data else it }
                        }
                        state.copy(
                            vehicles = updatedList,
                            isSubmitting = false,
                            snackbarMessage = if (existingId == null) {
                                "Added ${result.data.displayName} to Supabase"
                            } else {
                                "Updated ${result.data.displayName}"
                            },
                        )
                    }
                    onSuccess()
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    fun returnVehicle(
        vehicle: Vehicle,
        returnDate: String,
        reason: String,
        remarks: String?,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = repository.returnVehicle(vehicle.id, returnDate, reason, remarks)) {
                is RepoResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            vehicles = state.vehicles.map { if (it.id == vehicle.id) result.data else it },
                            isSubmitting = false,
                            snackbarMessage = "${vehicle.displayName} marked as returned",
                        )
                    }
                    onSuccess()
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    fun reactivateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = repository.reactivateVehicle(vehicle.id)) {
                is RepoResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            vehicles = state.vehicles.map { if (it.id == vehicle.id) result.data else it },
                            isSubmitting = false,
                            snackbarMessage = "${vehicle.displayName} reactivated",
                        )
                    }
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    fun deleteVehicle(vehicle: Vehicle, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = repository.deleteVehicle(vehicle.id)) {
                is RepoResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            vehicles = state.vehicles.filterNot { it.id == vehicle.id },
                            isSubmitting = false,
                            snackbarMessage = "Removed ${vehicle.displayName} from fleet",
                        )
                    }
                    onSuccess()
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    fun createReservation(
        vehicle: Vehicle,
        customerName: String,
        customerPhone: String?,
        customerEmail: String?,
        pickupDate: String,
        returnDate: String,
        days: Int,
        notes: String?,
        immediatePickup: Boolean,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (
                val result = repository.createReservation(
                    vehicle = vehicle,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    customerEmail = customerEmail,
                    pickupDate = pickupDate,
                    returnDate = returnDate,
                    days = days,
                    notes = notes,
                    immediatePickup = immediatePickup,
                )
            ) {
                is RepoResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            bookings = listOf(result.data) + state.bookings,
                            isSubmitting = false,
                            snackbarMessage = "Booked ${result.data.bookingCode} · ${vehicle.displayName} is now On Rent",
                        )
                    }
                    refreshData(showLoader = false)
                    onSuccess()
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    fun confirmPickup(booking: Booking) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (
                val result = repository.transitionBooking(
                    booking = booking,
                    newStatus = BookingStatus.ACTIVE,
                    targetVehicleStatus = VehicleStatus.ON_RENT,
                )
            ) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            snackbarMessage = "${booking.bookingCode} picked up · Vehicle On Rent",
                        )
                    }
                    refreshData(showLoader = false)
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    fun completeReturn(
        booking: Booking,
        sendToMaintenance: Boolean,
        maintenanceNote: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val nextVehicleStatus = if (sendToMaintenance) {
                VehicleStatus.MAINTENANCE
            } else {
                VehicleStatus.AVAILABLE
            }
            when (
                val result = repository.transitionBooking(
                    booking = booking,
                    newStatus = BookingStatus.COMPLETED,
                    targetVehicleStatus = nextVehicleStatus,
                    maintenanceNote = maintenanceNote,
                )
            ) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            snackbarMessage = "${booking.bookingCode} returned · Car set to ${nextVehicleStatus.label}",
                        )
                    }
                    refreshData(showLoader = false)
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    fun cancelBooking(booking: Booking) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (
                val result = repository.transitionBooking(
                    booking = booking,
                    newStatus = BookingStatus.CANCELLED,
                    targetVehicleStatus = VehicleStatus.AVAILABLE,
                )
            ) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            snackbarMessage = "${booking.bookingCode} cancelled · Vehicle Available",
                        )
                    }
                    refreshData(showLoader = false)
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    fun seedStarterFleet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = repository.seedStarterFleet()) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            snackbarMessage = "Seeded ${result.data.size} vehicles to Supabase project '${SupabaseConfig.projectName}'",
                        )
                    }
                    refreshData(showLoader = false)
                }
                else -> handleMutationFailure(result)
            }
        }
    }

    private fun handleMutationFailure(result: RepoResult<*>) {
        val message = when (result) {
            is RepoResult.MissingApiKey -> "Configure SUPABASE_PUBLISHABLE_KEY in AI Studio Secrets first."
            is RepoResult.SchemaMissing -> result.detail
            is RepoResult.Error -> result.message
            is RepoResult.Success -> return
        }
        _uiState.update {
            it.copy(
                isSubmitting = false,
                snackbarMessage = message,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeClient.stop()
    }
}
