package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ConnectionHealth
import com.example.data.model.CreateVehicleRequest
import com.example.data.model.Vehicle
import com.example.data.model.VehicleCategory
import com.example.data.model.VehicleStatus
import com.example.ui.FleetUiState
import com.example.ui.components.LicensePlateBadge
import com.example.ui.components.SupabaseConnectionBanner
import com.example.ui.components.VehicleStatusPill
import com.example.ui.components.VehicleThumbnail
import com.example.ui.theme.FleetMonoPriceStyle
import com.example.ui.theme.IdleFleetTheme
import java.util.Locale

@Composable
fun IdleListScreen(
    uiState: FleetUiState,
    onSelectStatusFilter: (VehicleStatus?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onQuickStatusChange: (Vehicle, VehicleStatus, String?) -> Unit,
    onReturnVehicle: (Vehicle, String, String, String?) -> Unit,
    onReactivateVehicle: (Vehicle) -> Unit,
    onEditVehicle: (Vehicle) -> Unit,
    onAddVehicleClick: () -> Unit,
    onSeedFleetClick: () -> Unit,
    onOpenInspector: () -> Unit,
    onRetrySync: () -> Unit,
    onCopySql: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var maintenancePromptVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var returnPromptVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var showReturnedVehicles by remember { mutableStateOf(false) }
    var returnDate by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())) }
    var returnReason by remember { mutableStateOf("Lease expired") }
    var returnRemarks by remember { mutableStateOf("") }

    val vehicles = if (showReturnedVehicles) uiState.returnedVehicles else uiState.filteredFleetVehicles

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "connection_banner") {
            SupabaseConnectionBanner(
                health = uiState.connectionHealth,
                onOpenInspector = onOpenInspector,
                onRetry = onRetrySync,
                onCopySql = onCopySql,
            )
        }

        item(key = "fleet_lifecycle_filter") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !showReturnedVehicles,
                    onClick = { showReturnedVehicles = false },
                    label = { Text("Active fleet (${uiState.activeFleetVehicles.size})") },
                    modifier = Modifier.testTag("filter_active_fleet"),
                )
                FilterChip(
                    selected = showReturnedVehicles,
                    onClick = { showReturnedVehicles = true },
                    label = { Text("Returned (${uiState.returnedVehicles.size})") },
                    modifier = Modifier.testTag("filter_returned_fleet"),
                )
            }
        }

        if (!showReturnedVehicles) item(key = "kpi_strip") {
            FleetKpiStrip(
                totalCount = uiState.vehicles.size,
                availableCount = uiState.availableCount,
                onRentCount = uiState.onRentCount,
                maintenanceCount = uiState.maintenanceCount,
                selectedFilter = uiState.selectedStatusFilter,
                onSelectFilter = onSelectStatusFilter,
            )
        }

        if (!showReturnedVehicles) item(key = "search_field") {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_vehicles_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("clear_search_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fleet_search_input"),
            )
        }

        if (uiState.isLoading && uiState.vehicles.isEmpty()) {
            item(key = "loading_indicator") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.vehicles.isEmpty()) {
            item(key = "empty_fleet") {
                EmptyFleetCard(
                    canWriteToSupabase = uiState.connectionHealth is ConnectionHealth.LiveRealtime ||
                        uiState.connectionHealth is ConnectionHealth.ConnectedPolling,
                    isSubmitting = uiState.isSubmitting,
                    onAddVehicleClick = onAddVehicleClick,
                    onSeedFleetClick = onSeedFleetClick,
                )
            }
        } else if (vehicles.isEmpty()) {
            item(key = "empty_filter") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = stringResource(R.string.empty_filter_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.empty_filter_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(items = vehicles, key = { it.id }) { vehicle ->
                if (showReturnedVehicles) {
                    ReturnedVehicleCard(
                        vehicle = vehicle,
                        isSubmitting = uiState.isSubmitting,
                        onReactivate = { onReactivateVehicle(vehicle) },
                    )
                } else FleetVehicleCard(
                    vehicle = vehicle,
                    isSubmitting = uiState.isSubmitting,
                    onCardClick = { onEditVehicle(vehicle) },
                    onReturnVehicle = { returnPromptVehicle = vehicle },
                    onSelectStatus = { targetStatus ->
                        if (targetStatus == VehicleStatus.MAINTENANCE && vehicle.status != VehicleStatus.MAINTENANCE) {
                            maintenancePromptVehicle = vehicle
                        } else if (targetStatus != vehicle.status) {
                            onQuickStatusChange(vehicle, targetStatus, null)
                        }
                    },
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(76.dp))
        }
    }

    returnPromptVehicle?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { if (!uiState.isSubmitting) returnPromptVehicle = null },
            title = { Text("Return leased vehicle?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${vehicle.year} ${vehicle.displayName} · ${vehicle.licensePlate}")
                    OutlinedTextField(
                        value = returnDate,
                        onValueChange = { returnDate = it },
                        label = { Text("Return date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("return_vehicle_date"),
                    )
                    Text("Reason", style = MaterialTheme.typography.labelLarge)
                    listOf("Lease expired", "Owner requested", "Other").forEach { reason ->
                        FilterChip(
                            selected = returnReason == reason,
                            onClick = { returnReason = reason },
                            label = { Text(reason) },
                        )
                    }
                    OutlinedTextField(
                        value = returnRemarks,
                        onValueChange = { returnRemarks = it },
                        label = { Text("Remarks (optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("return_vehicle_remarks"),
                    )
                    Text(
                        "This removes the vehicle from the active fleet but keeps its record and booking history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !uiState.isSubmitting && returnDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
                    onClick = {
                        onReturnVehicle(vehicle, returnDate, returnReason, returnRemarks)
                        returnPromptVehicle = null
                        returnDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
                        returnReason = "Lease expired"
                        returnRemarks = ""
                    },
                    modifier = Modifier.testTag("confirm_return_vehicle"),
                ) { Text(if (uiState.isSubmitting) "Saving…" else "Confirm return") }
            },
            dismissButton = {
                TextButton(onClick = { returnPromptVehicle = null }, enabled = !uiState.isSubmitting) {
                    Text("Cancel")
                }
            },
        )
    }

    maintenancePromptVehicle?.let { vehicle ->
        MaintenanceNoteDialog(
            vehicle = vehicle,
            onConfirm = { note ->
                maintenancePromptVehicle = null
                onQuickStatusChange(vehicle, VehicleStatus.MAINTENANCE, note.ifBlank { "Scheduled inspection" })
            },
            onDismiss = { maintenancePromptVehicle = null },
        )
    }
}

@Composable
private fun FleetKpiStrip(
    totalCount: Int,
    availableCount: Int,
    onRentCount: Int,
    maintenanceCount: Int,
    selectedFilter: VehicleStatus?,
    onSelectFilter: (VehicleStatus?) -> Unit,
) {
    val statusColors = IdleFleetTheme.statusColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KpiTile(
            label = "All",
            count = totalCount,
            accent = MaterialTheme.colorScheme.primary,
            selected = selectedFilter == null,
            testTag = "filter_all_tile",
            onClick = { onSelectFilter(null) },
            modifier = Modifier.weight(1f),
        )
        KpiTile(
            label = "Idle",
            count = availableCount,
            accent = statusColors.available,
            selected = selectedFilter == VehicleStatus.AVAILABLE,
            testTag = "filter_available_tile",
            onClick = { onSelectFilter(VehicleStatus.AVAILABLE) },
            modifier = Modifier.weight(1f),
        )
        KpiTile(
            label = "Rent",
            count = onRentCount,
            accent = statusColors.onRent,
            selected = selectedFilter == VehicleStatus.ON_RENT,
            testTag = "filter_on_rent_tile",
            onClick = { onSelectFilter(VehicleStatus.ON_RENT) },
            modifier = Modifier.weight(1f),
        )
        KpiTile(
            label = "Maint.",
            count = maintenanceCount,
            accent = statusColors.maintenance,
            selected = selectedFilter == VehicleStatus.MAINTENANCE,
            testTag = "filter_maintenance_tile",
            onClick = { onSelectFilter(VehicleStatus.MAINTENANCE) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KpiTile(
    label: String,
    count: Int,
    accent: Color,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .heightIn(min = 70.dp)
            .testTag(testTag),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FleetVehicleCard(
    vehicle: Vehicle,
    isSubmitting: Boolean,
    onCardClick: () -> Unit,
    onReturnVehicle: () -> Unit,
    onSelectStatus: (VehicleStatus) -> Unit,
) {
    val statusColors = IdleFleetTheme.statusColors

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("vehicle_card_${vehicle.licensePlate}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VehicleThumbnail(
                    vehicle = vehicle,
                    modifier = Modifier.size(width = 96.dp, height = 74.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LicensePlateBadge(plate = vehicle.licensePlate)
                        VehicleStatusPill(status = vehicle.status)
                    }

                    Text(
                        text = "${vehicle.year} ${vehicle.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${vehicle.category.label} · ${vehicle.seats} seats · ${vehicle.fuelType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "$%.0f/d", vehicle.dailyRate),
                            style = FleetMonoPriceStyle,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = vehicle.status == VehicleStatus.ON_RENT && !vehicle.currentRenter.isNullOrBlank()) {
                Surface(
                    color = statusColors.onRentContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = statusColors.onOnRentContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = buildString {
                                append("Renter: ${vehicle.currentRenter}")
                                if (!vehicle.returnDue.isNullOrBlank()) {
                                    append(" · Due ${vehicle.returnDue}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColors.onOnRentContainer,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = vehicle.status == VehicleStatus.MAINTENANCE && !vehicle.maintenanceNote.isNullOrBlank()) {
                Surface(
                    color = statusColors.maintenanceContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Build,
                            contentDescription = null,
                            tint = statusColors.onMaintenanceContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = vehicle.maintenanceNote.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColors.onMaintenanceContainer,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            OutlinedButton(
                onClick = onReturnVehicle,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag("return_vehicle_${vehicle.licensePlate}"),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Return leased vehicle")
            }

            // One-tap status switcher row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusActionChip(
                    label = "Available",
                    icon = Icons.Rounded.CheckCircle,
                    active = vehicle.status == VehicleStatus.AVAILABLE,
                    activeBg = statusColors.availableContainer,
                    activeFg = statusColors.onAvailableContainer,
                    enabled = !isSubmitting,
                    testTag = "status_available_${vehicle.licensePlate}",
                    onClick = { onSelectStatus(VehicleStatus.AVAILABLE) },
                    modifier = Modifier.weight(1f),
                )
                StatusActionChip(
                    label = "On Rent",
                    icon = Icons.Rounded.VpnKey,
                    active = vehicle.status == VehicleStatus.ON_RENT,
                    activeBg = statusColors.onRentContainer,
                    activeFg = statusColors.onOnRentContainer,
                    enabled = !isSubmitting,
                    testTag = "status_on_rent_${vehicle.licensePlate}",
                    onClick = { onSelectStatus(VehicleStatus.ON_RENT) },
                    modifier = Modifier.weight(1f),
                )
                StatusActionChip(
                    label = "Maint.",
                    icon = Icons.Rounded.Build,
                    active = vehicle.status == VehicleStatus.MAINTENANCE,
                    activeBg = statusColors.maintenanceContainer,
                    activeFg = statusColors.onMaintenanceContainer,
                    enabled = !isSubmitting,
                    testTag = "status_maintenance_${vehicle.licensePlate}",
                    onClick = { onSelectStatus(VehicleStatus.MAINTENANCE) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onCardClick,
                    modifier = Modifier.testTag("edit_vehicle_${vehicle.licensePlate}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit ${vehicle.displayName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    activeBg: Color,
    activeFg: Color,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (active) activeBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) activeFg else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) activeFg else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyFleetCard(
    canWriteToSupabase: Boolean,
    isSubmitting: Boolean,
    onAddVehicleClick: () -> Unit,
    onSeedFleetClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
            Text(
                text = stringResource(R.string.empty_fleet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.empty_fleet_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (canWriteToSupabase) {
                Button(
                    onClick = onAddVehicleClick,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("empty_add_vehicle_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.action_add_vehicle))
                }
                OutlinedButton(
                    onClick = onSeedFleetClick,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("empty_seed_fleet_button"),
                ) {
                    Text(text = stringResource(R.string.action_seed_fleet))
                }
            }
        }
    }
}

@Composable
private fun MaintenanceNoteDialog(
    vehicle: Vehicle,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf(vehicle.maintenanceNote.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Maintenance Bay") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Add a service note for ${vehicle.displayName} (${vehicle.licensePlate}):",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Service / Inspection Note") },
                    placeholder = { Text("e.g., Oil change & brake check") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("maintenance_note_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note.trim()) },
                modifier = Modifier.testTag("confirm_maintenance_button"),
            ) {
                Text("Set Maintenance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleSheet(
    existingVehicle: Vehicle?,
    isSubmitting: Boolean,
    onSave: (CreateVehicleRequest) -> Unit,
    onDelete: ((Vehicle) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var make by remember { mutableStateOf(existingVehicle?.make ?: "") }
    var model by remember { mutableStateOf(existingVehicle?.model ?: "") }
    var yearText by remember { mutableStateOf((existingVehicle?.year ?: 2025).toString()) }
    var plate by remember { mutableStateOf(existingVehicle?.licensePlate ?: "") }
    var rateText by remember { mutableStateOf(existingVehicle?.dailyRate?.toInt()?.toString() ?: "79") }
    var seatsText by remember { mutableStateOf((existingVehicle?.seats ?: 5).toString()) }
    var mileageText by remember { mutableStateOf((existingVehicle?.mileage ?: 1000).toString()) }
    var fuelType by remember { mutableStateOf(existingVehicle?.fuelType ?: "Hybrid") }
    var transmission by remember { mutableStateOf(existingVehicle?.transmission ?: "Automatic") }
    var category by remember { mutableStateOf(existingVehicle?.category ?: VehicleCategory.SEDAN) }
    var status by remember { mutableStateOf(existingVehicle?.status ?: VehicleStatus.AVAILABLE) }
    var maintenanceNote by remember { mutableStateOf(existingVehicle?.maintenanceNote ?: "") }

    val canSave = make.isNotBlank() && model.isNotBlank() && plate.isNotBlank() && !isSubmitting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (existingVehicle == null) {
                    stringResource(R.string.sheet_add_vehicle_title)
                } else {
                    stringResource(R.string.sheet_edit_vehicle_title)
                },
                style = MaterialTheme.typography.headlineSmall,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = make,
                    onValueChange = { make = it },
                    label = { Text("Make *") },
                    placeholder = { Text("Toyota") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vehicle_make_input"),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model *") },
                    placeholder = { Text("Camry Hybrid") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vehicle_model_input"),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("License Plate *") },
                    placeholder = { Text("IDL-104A") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("vehicle_plate_input"),
                )
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Daily Rate ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("vehicle_rate_input"),
                )
                OutlinedTextField(
                    value = seatsText,
                    onValueChange = { seatsText = it.filter(Char::isDigit).take(2) },
                    label = { Text("Seats") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.6f),
                )
                OutlinedTextField(
                    value = mileageText,
                    onValueChange = { mileageText = it.filter(Char::isDigit) },
                    label = { Text("Mileage") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.9f),
                )
            }

            Text(text = "Category", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VehicleCategory.entries) { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item.label) },
                    )
                }
            }

            Text(text = "Fleet Status", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleStatus.entries.forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        label = { Text(st.label) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fuelType,
                    onValueChange = { fuelType = it },
                    label = { Text("Fuel Type") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = transmission,
                    onValueChange = { transmission = it },
                    label = { Text("Transmission") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            if (status == VehicleStatus.MAINTENANCE) {
                OutlinedTextField(
                    value = maintenanceNote,
                    onValueChange = { maintenanceNote = it },
                    label = { Text("Maintenance Note") },
                    placeholder = { Text("Brake pad replacement, detailing…") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    onSave(
                        CreateVehicleRequest(
                            make = make.trim(),
                            model = model.trim(),
                            year = yearText.toIntOrNull() ?: 2025,
                            licensePlate = plate.trim().uppercase(),
                            category = category.label,
                            dailyRate = rateText.toDoubleOrNull() ?: 75.0,
                            status = status.wireValue,
                            seats = seatsText.toIntOrNull() ?: 5,
                            transmission = transmission.trim().ifBlank { "Automatic" },
                            fuelType = fuelType.trim().ifBlank { "Hybrid" },
                            mileage = mileageText.toIntOrNull() ?: 0,
                            currentRenter = existingVehicle?.currentRenter,
                            returnDue = existingVehicle?.returnDue,
                            maintenanceNote = if (status == VehicleStatus.MAINTENANCE) maintenanceNote.trim() else null,
                            imageUrl = existingVehicle?.imageUrl,
                        ),
                    )
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("save_vehicle_button"),
            ) {
                Text(text = stringResource(R.string.action_save))
            }

            if (existingVehicle != null && onDelete != null) {
                OutlinedButton(
                    onClick = { onDelete(existingVehicle) },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("delete_vehicle_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.action_delete_vehicle))
                }
            }
        }
    }
}


@Composable
private fun ReturnedVehicleCard(
    vehicle: Vehicle,
    isSubmitting: Boolean,
    onReactivate: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("returned_vehicle_${vehicle.licensePlate}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LicensePlateBadge(plate = vehicle.licensePlate)
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        "RETURNED",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text("${vehicle.year} ${vehicle.displayName}", style = MaterialTheme.typography.titleMedium)
            Text("Return date: ${vehicle.fleetReturnDate ?: "—"}", style = MaterialTheme.typography.bodySmall)
            Text("Reason: ${vehicle.fleetReturnReason ?: "—"}", style = MaterialTheme.typography.bodySmall)
            if (!vehicle.fleetReturnRemarks.isNullOrBlank()) {
                Text("Remarks: ${vehicle.fleetReturnRemarks}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = onReactivate,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag("reactivate_vehicle_${vehicle.licensePlate}"),
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reactivate vehicle")
            }
        }
    }
}
