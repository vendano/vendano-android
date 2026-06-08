package net.vendano.vendano_android.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.vendano.vendano_android.R
import net.vendano.vendano_android.ui.component.DarkGradientBackground
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.theme.ThemePalette
import net.vendano.vendano_android.ui.viewmodel.AppViewModel

/**
 * Splash / loading screen.
 * Mirrors iOS SplashView.swift: dark gradient, pulsing logo, branded text.
 */
@Composable
fun SplashScreen(
    loading: Boolean,
    appViewModel: AppViewModel,
    onNext: (() -> Unit)? = null,
) {
    val theme = LocalVendanoTheme.current
    val isHosky = theme.currentPalette == ThemePalette.HOSKY

    // Pulsing scale animation — mirrors iOS scaleEffect(pulse ? 1.6 : 0.8)
    val pulseAnim = rememberInfiniteTransition(label = "logo_pulse")
    val logoScale by pulseAnim.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_scale",
    )

    val logoRes = if (isHosky) R.drawable.vendoggo_logo_dark else R.drawable.vendano_logo_dark
    val appName = if (isHosky) "vendoggo" else "vendano"

    DarkGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pulsing logo image – rendered as a white template (mirrors iOS)
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = appName,
                colorFilter = ColorFilter.tint(theme.textReversed),
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale),
            )

            Spacer(Modifier.height(24.dp))

            // App name
            Text(
                text = appName,
                color = theme.textReversed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 48.sp,
                letterSpacing = (-1).sp,
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle – mirrors L10n.SplashView.easyAdaTransfersByPhoneOrEmail
            Text(
                text = "Easy ADA transfers by phone or email",
                color = theme.textPrimary.copy(alpha = 0.85f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(64.dp))

            if (loading) {
                CircularProgressIndicator(color = theme.textReversed)
            } else if (onNext != null) {
                Button(
                    onClick = onNext,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.textReversed,
                        contentColor = theme.accent,
                    ),
                    modifier = Modifier.defaultMinSize(minWidth = 160.dp),
                ) {
                    Text(
                        text = "Get Started",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}
