package net.vendano.vendano_android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.vendano.vendano_android.ui.component.LightGradientBackground
import net.vendano.vendano_android.ui.component.PrimaryButton
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.viewmodel.AppViewModel
import net.vendano.vendano_android.ui.viewmodel.OnboardingViewModel

/**
 * OTP/SMS verification code screen.
 * Mirrors iOS OTPView.swift.
 */
@Composable
fun OtpScreen(
    appViewModel: AppViewModel,
    onSuccess: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val theme = LocalVendanoTheme.current
    val otpCode by vm.otpCode.collectAsState()
    val loading by vm.authLoading.collectAsState()
    val error by vm.authError.collectAsState()
    val phone by appViewModel.otpPhone.collectAsState()

    LightGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Enter Code", fontSize = 28.sp, color = theme.textPrimary)
            Text(
                "We sent a 6-digit code to ${phone ?: "your phone"}.",
                fontSize = 15.sp, color = theme.textSecondary,
            )

            OutlinedTextField(
                value = otpCode,
                onValueChange = vm::setOtpCode,
                label = { Text("6-digit code") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            PrimaryButton(
                text = if (loading) "Verifying…" else "Verify",
                onClick = {
                    vm.confirmOtp(phone ?: "") { confirmedPhone ->
                        appViewModel.onPhoneConfirmed(confirmedPhone)
                        onSuccess()
                    }
                },
                enabled = !loading && otpCode.length == 6,
            )

            if (error != null) {
                Text(error!!, color = theme.negative, fontSize = 13.sp)
            }
        }
    }
}
