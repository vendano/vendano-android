package net.vendano.vendano_android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─── Palette selection ────────────────────────────────────────────────────────

enum class ThemePalette { LIGHT, DARK, HOSKY }

@Stable
class VendanoThemeState(palette: ThemePalette = ThemePalette.DARK) {
    var currentPalette by mutableStateOf(palette)

    val accent: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.AccentLight
            ThemePalette.DARK  -> VendanoColors.AccentDark
            ThemePalette.HOSKY -> VendanoColors.HoskyAccent
        }

    val accentAlt: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.AccentAltLight
            ThemePalette.DARK  -> VendanoColors.AccentAltDark
            ThemePalette.HOSKY -> VendanoColors.HoskyAccentAlt
        }

    val backgroundStart: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.BackgroundStartLight
            ThemePalette.DARK  -> VendanoColors.BackgroundStartDark
            ThemePalette.HOSKY -> VendanoColors.HoskyBackgroundStart
        }

    val backgroundEnd: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.BackgroundEndLight
            ThemePalette.DARK  -> VendanoColors.BackgroundEndDark
            ThemePalette.HOSKY -> VendanoColors.HoskyBackgroundEnd
        }

    val backgroundLaunch: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.BackgroundLaunchLight
            ThemePalette.DARK  -> VendanoColors.BackgroundLaunchDark
            ThemePalette.HOSKY -> VendanoColors.HoskyBackgroundStart
        }

    val cellBackground: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.CellBackgroundLight
            ThemePalette.DARK  -> VendanoColors.CellBackgroundDark
            ThemePalette.HOSKY -> VendanoColors.HoskyCellBackground
        }

    val fieldBackground: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.FieldBackgroundLight
            else               -> VendanoColors.FieldBackgroundDark
        }

    val textPrimary: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.TextPrimaryLight
            else               -> VendanoColors.TextPrimaryDark
        }

    val textSecondary: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.TextSecondaryLight
            else               -> VendanoColors.TextSecondaryDark
        }

    val textReversed: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.TextReversedLight
            ThemePalette.DARK  -> VendanoColors.TextReversedLight
            ThemePalette.HOSKY -> VendanoColors.HoskyTextReversed
        }

    val positive: Color
        get() = when (currentPalette) {
            ThemePalette.HOSKY -> VendanoColors.HoskyAccent
            else               -> VendanoColors.Positive
        }

    val negative: Color
        get() = when (currentPalette) {
            ThemePalette.HOSKY -> VendanoColors.HoskyNegative
            else               -> VendanoColors.Negative
        }

    val glowStart: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.GlowPurpleLight
            ThemePalette.DARK  -> VendanoColors.GlowPurpleDark
            ThemePalette.HOSKY -> VendanoColors.HoskyGlowPurple
        }

    val glowEnd: Color
        get() = when (currentPalette) {
            ThemePalette.LIGHT -> VendanoColors.GlowPinkLight
            ThemePalette.DARK  -> VendanoColors.GlowPinkDark
            ThemePalette.HOSKY -> VendanoColors.HoskyGlowPink
        }

    val isDark: Boolean get() = currentPalette != ThemePalette.LIGHT

    val backgroundGradient: Brush
        get() = Brush.verticalGradient(listOf(backgroundStart, backgroundEnd))
}

val LocalVendanoTheme = staticCompositionLocalOf { VendanoThemeState() }

@Composable
fun VendanoTheme(
    palette: ThemePalette? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val resolvedPalette = palette ?: if (systemDark) ThemePalette.DARK else ThemePalette.LIGHT

    val themeState = remember { VendanoThemeState(resolvedPalette) }
    SideEffect { themeState.currentPalette = resolvedPalette }

    val colorScheme = if (resolvedPalette == ThemePalette.LIGHT) VendanoLightColorScheme
                      else VendanoDarkColorScheme

    val typography = if (resolvedPalette == ThemePalette.HOSKY) HoskyTypography else VendanoTypography

    CompositionLocalProvider(LocalVendanoTheme provides themeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
