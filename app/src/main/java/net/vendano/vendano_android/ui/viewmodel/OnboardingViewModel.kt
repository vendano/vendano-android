package net.vendano.vendano_android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.vendano.vendano_android.data.firebase.FirebaseAuthService
import net.vendano.vendano_android.data.firebase.FirebaseProfileService
import net.vendano.vendano_android.domain.cardano.MnemonicHelper
import net.vendano.vendano_android.domain.model.AppEnvironment
import net.vendano.vendano_android.domain.model.OnboardingStep
import net.vendano.vendano_android.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * Onboarding ViewModel.
 * Handles: splash → auth → OTP → profile → wallet choice → new/import seed → confirm seed → home.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authService: FirebaseAuthService,
    private val profileService: FirebaseProfileService,
    private val walletRepo: WalletRepository,
) : ViewModel() {

    // ─── Auth ─────────────────────────────────────────────────────

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    fun clearAuthError() { _authError.value = null }

    // ─── Email auth ───────────────────────────────────────────────

    private val _emailInput = MutableStateFlow("")
    val emailInput: StateFlow<String> = _emailInput.asStateFlow()
    fun setEmail(v: String) { _emailInput.value = v }

    fun sendEmailLink(onSuccess: () -> Unit) {
        _authLoading.value = true
        viewModelScope.launch {
            val err = authService.sendEmailLink(_emailInput.value.trim())
            _authLoading.value = false
            if (err == null) onSuccess() else _authError.value = err
        }
    }

    // ─── Phone auth ───────────────────────────────────────────────

    private val _dialCode = MutableStateFlow("+1")
    val dialCode: StateFlow<String> = _dialCode.asStateFlow()
    fun setDialCode(v: String) { _dialCode.value = v }

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()
    fun setPhoneNumber(v: String) { _phoneNumber.value = v }

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()
    fun setOtpCode(v: String) { _otpCode.value = v }

    fun sendPhoneOtp(
        phone: String,
        activity: android.app.Activity,
        onSent: () -> Unit,
        onAutoVerified: () -> Unit = {},
    ) {
        _authLoading.value = true
        viewModelScope.launch {
            authService.sendPhoneOtp(
                e164 = phone,
                activity = activity,
                onCodeSent = {
                    _authLoading.value = false
                    onSent()
                },
                onError = { err ->
                    _authLoading.value = false
                    _authError.value = err
                },
                onAutoVerified = {
                    _authLoading.value = false
                    onAutoVerified()
                },
            )
        }
    }

    fun confirmOtp(phone: String, onSuccess: (String) -> Unit) {
        _authLoading.value = true
        viewModelScope.launch {
            val err = authService.confirmPhoneOtp(_otpCode.value, phone)
            _authLoading.value = false
            if (err == null) {
                profileService.savePhone(phone)
                onSuccess(phone)
            } else {
                _authError.value = err
            }
        }
    }

    // ─── Profile creation ─────────────────────────────────────────

    private val _displayNameInput = MutableStateFlow("")
    val displayNameInput: StateFlow<String> = _displayNameInput.asStateFlow()
    fun setDisplayNameInput(v: String) { _displayNameInput.value = v }

    fun saveProfile(onSuccess: () -> Unit) {
        val name = _displayNameInput.value.trim()
        if (name.isBlank()) { _authError.value = "Name cannot be empty"; return }
        _authLoading.value = true
        viewModelScope.launch {
            try {
                profileService.updateDisplayName(name)
                onSuccess()
            } catch (e: Exception) {
                _authError.value = e.message
            } finally {
                _authLoading.value = false
            }
        }
    }

    // ─── Mnemonic / Seed ─────────────────────────────────────────

    private val _mnemonicInput = MutableStateFlow("")
    val mnemonicInput: StateFlow<String> = _mnemonicInput.asStateFlow()
    fun setMnemonicInput(v: String) { _mnemonicInput.value = v }

    val parsedWords: StateFlow<List<String>> = _mnemonicInput
        .map { MnemonicHelper.tokenize(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val mnemonicValid: StateFlow<Boolean> = parsedWords
        .map { MnemonicHelper.isPlausibleMnemonic(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _walletLoading = MutableStateFlow(false)
    val walletLoading: StateFlow<Boolean> = _walletLoading.asStateFlow()

    private val _walletError = MutableStateFlow<String?>(null)
    val walletError: StateFlow<String?> = _walletError.asStateFlow()

    fun clearWalletError() { _walletError.value = null }

    /**
     * Import wallet from pasted mnemonic. On success saves seed to EncryptedSharedPrefs
     * and returns the derived address.
     */
    fun importWallet(
        words: List<String>,
        env: AppEnvironment,
        onSuccess: (address: String) -> Unit,
    ) {
        _walletLoading.value = true
        viewModelScope.launch {
            walletRepo.importWallet(words, env)
                .onSuccess { address ->
                    walletRepo.saveSeedWords(words)
                    profileService.saveAddress(address)
                    onSuccess(address)
                }
                .onFailure { e ->
                    _walletError.value = e.message ?: "Import failed"
                }
            _walletLoading.value = false
        }
    }

    /**
     * Create a brand-new wallet. The generated mnemonic words are provided by
     * the caller (NewSeedScreen generates them via bloxbean MnemonicCode).
     */
    fun createWallet(
        words: List<String>,
        env: AppEnvironment,
        onSuccess: (address: String) -> Unit,
    ) {
        importWallet(words, env, onSuccess)
    }

    // ─── Confirm seed check ───────────────────────────────────────

    /**
     * Verify that the user typed back the seed correctly.
     * Returns true if the provided words match the canonical words.
     */
    fun confirmSeed(typed: List<String>, canonical: List<String>): Boolean {
        if (typed.size != canonical.size) return false
        return typed.zip(canonical).all { (a, b) -> a.trim().lowercase() == b.trim().lowercase() }
    }
}
