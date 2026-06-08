package net.vendano.vendano_android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.vendano.vendano_android.ui.component.LightGradientBackground
import net.vendano.vendano_android.ui.component.PrimaryButton
import net.vendano.vendano_android.ui.theme.LocalVendanoTheme
import net.vendano.vendano_android.ui.viewmodel.AppViewModel
import net.vendano.vendano_android.ui.viewmodel.OnboardingViewModel

/**
 * Profile creation screen.
 * Mirrors iOS CreateProfileView.swift.
 */
@Composable
fun CreateProfileScreen(
    appViewModel: AppViewModel,
    onSuccess: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val theme = LocalVendanoTheme.current
    val focus = LocalFocusManager.current
    val name by vm.displayNameInput.collectAsState()
    val loading by vm.authLoading.collectAsState()
    val error by vm.authError.collectAsState()

    LightGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Create Profile", fontSize = 28.sp, color = theme.textPrimary)
            Text(
                "Choose a display name. This is how others will see you.",
                fontSize = 15.sp, color = theme.textSecondary,
            )

            OutlinedTextField(
                value = name,
                onValueChange = vm::setDisplayNameInput,
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                singleLine = true,
            )

            PrimaryButton(
                text = if (loading) "Saving…" else "Continue",
                onClick = {
                    focus.clearFocus()
                    vm.saveProfile {
                        appViewModel.updateDisplayName(name)
                        onSuccess()
                    }
                },
                enabled = !loading && name.trim().isNotBlank(),
            )

            if (error != null) {
                Text(error!!, color = theme.negative, fontSize = 13.sp)
            }
        }
    }
}
