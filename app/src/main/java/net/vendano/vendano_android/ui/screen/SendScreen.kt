package net.vendano.vendano_android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.vendano.vendano_android.domain.model.SendMethod
import net.vendano.vendano_android.ui.component.LightGradientBackground
import net.vendano.vendano_android.ui.component.PrimaryButton
import net.vendano.vendano_android.ui.component.ToastBanner
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.viewmodel.*

/**
 * Send ADA screen.
 * Mirrors iOS SendView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    appViewModel: AppViewModel,
    walletViewModel: WalletViewModel,
    sendViewModel: SendViewModel,
    onClose: () -> Unit,
) {
    val theme = LocalVendanoTheme.current

    val method by sendViewModel.sendMethod.collectAsState()
    val dialCode by sendViewModel.dialCode.collectAsState()
    val localNumber by sendViewModel.localNumber.collectAsState()
    val emailInput by sendViewModel.emailInput.collectAsState()
    val addressInput by sendViewModel.addressInput.collectAsState()
    val adaText by sendViewModel.adaText.collectAsState()
    val tipText by sendViewModel.tipText.collectAsState()
    val recipient by sendViewModel.recipient.collectAsState()
    val networkFee by sendViewModel.networkFee.collectAsState()
    val feeLoading by sendViewModel.feeLoading.collectAsState()
    val feeError by sendViewModel.feeError.collectAsState()
    val sending by sendViewModel.isSending.collectAsState()
    val spendableNullable by walletViewModel.spendableAda.collectAsState()
    val maxAda = spendableNullable ?: walletViewModel.adaBalance.value
    val fiatRate by walletViewModel.adaFiatRate.collectAsState()
    val fiatCurrency by walletViewModel.fiatCurrency.collectAsState()
    val env by appViewModel.environment.collectAsState()

    // Collect send events from SharedFlow
    val sendEventState = remember { mutableStateOf<SendViewModel.SendUiEvent?>(null) }
    LaunchedEffect(sendViewModel) {
        sendViewModel.sendEvent.collect { event ->
            sendEventState.value = event
        }
    }
    LaunchedEffect(sendEventState.value) {
        when (val e = sendEventState.value) {
            is SendViewModel.SendUiEvent.Success -> {
                appViewModel.showToast("Transaction submitted!")
                sendEventState.value = null
                onClose()
            }
            is SendViewModel.SendUiEvent.Error -> {
                appViewModel.showToast("Error: ${e.message}")
                sendEventState.value = null
            }
            null -> Unit
        }
    }

    val tabs = listOf("Phone", "Email", "Address")
    val selectedTab = when (method) {
        SendMethod.PHONE   -> 0
        SendMethod.EMAIL   -> 1
        SendMethod.ADDRESS -> 2
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LightGradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Send ADA", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = theme.textSecondary)
                    }
                }

                // Max display
                Text(
                    "Available: ₳ ${String.format("%.2f", maxAda)}",
                    fontSize = 13.sp, color = theme.textSecondary,
                )

                // Send-to method tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = theme.fieldBackground,
                    contentColor = theme.accent,
                    indicator = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(3.dp)
                        )
                    },
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                sendViewModel.setSendMethod(
                                    when (index) {
                                        0 -> SendMethod.PHONE
                                        1 -> SendMethod.EMAIL
                                        else -> SendMethod.ADDRESS
                                    }
                                )
                            },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedTab == index) theme.accent else theme.textSecondary,
                                )
                            }
                        )
                    }
                }

                // Recipient input
                when (method) {
                    SendMethod.PHONE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dialCode,
                            onValueChange = sendViewModel::setDialCode,
                            label = { Text("Code") },
                            modifier = Modifier.width(90.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = localNumber,
                            onValueChange = sendViewModel::setLocalNumber,
                            label = { Text("Number") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                        )
                    }
                    SendMethod.EMAIL -> OutlinedTextField(
                        value = emailInput,
                        onValueChange = sendViewModel::setEmail,
                        label = { Text("Email address") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                    )
                    SendMethod.ADDRESS -> OutlinedTextField(
                        value = addressInput,
                        onValueChange = sendViewModel::setAddress,
                        label = { Text("Cardano address or \$handle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                // Resolved recipient indicator
                if (recipient != null) {
                    Text(
                        text = "→ ${recipient!!.name.ifBlank { recipient!!.address.take(20) }}…",
                        fontSize = 13.sp, color = theme.positive,
                    )
                }

                // Amount
                OutlinedTextField(
                    value = adaText,
                    onValueChange = sendViewModel::setAdaText,
                    label = { Text("Amount (ADA)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    trailingIcon = {
                        if (maxAda > 0.0) {
                            TextButton(onClick = { sendViewModel.setAdaText(String.format("%.2f", maxAda)) }) {
                                Text("MAX", fontSize = 11.sp, color = theme.accent)
                            }
                        }
                    }
                )

                // Optional tip
                OutlinedTextField(
                    value = tipText,
                    onValueChange = sendViewModel::setTipText,
                    label = { Text("Developer tip (ADA) – optional") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )

                // Fee estimation: recalc when amount/recipient change
                LaunchedEffect(adaText, recipient, tipText) {
                    sendViewModel.recalcFee(walletViewModel)
                }

                // Fee display
                if (feeLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = theme.accent, strokeWidth = 2.dp)
                        Text("Estimating fee…", fontSize = 13.sp, color = theme.textSecondary)
                    }
                } else if (networkFee > 0.0) {
                    Text(
                        "Network fee: ₳ ${String.format("%.4f", networkFee)}",
                        fontSize = 13.sp, color = theme.textSecondary,
                    )
                }

                if (feeError != null) {
                    Text(feeError!!, color = theme.negative, fontSize = 13.sp)
                }

                val adaDouble = adaText.toDoubleOrNull() ?: 0.0

                PrimaryButton(
                    text = if (sending) "Sending…" else "Send ₳ ${String.format("%.2f", adaDouble)}",
                    onClick = {
                        sendViewModel.send(env) {
                            walletViewModel.refreshOnChainData()
                        }
                    },
                    enabled = !sending && adaDouble > 0.0 && (recipient != null || addressInput.startsWith("addr")),
                )

                Spacer(Modifier.height(32.dp))
            }
        }

        // Toast overlay
        val toastMsg by appViewModel.toastMessage.collectAsState()
        ToastBanner(
            message = toastMsg,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
