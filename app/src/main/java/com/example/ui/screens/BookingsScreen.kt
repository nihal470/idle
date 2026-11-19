package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentReturn
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.ui.FleetUiState
import com.example.ui.components.BookingStatusPill
import com.example.ui.components.LicensePlateBadge
import com.example.ui.components.SupabaseConnectionBanner
import com.example.ui.theme.FleetMonoCodeStyle
import com.example.ui.theme.FleetMonoPriceStyle
import com.example.ui.theme.IdleFleetTheme
import java.util.Locale

@Composable
fun BookingsScreen(
    uiState: FleetUiState,
    onSelectBookingFilter: (BookingStatus?) -> Unit,
    onConfirmPickup: (Booking) -> Unit,
    onCompleteReturn: (Booking, Boolean, String?) -> Unit,
    onCancelBooking: (Booking) -> Unit,
    onOpenInspector: () -> Unit,
    onRetrySync: () -> Unit,
    onCopySql: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var returnDialogBooking by remember { mutableStateOf<Booking?>(null) }
    val bookings = uiState.filteredBookings
    val statusColors = IdleFleetTheme.statusColors

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "bookings_banner") {
            SupabaseConnectionBanner(
                health = uiState.connectionHealth,
                onOpenInspector = onOpenInspector,
                onRetry = onRetrySync,
                onCopySql = onCopySql,
            )
        }

        item(key = "booking_summary_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BookingStatBox(
                    label = "Active",
                    value = uiState.activeBookingsCount,
                    accent = statusColors.onRent,
                    modifier = Modifier.weight(1f),
                )
                BookingStatBox(
                    label = "Pending",
                    value = uiState.pendingBookingsCount,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                BookingStatBox(
                    label = "Completed",
                    value = uiState.completedBookingsCount,
                    accent = statusColors.available,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "booking_filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.selectedBookingFilter == null,
                        onClick = { onSelectBookingFilter(null) },
                        label = { Text("All (${uiState.bookings.size})") },
                        modifier = Modifier.testTag("booking_filter_all"),
                    )
                }
                items(BookingStatus.entries) { status ->
                    FilterChip(
                        selected = uiState.selectedBookingFilter == status,
                        onClick = { onSelectBookingFilter(status) },
                        label = { Text(status.label) },
                        modifier = Modifier.testTag("booking_filter_${status.name.lowercase()}"),
                    )
                }
            }
        }

        if (bookings.isEmpty()) {
            item(key = "empty_bookings") {
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
                            imageVector = Icons.Rounded.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = stringResource(R.string.empty_bookings_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.empty_bookings_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(items = bookings, key = { it.id }) { booking ->
                BookingCard(
                    booking = booking,
                    isSubmitting = uiState.isSubmitting,
                    onConfirmPickup = { onConfirmPickup(booking) },
                    onReturnClick = { returnDialogBooking = booking },
                    onCancelClick = { onCancelBooking(booking) },
                )
            }
        }

        item(key = "bookings_bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    returnDialogBooking?.let { booking ->
        ReturnCheckInDialog(
            booking = booking,
            onReturnAvailable = {
                returnDialogBooking = null
                onCompleteReturn(booking, false, null)
            },
            onReturnMaintenance = { note ->
                returnDialogBooking = null
                onCompleteReturn(booking, true, note.ifBlank { "Post-rental inspection" })
            },
            onDismiss = { returnDialogBooking = null },
        )
    }
}

@Composable
private fun BookingStatBox(
    label: String,
    value: Int,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    isSubmitting: Boolean,
    onConfirmPickup: () -> Unit,
    onReturnClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("booking_card_${booking.bookingCode}"),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "#${booking.bookingCode}",
                        style = FleetMonoCodeStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LicensePlateBadge(plate = booking.licensePlate)
                }
                BookingStatusPill(status = booking.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = booking.vehicleName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = String.format(Locale.US, "$%.0f", booking.totalPrice),
                    style = FleetMonoPriceStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = booking.customerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (!booking.customerPhone.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = booking.customerPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${booking.pickupDate} → ${booking.returnDate} (${booking.days}d)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!booking.notes.isNullOrBlank()) {
                    Text(
                        text = "Note: ${booking.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.ACTIVE) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (booking.status == BookingStatus.PENDING) {
                        Button(
                            onClick = onConfirmPickup,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("confirm_pickup_${booking.bookingCode}"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VpnKey,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_confirm_pickup))
                        }
                    }

                    if (booking.status == BookingStatus.ACTIVE) {
                        Button(
                            onClick = onReturnClick,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("complete_return_${booking.bookingCode}"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AssignmentReturn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_complete_return))
                        }
                    }

                    OutlinedButton(
                        onClick = onCancelClick,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("cancel_booking_${booking.bookingCode}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_cancel_booking))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnCheckInDialog(
    booking: Booking,
    onReturnAvailable: () -> Unit,
    onReturnMaintenance: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.return_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${booking.vehicleName} (${booking.licensePlate}) returned by ${booking.customerName}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.return_dialog_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional Maintenance Note") },
                    placeholder = { Text("Only needed if flagging for service") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onReturnAvailable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("return_to_available_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.return_to_available))
                }
                OutlinedButton(
                    onClick = { onReturnMaintenance(note.trim()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("return_to_maintenance_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.return_to_maintenance))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
    )
}
