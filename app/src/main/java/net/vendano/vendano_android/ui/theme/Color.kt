package net.vendano.vendano_android.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Vendano palette — sourced 1-to-1 from iOS AppColors.xcassets ─────────────

object VendanoColors {
    // Accent: orange in light mode, sky-blue in dark mode
    val AccentLight      = Color(0xFFE85B00)
    val AccentDark       = Color(0xFF4FB2C8)

    // AccentAlt: amber in light, teal in dark
    val AccentAltLight   = Color(0xFFEC9A4E)
    val AccentAltDark    = Color(0xFF6BCDD3)

    // Background gradients
    val BackgroundStartLight = Color(0xFFFFF4EA)
    val BackgroundEndLight   = Color(0xFFFFD9B8)
    val BackgroundStartDark  = Color(0xFF113241)
    val BackgroundEndDark    = Color(0xFF060E1B)

    // Splash / launch background (orange in light, teal in dark)
    val BackgroundLaunchLight = Color(0xFFEC792E)
    val BackgroundLaunchDark  = Color(0xFF82BED0)

    // Cell / field surfaces
    val CellBackgroundLight  = Color(0xFFFFF3EA)
    val CellBackgroundDark   = Color(0xFF2F3E48)
    val FieldBackgroundLight = Color(0xFFFFFFFF)
    val FieldBackgroundDark  = Color(0xFF1E2A36)

    // Glow effects
    val GlowPinkLight   = Color(0xFFDD746A)
    val GlowPinkDark    = Color(0xFFFF4D9E)
    val GlowPurpleLight = Color(0xFFB88447)
    val GlowPurpleDark  = Color(0xFF9C7CFF)

    // Status
    val Positive = Color(0xFF22C55E)
    val Negative = Color(0xFFEF4444)

    // Text
    val TextPrimaryLight   = Color(0xFF1B1B1F)
    val TextPrimaryDark    = Color(0xFFE1E1E6)
    val TextSecondaryLight = Color(0xFF5F5F6B)
    val TextSecondaryDark  = Color(0xFF60818E)
    val TextReversedLight  = Color(0xFFFFFFFF)
    val TextReversedDark   = Color(0xFF1B1B1F)

    // ─── HOSKY theme ─────────────────────────────────────────────────────────
    val HoskyAccent          = Color(0xFF4FB2C8)
    val HoskyAccentAlt       = Color(0xFF5EF1F5)
    val HoskyBackgroundStart = Color(0xFF0D1B26)
    val HoskyBackgroundEnd   = Color(0xFF1A3A43)
    val HoskyGlowPink        = Color(0xFFFF45C5)
    val HoskyGlowPurple      = Color(0xFFBB86FC)
    val HoskyNegative        = Color(0xFFFF5A5F)
    val HoskyCellBackground  = Color(0xFF45255C)
    val HoskyTextReversed    = Color(0xFFF5F0FF)
}

// ─── Material3 colour schemes ─────────────────────────────────────────────────

val VendanoLightColorScheme = androidx.compose.material3.lightColorScheme(
    primary      = VendanoColors.AccentLight,
    secondary    = VendanoColors.AccentAltLight,
    background   = VendanoColors.BackgroundStartLight,
    surface      = VendanoColors.CellBackgroundLight,
    onPrimary    = VendanoColors.TextReversedLight,
    onBackground = VendanoColors.TextPrimaryLight,
    onSurface    = VendanoColors.TextPrimaryLight,
    error        = VendanoColors.Negative,
    onError      = VendanoColors.TextReversedLight,
)

val VendanoDarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary      = VendanoColors.AccentDark,
    secondary    = VendanoColors.AccentAltDark,
    background   = VendanoColors.BackgroundStartDark,
    surface      = VendanoColors.CellBackgroundDark,
    onPrimary    = VendanoColors.TextReversedDark,
    onBackground = VendanoColors.TextPrimaryDark,
    onSurface    = VendanoColors.TextPrimaryDark,
    error        = VendanoColors.Negative,
    onError      = VendanoColors.TextReversedLight,
)
