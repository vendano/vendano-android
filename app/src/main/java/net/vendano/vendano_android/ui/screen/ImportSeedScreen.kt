package net.vendano.vendano_android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.vendano.vendano_android.domain.cardano.MnemonicHelper
import net.vendano.vendano_android.ui.component.LightGradientBackground
import net.vendano.vendano_android.ui.component.PrimaryButton
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.viewmodel.AppViewModel
import net.vendano.vendano_android.ui.viewmodel.OnboardingViewModel
import net.vendano.vendano_android.ui.viewmodel.WalletViewModel

/**
 * Import seed phrase screen.
 * Mirrors iOS ImportSeedView.swift.
 */
@Composable
fun ImportSeedScreen(
    appViewModel: AppViewModel,
    walletViewModel: WalletViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val theme = LocalVendanoTheme.current
    val input by vm.mnemonicInput.collectAsState()
    val words by vm.parsedWords.collectAsState()
    val valid by vm.mnemonicValid.collectAsState()
    val loading by vm.walletLoading.collectAsState()
    val error by vm.walletError.collectAsState()
    val env by appViewModel.environment.collectAsState()

    LightGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.accent)
                }
                Text("Import Wallet", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            }

            Text(
                "Paste your 12, 15, or 24-word seed phrase below. Separate words with spaces.",
                fontSize = 14.sp, color = theme.textSecondary,
            )

            OutlinedTextField(
                value = input,
                onValueChange = vm::setMnemonicInput,
                label = { Text("Seed phrase") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                maxLines = 8,
            )

            if (words.isNotEmpty()) {
                Text(
                    text = "${words.size} words detected",
                    color = if (valid) theme.positive else theme.textSecondary,
                    fontSize = 13.sp,
                )
            }

            if (error != null) {
                Text(error!!, color = theme.negative, fontSize = 13.sp)
                TextButton(onClick = vm::clearWalletError) { Text("Dismiss") }
            }

            PrimaryButton(
                text = if (loading) "Importing…" else "Import Wallet",
                onClick = {
                    vm.clearWalletError()
                    vm.importWallet(words, env) { address ->
                        appViewModel.setWalletAddress(address)
                        walletViewModel.configure(
                            walletAddress = address,
                            stakeAddress = "",
                            allAddresses = listOf(address),
                            env = env,
                        )
                        walletViewModel.refreshOnChainData()
                        onSuccess()
                    }
                },
                enabled = !loading && valid,
            )
        }
    }
}
