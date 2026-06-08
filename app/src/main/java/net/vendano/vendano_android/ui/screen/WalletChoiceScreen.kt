package net.vendano.vendano_android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.vendano.vendano_android.ui.component.LightGradientBackground
import net.vendano.vendano_android.ui.component.PrimaryButton
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme

/**
 * Wallet choice screen: create a new wallet or import an existing one.
 * Mirrors iOS WalletChoiceView.swift.
 */
@Composable
fun WalletChoiceScreen(
    onCreateNew: () -> Unit,
    onImport: () -> Unit,
) {
    val theme = LocalVendanoTheme.current

    LightGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Your Wallet", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Create a new Cardano wallet or import an existing 24-word seed phrase.",
                fontSize = 15.sp, color = theme.textSecondary,
            )

            Spacer(Modifier.height(48.dp))

            PrimaryButton(text = "Create New Wallet", onClick = onCreateNew)
            Spacer(Modifier.height(16.dp))

            // Outlined / secondary style for import
            androidx.compose.material3.OutlinedButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
            ) {
                Text("Import Existing Wallet", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
