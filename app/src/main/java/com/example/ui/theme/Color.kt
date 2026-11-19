package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Brand Palette — Precision Fleet Dispatch
val CobaltPrimary = Color(0xFF1D4ED8)
val CobaltOnPrimary = Color(0xFFFFFFFF)
val CobaltPrimaryContainer = Color(0xFFDBEAFE)
val CobaltOnPrimaryContainer = Color(0xFF0A2463)

val SlateSecondary = Color(0xFF334155)
val SlateOnSecondary = Color(0xFFFFFFFF)
val SlateSecondaryContainer = Color(0xFFE2E8F0)
val SlateOnSecondaryContainer = Color(0xFF0F172A)

val EmeraldTertiary = Color(0xFF059669)
val EmeraldOnTertiary = Color(0xFFFFFFFF)
val EmeraldTertiaryContainer = Color(0xFFD1FAE5)
val EmeraldOnTertiaryContainer = Color(0xFF064E3B)

val LightBackground = Color(0xFFF4F6FA)
val LightOnBackground = Color(0xFF0F172A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF0F172A)
val LightSurfaceVariant = Color(0xFFE8EDF5)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)

// Dark Theme Palette — Night Dispatch
val CobaltPrimaryDark = Color(0xFF60A5FA)
val CobaltOnPrimaryDark = Color(0xFF0A1D42)
val CobaltPrimaryContainerDark = Color(0xFF1E3A8A)
val CobaltOnPrimaryContainerDark = Color(0xFFDBEAFE)

val SlateSecondaryDark = Color(0xFF94A3B8)
val SlateOnSecondaryDark = Color(0xFF0F172A)
val SlateSecondaryContainerDark = Color(0xFF1E293B)
val SlateOnSecondaryContainerDark = Color(0xFFE2E8F0)

val EmeraldTertiaryDark = Color(0xFF34D399)
val EmeraldOnTertiaryDark = Color(0xFF022C22)
val EmeraldTertiaryContainerDark = Color(0xFF065F46)
val EmeraldOnTertiaryContainerDark = Color(0xFFD1FAE5)

val DarkBackground = Color(0xFF0B1120)
val DarkOnBackground = Color(0xFFF1F5F9)
val DarkSurface = Color(0xFF131C2E)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)
val DarkOutlineVariant = Color(0xFF1E293B)

val ErrorLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFECACA)

@Immutable
data class FleetStatusColors(
    val available: Color,
    val availableContainer: Color,
    val onAvailableContainer: Color,
    val onRent: Color,
    val onRentContainer: Color,
    val onOnRentContainer: Color,
    val maintenance: Color,
    val maintenanceContainer: Color,
    val onMaintenanceContainer: Color,
    val plateBackground: Color,
    val plateBorder: Color,
    val plateText: Color,
    val heroScrimStart: Color,
    val heroScrimEnd: Color,
)

val LightFleetStatusColors = FleetStatusColors(
    available = Color(0xFF059669),
    availableContainer = Color(0xFFECFDF5),
    onAvailableContainer = Color(0xFF065F46),
    onRent = Color(0xFFD97706),
    onRentContainer = Color(0xFFFEF3C7),
    onOnRentContainer = Color(0xFF92400E),
    maintenance = Color(0xFFDC2626),
    maintenanceContainer = Color(0xFFFEF2F2),
    onMaintenanceContainer = Color(0xFF991B1B),
    plateBackground = Color(0xFFF8FAFC),
    plateBorder = Color(0xFFCBD5E1),
    plateText = Color(0xFF0F172A),
    heroScrimStart = Color(0xCC0B1120),
    heroScrimEnd = Color(0x220B1120),
)

val DarkFleetStatusColors = FleetStatusColors(
    available = Color(0xFF34D399),
    availableContainer = Color(0xFF064E3B),
    onAvailableContainer = Color(0xFFA7F3D0),
    onRent = Color(0xFFFBBF24),
    onRentContainer = Color(0xFF451A03),
    onOnRentContainer = Color(0xFFFDE68A),
    maintenance = Color(0xFFF87171),
    maintenanceContainer = Color(0xFF450A0A),
    onMaintenanceContainer = Color(0xFFFECACA),
    plateBackground = Color(0xFF1E293B),
    plateBorder = Color(0xFF475569),
    plateText = Color(0xFFF1F5F9),
    heroScrimStart = Color(0xE60B1120),
    heroScrimEnd = Color(0x440B1120),
)

val LocalFleetStatusColors = staticCompositionLocalOf { LightFleetStatusColors }
