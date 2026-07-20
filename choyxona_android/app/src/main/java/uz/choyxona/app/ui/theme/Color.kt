package uz.choyxona.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Legacy color names, now theme-aware: they resolve against the active
 * [AppColors] palette, so every screen that imports them follows
 * light/dark mode automatically.
 */

val PrimaryGreen: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.primary

val PrimaryGreenDark: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryDark

val PrimaryGreenLight: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryLight

val SecondaryGreen: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryContainer

val BackgroundLight: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.background

val BackgroundWhite: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface

val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.textPrimary

val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSecondary

val TextTertiary: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.textTertiary

val GlassWhite: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface

val GlassSurface: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceAlt

val StatusPending: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusPending

val StatusSuccessful: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusSuccessful

val StatusCancelled: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.statusCancelled

val DividerColor: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.divider

val ErrorRed: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.error

val WarningOrange: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.warning

val CardBorderGreen: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.primary.copy(alpha = 0.35f)

val CardBorderLight: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.border
