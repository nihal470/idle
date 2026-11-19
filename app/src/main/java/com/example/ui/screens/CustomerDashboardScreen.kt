package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirlineSeatReclineNormal
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CustomerDashboardScreen(
    uiState: FleetUiState,
    onSelectCategory: (VehicleCategory?) -> Unit,
    onTripDaysChange: (Int) -> Unit,
    onToggleShowBusy: (Boolean) -> Unit,
    onReserveClick: (Vehicle) -> Unit,
    onOpenInspector: () -> Unit,
    onRetrySync: () -> Unit,
    onCopySql: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cars = uiState.customerVehicles

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "customer_banner") {
            SupabaseConnectionBanner(
                health = uiState.connectionHealth,
                onOpenInspector = onOpenInspector,
                onRetry = onRetrySync,
                onCopySql = onCopySql,
            )
        }

        item(key = "hero_banner") {
            CustomerHeroCard(
                availableCount = uiState.availableCount,
                selectedDays = uiState.customerTripDays,
                onSelectDays = onTripDaysChange,
            )
        }

        item(key = "category_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Browse Fleet",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.customer_show_all_toggle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Switch(
                            checked = uiState.customerShowBusyVehicles,
                            onCheckedChange = onToggleShowBusy,
                            modifier = Modifier.testTag("customer_show_busy_switch"),
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text("All Types") },
                            modifier = Modifier.testTag("category_chip_all"),
                        )
                    }
                    items(VehicleCategory.entries) { cat ->
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == cat,
                            onClick = { onSelectCategory(cat) },
                            label = { Text(cat.label) },
                            modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        if (cars.isEmpty()) {
            item(key = "empty_customer_cars") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = stringResource(R.string.empty_available_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.empty_available_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(items = cars, key = { it.id }) { vehicle ->
                CustomerShowroomCard(
                    vehicle = vehicle,
                    tripDays = uiState.customerTripDays,
                    onReserveClick = { onReserveClick(vehicle) },
                )
            }
        }

        item(key = "customer_bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CustomerHeroCard(
    availableCount: Int,
    selectedDays: Int,
    onSelectDays: (Int) -> Unit,
) {
    val statusColors = IdleFleetTheme.statusColors
    val dayPresets = listOf(1, 3, 5, 7)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_fleet_hero_1795124725060),
            contentDescription = "Idle Fleet showroom",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            statusColors.heroScrimStart,
                            statusColors.heroScrimEnd,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                color = statusColors.availableContainer,
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = statusColors.available,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "$availableCount Ready for Instant Pickup",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColors.onAvailableContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.customer_hero_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.customer_trip_length),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    dayPresets.forEach { days ->
                        val active = selectedDays == days
                        Surface(
                            onClick = { onSelectDays(days) },
                            color = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.testTag("trip_days_chip_$days"),
                        ) {
                            Text(
                                text = "${days}d",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerShowroomCard(
    vehicle: Vehicle,
    tripDays: Int,
    onReserveClick: () -> Unit,
) {
    val isAvailable = vehicle.status == VehicleStatus.AVAILABLE
    val estimatedTotal = vehicle.dailyRate * tripDays

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_car_card_${vehicle.licensePlate}"),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
            ) {
                VehicleThumbnail(
                    vehicle = vehicle,
                    modifier = Modifier.fillMaxSize(),
                )
                VehicleStatusPill(
                    status = vehicle.status,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
                LicensePlateBadge(
                    plate = vehicle.licensePlate,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${vehicle.year} · ${vehicle.category.label.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = vehicle.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "$%.0f", estimatedTotal),
                            style = FleetMonoPriceStyle,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = String.format(Locale.US, "$%.0f/day · %dd", vehicle.dailyRate, tripDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SpecChip(icon = Icons.Rounded.AirlineSeatReclineNormal, text = "${vehicle.seats} Seats")
                    SpecChip(icon = Icons.Rounded.Settings, text = vehicle.transmission)
                    SpecChip(icon = Icons.Rounded.LocalGasStation, text = vehicle.fuelType)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Button(
                    onClick = onReserveClick,
                    enabled = isAvailable,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("reserve_button_${vehicle.licensePlate}"),
                ) {
                    Text(
                        text = if (isAvailable) {
                            stringResource(R.string.action_reserve_now)
                        } else {
                            stringResource(R.string.label_currently_unavailable)
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationSheet(
    vehicle: Vehicle,
    initialDays: Int,
    isSubmitting: Boolean,
    onConfirmReservation: (
        customerName: String,
        customerPhone: String?,
        customerEmail: String?,
        pickupDate: String,
        returnDate: String,
        days: Int,
        notes: String?,
        immediatePickup: Boolean,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    var days by remember { mutableIntStateOf(initialDays.coerceIn(1, 30)) }
    var notes by remember { mutableStateOf("") }
    var immediatePickup by remember { mutableStateOf(true) }

    val (pickupDate, returnDate) = remember(days) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val start = fmt.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, days)
        val end = fmt.format(cal.time)
        start to end
    }

    val total = vehicle.dailyRate * days
    val canSubmit = customerName.trim().length >= 2 && !isSubmitting

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
                text = stringResource(R.string.sheet_reserve_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VehicleThumbnail(
                        vehicle = vehicle,
                        modifier = Modifier.size(width = 76.dp, height = 56.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${vehicle.year} ${vehicle.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${vehicle.licensePlate} · ${String.format(Locale.US, "$%.0f/day", vehicle.dailyRate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "$%.0f", total),
                        style = FleetMonoPriceStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Customer Full Name *") },
                placeholder = { Text("Jordan Taylor") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reserve_customer_name_input"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Phone") },
                    placeholder = { Text("+1 555-0192") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reserve_customer_phone_input"),
                )
                OutlinedTextField(
                    value = customerEmail,
                    onValueChange = { customerEmail = it },
                    label = { Text("Email") },
                    placeholder = { Text("jordan@mail.com") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reserve_customer_email_input"),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "$pickupDate → $returnDate",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 5, 7).forEach { preset ->
                        FilterChip(
                            selected = days == preset,
                            onClick = { days = preset },
                            label = { Text("${preset}d") },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Immediate Handover (Active)",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Sets booking to Active and vehicle to On Rent immediately",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = immediatePickup,
                    onCheckedChange = { immediatePickup = it },
                    modifier = Modifier.testTag("reserve_immediate_pickup_switch"),
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Special Requests / Notes") },
                placeholder = { Text("Child seat, airport terminal curb pickup…") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onConfirmReservation(
                        customerName,
                        customerPhone,
                        customerEmail,
                        pickupDate,
                        returnDate,
                        days,
                        notes,
                        immediatePickup,
                    )
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("confirm_reservation_button"),
            ) {
                Text(
                    text = "${stringResource(R.string.action_confirm_reservation)} · ${String.format(Locale.US, "$%.0f", total)}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
