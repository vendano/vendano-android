package net.vendano.vendano_android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import net.vendano.vendano_android.ui.component.LightGradientBackground
import net.vendano.vendano_android.ui.component.PrimaryButton
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.viewmodel.AppViewModel
import net.vendano.vendano_android.ui.viewmodel.OnboardingViewModel
import net.vendano.vendano_android.ui.viewmodel.WalletViewModel

/**
 * Confirm seed phrase screen: user must re-enter select words to prove backup.
 * Mirrors iOS ConfirmSeedView.swift.
 */
@Composable
fun ConfirmSeedScreen(
    appViewModel: AppViewModel,
    walletViewModel: WalletViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val theme = LocalVendanoTheme.current
    val canonicalWords by appViewModel.seedWords.collectAsState()
    val env by appViewModel.environment.collectAsState()
    val loading by vm.walletLoading.collectAsState()
    val walletError by vm.walletError.collectAsState()

    // Ask user to confirm words at positions 1, 7, 13, 19 (1-indexed)
    val checkPositions = remember(canonicalWords.size) {
        if (canonicalWords.size >= 20) listOf(0, 6, 12, 18) else (0 until canonicalWords.size).toList()
    }

    val userInputs = remember { mutableStateListOf(*Array(checkPositions.size) { "" }) }

    val allCorrect by derivedStateOf {
        checkPositions.indices.all { i ->
            userInputs[i].trim().lowercase() == canonicalWords.getOrNull(checkPositions[i])?.lowercase()
        }
    }

    LightGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.accent)
                }
                Text("Confirm Backup", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            }

            Text(
                "Enter the words at the requested positions to confirm you've backed up your seed phrase.",
                fontSize = 14.sp, color = theme.textSecondary,
            )

            checkPositions.forEachIndexed { i, wordIndex ->
                OutlinedTextField(
                    value = userInputs[i],
                    onValueChange = { userInputs[i] = it },
                    label = { Text("Word #${wordIndex + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = userInputs[i].isNotEmpty() &&
                        userInputs[i].trim().lowercase() != canonicalWords.getOrNull(wordIndex)?.lowercase(),
                )
            }

            if (walletError != null) {
                Text(walletError!!, color = theme.negative, fontSize = 13.sp)
            }

            PrimaryButton(
                text = if (loading) "Creating wallet…" else "Confirm & Create Wallet",
                onClick = {
                    vm.createWallet(canonicalWords, env) { address ->
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
                enabled = !loading && allCorrect,
            )
        }
    }
}
