package net.vendano.vendano_android.ui.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.theme.VendanoColors

/**
 * Toast banner that appears at the bottom of the screen.
 * Mirrors iOS ToastBanner.swift.
 */
@Composable
fun ToastBanner(
    message: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier.zIndex(10f),
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 32.dp)
                    .background(
                        color = VendanoColors.BackgroundEndDark,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text = message ?: "",
                    color = VendanoColors.TextPrimaryDark,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
