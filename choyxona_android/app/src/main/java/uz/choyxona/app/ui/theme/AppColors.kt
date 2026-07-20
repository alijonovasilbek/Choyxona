package uz.choyxona.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color palette. Every screen reads colors through this (via the
 * legacy top-level accessors in Color.kt or directly), so switching
 * light/dark re-skins the whole app.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val primaryContainer: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnPrimary: Color,
    val divider: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val statusPending: Color,
    val statusSuccessful: Color,
    val statusCancelled: Color,
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEEF2F7),
    surfaceElevated = Color(0xFFFFFFFF),
    primary = Color(0xFF0FA968),
    primaryDark = Color(0xFF0B8A55),
    primaryLight = Color(0xFF5ED4A4),
    primaryContainer = Color(0xFFDDF5EA),
    accent = Color(0xFFF59E0B),
    textPrimary = Color(0xFF0F1728),
    textSecondary = Color(0xFF5B6472),
    textTertiary = Color(0xFF98A2B3),
    textOnPrimary = Color(0xFFFFFFFF),
    divider = Color(0xFFE7EBF0),
    border = Color(0xFFE3E8EF),
    success = Color(0xFF12B76A),
    warning = Color(0xFFF79009),
    error = Color(0xFFF04438),
    info = Color(0xFF2E90FA),
    statusPending = Color(0xFFF79009),
    statusSuccessful = Color(0xFF12B76A),
    statusCancelled = Color(0xFFF04438),
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF0C111D),
    surface = Color(0xFF161B26),
    surfaceAlt = Color(0xFF1F242F),
    surfaceElevated = Color(0xFF1C222E),
    primary = Color(0xFF3CCB7F),
    primaryDark = Color(0xFF2AA867),
    primaryLight = Color(0xFF74E3AC),
    primaryContainer = Color(0xFF12362A),
    accent = Color(0xFFFDB022),
    textPrimary = Color(0xFFF0F2F5),
    textSecondary = Color(0xFF98A2B3),
    textTertiary = Color(0xFF667085),
    textOnPrimary = Color(0xFF06251A),
    divider = Color(0xFF29303D),
    border = Color(0xFF2C3340),
    success = Color(0xFF32D583),
    warning = Color(0xFFFDB022),
    error = Color(0xFFF97066),
    info = Color(0xFF53B1FD),
    statusPending = Color(0xFFFDB022),
    statusSuccessful = Color(0xFF32D583),
    statusCancelled = Color(0xFFF97066),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
