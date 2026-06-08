package net.vendano.vendano_android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme

/**
 * Dark gradient background.
 * Mirrors iOS DarkGradientView:
 *   1. Solid BackgroundStart fill
 *   2. Accent colour linear overlay (0.9 → 0.7 opacity, top-leading → bottom-trailing)
 *
 * In Vendano light mode this produces the warm orange gradient the design calls for.
 */
@Composable
fun DarkGradientBackground(content: @Composable () -> Unit) {
    val theme = LocalVendanoTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundStart)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        theme.accent.copy(alpha = 0.9f),
                        theme.accent.copy(alpha = 0.7f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            ),
    ) { content() }
}

/**
 * Light gradient background.
 * Mirrors iOS LightGradientView:
 *   Vertical gradient BackgroundStart → BackgroundEnd.
 */
@Composable
fun LightGradientBackground(content: @Composable () -> Unit) {
    val theme = LocalVendanoTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundGradient),
    ) { content() }
}
