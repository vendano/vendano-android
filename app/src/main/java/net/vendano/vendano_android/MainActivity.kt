package net.vendano.vendano_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.vendano.vendano_android.ui.component.OfflineBanner
import net.vendano.vendano_android.ui.navigation.VendanoNavGraph
import net.vendano.vendano_android.ui.theme.ThemePalette
import net.vendano.vendano_android.ui.theme.VendanoTheme
import net.vendano.vendano_android.ui.viewmodel.AppViewModel
import net.vendano.vendano_android.ui.viewmodel.WalletViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VendanoApp()
        }
    }
}

@Composable
private fun VendanoApp() {
    val appViewModel: AppViewModel = hiltViewModel()
    val walletViewModel: WalletViewModel = hiltViewModel()

    val appearance by appViewModel.appearancePreference.collectAsState()

    val palette = when (appearance) {
        "dark"  -> ThemePalette.DARK
        "hosky" -> ThemePalette.HOSKY
        else    -> ThemePalette.LIGHT
    }

    // Kick off bootstrap exactly once per process lifetime
    LaunchedEffect(Unit) {
        appViewModel.bootstrap(isDemo = false)
    }

    VendanoTheme(palette = palette) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            Column(modifier = Modifier.fillMaxSize()) {
                OfflineBanner()
                VendanoNavGraph(
                    navController = navController,
                    appViewModel = appViewModel,
                    walletViewModel = walletViewModel,
                )
            }
        }
    }
}
