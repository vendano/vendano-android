package net.vendano.vendano_android.ui.component

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import net.vendano.vendano_android.ui.theme.VendanoColors

/**
 * Offline banner shown when network is unavailable.
 * Mirrors iOS OfflineBanner.swift.
 */
@Composable
fun OfflineBanner() {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val cm = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isConnected = true }
            override fun onLost(network: Network) { isConnected = false }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(request, callback)
        onDispose { cm?.unregisterNetworkCallback(callback) }
    }

    AnimatedVisibility(
        visible = !isConnected,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Text(
            text = "No internet connection",
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(VendanoColors.Negative)
                .padding(8.dp),
        )
    }
}
