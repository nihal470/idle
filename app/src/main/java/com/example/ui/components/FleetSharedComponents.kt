package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BookingStatus
import com.example.data.model.ConnectionHealth
import com.example.data.model.SupabaseSchemaSql
import com.example.data.model.Vehicle
import com.example.data.model.VehicleStatus
import com.example.data.remote.RealtimeConnectionState
import com.example.data.remote.SupabaseConfig
import com.example.ui.theme.FleetMonoCodeStyle
import com.example.ui.theme.IdleFleetTheme

@Composable
fun VehicleStatusPill(
    status: VehicleStatus,
    modifier: Modifier = Modifier,
) {
    val statusColors = IdleFleetTheme.statusColors
    val (bg, fg, dot) = when (status) {
        VehicleStatus.AVAILABLE -> Triple(
            statusColors.availableContainer,
            statusColors.onAvailableContainer,
            statusColors.available,
        )
        VehicleStatus.ON_RENT -> Triple(
            statusColors.onRentContainer,
            statusColors.onOnRentContainer,
            statusColors.onRent,
        )
        VehicleStatus.MAINTENANCE -> Triple(
            statusColors.maintenanceContainer,
            statusColors.onMaintenanceContainer,
            statusColors.maintenance,
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
            Text(
                text = status.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun BookingStatusPill(
    status: BookingStatus,
    modifier: Modifier = Modifier,
) {
    val statusColors = IdleFleetTheme.statusColors
    val (bg, fg) = when (status) {
        BookingStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        BookingStatus.ACTIVE -> statusColors.onRentContainer to statusColors.onOnRentContainer
        BookingStatus.COMPLETED -> statusColors.availableContainer to statusColors.onAvailableContainer
        BookingStatus.CANCELLED -> statusColors.maintenanceContainer to statusColors.onMaintenanceContainer
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier,
    ) {
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun LicensePlateBadge(
    plate: String,
    modifier: Modifier = Modifier,
) {
    val colors = IdleFleetTheme.statusColors
    Surface(
        color = colors.plateBackground,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, colors.plateBorder),
        modifier = modifier,
    ) {
        Text(
            text = plate.uppercase(),
            style = FleetMonoCodeStyle,
            color = colors.plateText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun VehicleThumbnail(
    vehicle: Vehicle,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (!vehicle.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = vehicle.imageUrl,
                contentDescription = vehicle.displayName,
                contentScale = ContentScale.Crop,
                fallback = painterResource(id = vehicle.category.fallbackDrawable),
                error = painterResource(id = vehicle.category.fallbackDrawable),
                placeholder = painterResource(id = vehicle.category.fallbackDrawable),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(id = vehicle.category.fallbackDrawable),
                contentDescription = vehicle.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun LiveSupabaseBadge(
    health: ConnectionHealth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColors = IdleFleetTheme.statusColors
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    val (label, dotColor, bgColor, textColor, isPulsing) = when (health) {
        ConnectionHealth.LiveRealtime -> Quint(
            stringResource(R.string.connection_live),
            statusColors.available,
            statusColors.availableContainer,
            statusColors.onAvailableContainer,
            true,
        )
        ConnectionHealth.ConnectedPolling -> Quint(
            stringResource(R.string.connection_polling),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            false,
        )
        ConnectionHealth.Connecting -> Quint(
            stringResource(R.string.connection_connecting),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            true,
        )
        ConnectionHealth.MissingApiKey -> Quint(
            stringResource(R.string.connection_key_needed),
            statusColors.onRent,
            statusColors.onRentContainer,
            statusColors.onOnRentContainer,
            false,
        )
        is ConnectionHealth.SchemaMissing -> Quint(
            stringResource(R.string.connection_schema_needed),
            statusColors.onRent,
            statusColors.onRentContainer,
            statusColors.onOnRentContainer,
            false,
        )
        is ConnectionHealth.Error -> Quint(
            stringResource(R.string.connection_error),
            statusColors.maintenance,
            statusColors.maintenanceContainer,
            statusColors.onMaintenanceContainer,
            false,
        )
    }

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier.testTag("supabase_status_badge"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(if (isPulsing) pulseAlpha else 1f)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class Quint<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E,
)

@Composable
fun SupabaseConnectionBanner(
    health: ConnectionHealth,
    onOpenInspector: () -> Unit,
    onRetry: () -> Unit,
    onCopySql: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColors = IdleFleetTheme.statusColors
    when (health) {
        ConnectionHealth.MissingApiKey -> {
            Surface(
                color = statusColors.onRentContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, statusColors.onRent.copy(alpha = 0.35f)),
                modifier = modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = statusColors.onOnRentContainer,
                        )
                        Text(
                            text = stringResource(R.string.banner_missing_key_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = statusColors.onOnRentContainer,
                        )
                    }
                    Text(
                        text = stringResource(R.string.banner_missing_key_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors.onOnRentContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenInspector,
                            modifier = Modifier.testTag("open_supabase_setup_button"),
                        ) {
                            Text(text = stringResource(R.string.sheet_supabase_title))
                        }
                    }
                }
            }
        }
        is ConnectionHealth.SchemaMissing -> {
            Surface(
                color = statusColors.onRentContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, statusColors.onRent.copy(alpha = 0.35f)),
                modifier = modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = statusColors.onOnRentContainer,
                        )
                        Text(
                            text = stringResource(R.string.banner_schema_missing_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = statusColors.onOnRentContainer,
                        )
                    }
                    Text(
                        text = health.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColors.onOnRentContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onCopySql,
                            modifier = Modifier.testTag("banner_copy_sql_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.action_copy_sql))
                        }
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag("banner_retry_sync_button"),
                        ) {
                            Text(text = stringResource(R.string.action_retry_sync))
                        }
                    }
                }
            }
        }
        is ConnectionHealth.Error -> {
            Surface(
                color = statusColors.maintenanceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, statusColors.maintenance.copy(alpha = 0.35f)),
                modifier = modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = statusColors.onMaintenanceContainer,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Supabase Sync Issue",
                            style = MaterialTheme.typography.titleSmall,
                            color = statusColors.onMaintenanceContainer,
                        )
                        Text(
                            text = health.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColors.onMaintenanceContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier.testTag("banner_error_retry_button"),
                    ) {
                        Text(text = stringResource(R.string.action_retry_sync))
                    }
                }
            }
        }
        else -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseInspectorSheet(
    health: ConnectionHealth,
    realtimeState: RealtimeConnectionState,
    lastSyncedAt: String?,
    vehicleCount: Int,
    isSubmitting: Boolean,
    onSyncNow: () -> Unit,
    onSeedFleet: () -> Unit,
    onSqlCopied: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = if (health is ConnectionHealth.LiveRealtime || health is ConnectionHealth.ConnectedPolling) {
                        Icons.Rounded.CloudDone
                    } else {
                        Icons.Rounded.CloudOff
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = stringResource(R.string.sheet_supabase_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Project: ${SupabaseConfig.projectName} (${SupabaseConfig.projectId})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InfoRow("Endpoint", SupabaseConfig.projectUrl)
                    InfoRow(
                        "API Key (Secrets)",
                        if (SupabaseConfig.isApiKeyConfigured) "Configured via BuildConfig" else "Missing — Add SUPABASE_PUBLISHABLE_KEY in Secrets",
                    )
                    InfoRow("Realtime Socket", realtimeState.name)
                    InfoRow("Last Synced", lastSyncedAt ?: "Not synced yet")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inspector_sync_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.action_retry_sync))
                }

                OutlinedButton(
                    onClick = {
                        copyTextToClipboard(context, "Idle Fleet SQL", SupabaseSchemaSql.setupScript)
                        onSqlCopied()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inspector_copy_sql_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = stringResource(R.string.action_copy_sql))
                }
            }

            if (SupabaseConfig.isApiKeyConfigured &&
                vehicleCount == 0 &&
                (health is ConnectionHealth.LiveRealtime || health is ConnectionHealth.ConnectedPolling)
            ) {
                OutlinedButton(
                    onClick = onSeedFleet,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inspector_seed_fleet_button"),
                ) {
                    Text(text = stringResource(R.string.action_seed_fleet))
                }
            }

            HorizontalDivider()

            Text(
                text = "Supabase SQL Schema (vehicles + bookings + Realtime)",
                style = MaterialTheme.typography.titleSmall,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
            ) {
                Text(
                    text = SupabaseSchemaSql.setupScript,
                    style = FleetMonoCodeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}
