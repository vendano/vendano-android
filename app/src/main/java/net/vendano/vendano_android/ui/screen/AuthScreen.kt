package net.vendano.vendano_android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.vendano.vendano_android.ui.component.LightGradientBackground
import net.vendano.vendano_android.ui.component.PrimaryButton
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.viewmodel.AppViewModel
import net.vendano.vendano_android.ui.viewmodel.OnboardingViewModel

/**
 * Auth screen: choose phone or email, enter handle.
 * Mirrors iOS AuthView.swift.
 */
@Composable
fun AuthScreen(
    appViewModel: AppViewModel,
    onOtpSent: () -> Unit,
    onEmailSent: () -> Unit,
    onSuccess: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val theme = LocalVendanoTheme.current
    val focus = LocalFocusManager.current
    val activity = LocalContext.current as? android.app.Activity

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Phone", "Email")

    val phone by vm.phoneNumber.collectAsState()
    val dialCode by vm.dialCode.collectAsState()
    val email by vm.emailInput.collectAsState()
    val loading by vm.authLoading.collectAsState()
    val error by vm.authError.collectAsState()

    // Shown after the magic link email is dispatched
    var emailSentTo by remember { mutableStateOf<String?>(null) }

    LightGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 64.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Sign In",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary,
            )
            Text(
                text = "Your phone number or email becomes your Cardano address book.",
                fontSize = 15.sp,
                color = theme.textSecondary,
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = theme.fieldBackground,
                contentColor = theme.accent,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .padding(horizontal = 8.dp),
                    )
                },
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            emailSentTo = null
                            vm.clearAuthError()
                        },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) theme.accent else theme.textSecondary,
                            )
                        },
                    )
                }
            }

            if (selectedTab == 0) {
                // ─── Phone tab ───────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dialCode,
                        onValueChange = vm::setDialCode,
                        label = { Text("Code") },
                        modifier = Modifier.width(90.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next,
                        ),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = vm::setPhoneNumber,
                        label = { Text("Phone number") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        singleLine = true,
                    )
                }

                PrimaryButton(
                    text = if (loading) "Sending…" else "Send SMS Code",
                    onClick = {
                        focus.clearFocus()
                        val fullPhone = "$dialCode${phone.filter { it.isDigit() }}"
                        appViewModel.setOtpPhone(fullPhone)
                        activity?.let { act ->
                            vm.sendPhoneOtp(
                            phone = fullPhone,
                            activity = act,
                            onSent = onOtpSent,
                            onAutoVerified = onSuccess,
                        )
                        }
                    },
                    enabled = !loading && phone.filter { it.isDigit() }.length >= 6,
                )

            } else {
                // ─── Email tab ───────────────────────────────────────────────
                if (emailSentTo == null) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = vm::setEmail,
                        label = { Text("Email address") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        singleLine = true,
                    )

                    PrimaryButton(
                        text = if (loading) "Sending…" else "Send Magic Link",
                        onClick = {
                            focus.clearFocus()
                            val trimmed = email.trim()
                            appViewModel.setOtpEmail(trimmed)
                            vm.sendEmailLink(
                                onSuccess = {
                                    emailSentTo = trimmed
                                    onEmailSent()
                                },
                            )
                        },
                        enabled = !loading && email.contains("@") && email.contains("."),
                    )
                } else {
                    // Confirmation state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = theme.accent,
                            modifier = Modifier.size(56.dp),
                        )
                        Text(
                            text = "Check your inbox",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textPrimary,
                        )
                        Text(
                            text = "We sent a magic sign-in link to\n$emailSentTo\n\nTap the link in your email to sign in.",
                            fontSize = 15.sp,
                            color = theme.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                        TextButton(
                            onClick = {
                                emailSentTo = null
                                vm.clearAuthError()
                            },
                        ) {
                            Text("Use a different email", color = theme.accent)
                        }
                        OutlinedButton(
                            onClick = {
                                if (emailSentTo != null) {
                                    vm.sendEmailLink(onSuccess = {})
                                }
                            },
                            shape = RoundedCornerShape(50),
                        ) {
                            Text("Resend link", color = theme.accent)
                        }
                    }
                }
            }

            if (error != null) {
                Text(error!!, color = theme.negative, fontSize = 13.sp)
            }
        }
    }
}
