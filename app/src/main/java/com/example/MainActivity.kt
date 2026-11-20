package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.SupabaseSchemaSql
import com.example.data.model.Vehicle
import com.example.ui.FleetTab
import com.example.ui.FleetViewModel
import com.example.ui.components.LiveSupabaseBadge
import com.example.ui.components.SupabaseInspectorSheet
import com.example.ui.components.copyTextToClipboard
import com.example.ui.screens.AddEditVehicleSheet
import com.example.ui.screens.BookingsScreen
import com.example.ui.screens.CustomerDashboardScreen
import com.example.ui.screens.IdleListScreen
import com.example.ui.screens.ReservationSheet
import com.example.ui.theme.IdleFleetTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IdleFleetTheme {
                IdleFleetApp()
            }
        }
    }
}

private data class NavDestination(
    val tab: FleetTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val FleetDestinations = listOf(
    NavDestination(
        tab = FleetTab.IDLE_LIST,
        labelRes = R.string.nav_idle_list,
        selectedIcon = Icons.Rounded.DirectionsCar,
        unselectedIcon = Icons.Outlined.DirectionsCar,
        testTag = "nav_tab_idle_list",
    ),
    NavDestination(
        tab = FleetTab.CUSTOMER_DASHBOARD,
        labelRes = R.string.nav_customer,
        selectedIcon = Icons.Rounded.Storefront,
        unselectedIcon = Icons.Outlined.Storefront,
        testTag = "nav_tab_customer",
    ),
    NavDestination(
        tab = FleetTab.BOOKINGS,
        labelRes = R.string.nav_bookings,
        selectedIcon = Icons.Rounded.EventNote,
        unselectedIcon = Icons.Outlined.EventNote,
        testTag = "nav_tab_bookings",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdleFleetApp(
    viewModel: FleetViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by remember { mutableStateOf(FleetTab.IDLE_LIST) }
    var showInspectorSheet by remember { mutableStateOf(false) }
    var showAddVehicleSheet by remember { mutableStateOf(false) }
    var editingVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var reservingVehicle by remember { mutableStateOf<Vehicle?>(null) }

    // Return to primary Idle List tab on system back when viewing secondary tabs
    BackHandler(enabled = activeTab != FleetTab.IDLE_LIST) {
        activeTab = FleetTab.IDLE_LIST
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val sqlCopiedText = stringResource(R.string.sql_copied_toast)
    val onCopySql: () -> Unit = {
        copyTextToClipboard(context, "Idle Fleet Supabase SQL", SupabaseSchemaSql.setupScript)
        scope.launch { snackbarHostState.showSnackbar(sqlCopiedText) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    Spacer(modifier = Modifier.weight(0.15f))
                    FleetDestinations.forEach { dest ->
                        val selected = activeTab == dest.tab
                        val badgeCount = when (dest.tab) {
                            FleetTab.IDLE_LIST -> uiState.availableCount
                            FleetTab.BOOKINGS -> uiState.activeBookingsCount + uiState.pendingBookingsCount
                            else -> 0
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = { activeTab = dest.tab },
                            icon = {
                                NavItemIcon(
                                    icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    label = stringResource(dest.labelRes),
                                    badgeCount = badgeCount,
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            modifier = Modifier.testTag(dest.testTag),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                        title = {
                            Column {
                                Text(
                                    text = when (activeTab) {
                                        FleetTab.IDLE_LIST -> stringResource(R.string.idle_list_title)
                                        FleetTab.CUSTOMER_DASHBOARD -> stringResource(R.string.app_name)
                                        FleetTab.BOOKINGS -> stringResource(R.string.bookings_title)
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Text(
                                    text = when (activeTab) {
                                        FleetTab.IDLE_LIST -> stringResource(R.string.idle_list_subtitle)
                                        FleetTab.CUSTOMER_DASHBOARD -> "Customer Reservation Dashboard"
                                        FleetTab.BOOKINGS -> stringResource(R.string.bookings_subtitle)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        actions = {
                            LiveSupabaseBadge(
                                health = uiState.connectionHealth,
                                onClick = { showInspectorSheet = true },
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.refreshData(showLoader = true) },
                                modifier = Modifier.testTag("top_bar_refresh_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.cd_refresh_fleet),
                                )
                            }
                        },
                    )
                },
                floatingActionButton = {
                    if (activeTab == FleetTab.IDLE_LIST) {
                        ExtendedFloatingActionButton(
                            onClick = { showAddVehicleSheet = true },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.cd_add_vehicle),
                                )
                            },
                            text = { Text(stringResource(R.string.action_add_vehicle)) },
                            modifier = Modifier.testTag("add_vehicle_fab"),
                        )
                    }
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars,
                        ) {
                            FleetDestinations.forEach { dest ->
                                val selected = activeTab == dest.tab
                                val badgeCount = when (dest.tab) {
                                    FleetTab.IDLE_LIST -> uiState.availableCount
                                    FleetTab.BOOKINGS -> uiState.activeBookingsCount + uiState.pendingBookingsCount
                                    else -> 0
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { activeTab = dest.tab },
                                    icon = {
                                        NavItemIcon(
                                            icon = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            label = stringResource(dest.labelRes),
                                            badgeCount = badgeCount,
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val contentModifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp)

                    when (activeTab) {
                        FleetTab.IDLE_LIST -> {
                            IdleListScreen(
                                uiState = uiState,
                                onSelectStatusFilter = viewModel::setStatusFilter,
                                onSearchQueryChange = viewModel::setSearchQuery,
                                onReturnVehicle = { vehicle, date, reason, remarks ->
                                    viewModel.returnVehicle(
                                        vehicle = vehicle,
                                        returnDate = date,
                                        reason = reason,
                                        remarks = remarks,
                                    ) {}
                                },
                                onReactivateVehicle = viewModel::reactivateVehicle,
                                onQuickStatusChange = { vehicle, newStatus, note ->
                                    viewModel.quickUpdateVehicleStatus(
                                        vehicle = vehicle,
                                        newStatus = newStatus,
                                        maintenanceNote = note,
                                    )
                                },
                                onEditVehicle = { editingVehicle = it },
                                onAddVehicleClick = { showAddVehicleSheet = true },
                                onSeedFleetClick = viewModel::seedStarterFleet,
                                onOpenInspector = { showInspectorSheet = true },
                                onRetrySync = { viewModel.refreshData(showLoader = true) },
                                onCopySql = onCopySql,
                                modifier = contentModifier,
                            )
                        }

                        FleetTab.CUSTOMER_DASHBOARD -> {
                            CustomerDashboardScreen(
                                uiState = uiState,
                                onSelectCategory = viewModel::setCategoryFilter,
                                onTripDaysChange = viewModel::setCustomerTripDays,
                                onToggleShowBusy = viewModel::setCustomerShowBusy,
                                onReserveClick = { reservingVehicle = it },
                                onOpenInspector = { showInspectorSheet = true },
                                onRetrySync = { viewModel.refreshData(showLoader = true) },
                                onCopySql = onCopySql,
                                modifier = contentModifier,
                            )
                        }

                        FleetTab.BOOKINGS -> {
                            BookingsScreen(
                                uiState = uiState,
                                onSelectBookingFilter = viewModel::setBookingFilter,
                                onConfirmPickup = viewModel::confirmPickup,
                                onCompleteReturn = viewModel::completeReturn,
                                onCancelBooking = viewModel::cancelBooking,
                                onOpenInspector = { showInspectorSheet = true },
                                onRetrySync = { viewModel.refreshData(showLoader = true) },
                                onCopySql = onCopySql,
                                modifier = contentModifier,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInspectorSheet) {
        SupabaseInspectorSheet(
            health = uiState.connectionHealth,
            realtimeState = uiState.realtimeState,
            lastSyncedAt = uiState.lastSyncedAt,
            vehicleCount = uiState.vehicles.size,
            isSubmitting = uiState.isSubmitting,
            onSyncNow = {
                viewModel.refreshData(showLoader = true)
            },
            onSeedFleet = {
                viewModel.seedStarterFleet()
                showInspectorSheet = false
            },
            onSqlCopied = {
                scope.launch { snackbarHostState.showSnackbar(sqlCopiedText) }
            },
            onDismiss = { showInspectorSheet = false },
        )
    }

    if (showAddVehicleSheet || editingVehicle != null) {
        AddEditVehicleSheet(
            existingVehicle = editingVehicle,
            isSubmitting = uiState.isSubmitting,
            onSave = { request ->
                viewModel.saveVehicle(
                    existingId = editingVehicle?.id,
                    request = request,
                    onSuccess = {
                        showAddVehicleSheet = false
                        editingVehicle = null
                    },
                )
            },
            onDelete = editingVehicle?.let { vehicleToDelete ->
                {
                    viewModel.deleteVehicle(vehicleToDelete) {
                        editingVehicle = null
                    }
                }
            },
            onDismiss = {
                showAddVehicleSheet = false
                editingVehicle = null
            },
        )
    }

    reservingVehicle?.let { vehicle ->
        ReservationSheet(
            vehicle = vehicle,
            initialDays = uiState.customerTripDays,
            isSubmitting = uiState.isSubmitting,
            onConfirmReservation = { name, phone, email, pickup, ret, days, notes, immediate ->
                viewModel.createReservation(
                    vehicle = vehicle,
                    customerName = name,
                    customerPhone = phone,
                    customerEmail = email,
                    pickupDate = pickup,
                    returnDate = ret,
                    days = days,
                    notes = notes,
                    immediatePickup = immediate,
                    onSuccess = {
                        reservingVehicle = null
                        activeTab = FleetTab.BOOKINGS
                    },
                )
            },
            onDismiss = { reservingVehicle = null },
        )
    }
}

@Composable
private fun NavItemIcon(
    icon: ImageVector,
    label: String,
    badgeCount: Int,
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(text = badgeCount.toString())
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = label)
    }
}
