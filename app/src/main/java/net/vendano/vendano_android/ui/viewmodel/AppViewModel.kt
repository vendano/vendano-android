package net.vendano.vendano_android.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.vendano.vendano_android.data.firebase.FirebaseProfileService
import net.vendano.vendano_android.data.prefs.AppPreferences
import net.vendano.vendano_android.data.secure.SecureKeyStore
import net.vendano.vendano_android.domain.model.*
import net.vendano.vendano_android.domain.repository.WalletRepository
import net.vendano.vendano_android.util.Config
import javax.inject.Inject

private const val TAG = "AppViewModel"

/**
 * Root application state ViewModel.
 * Android equivalent of iOS AppState.swift.
 *
 * Holds the navigation step, user profile, wallet address, and
 * top-level UI state (toast, recent txs).
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val profileService: FirebaseProfileService,
    private val prefs: AppPreferences,
    private val secureKeyStore: SecureKeyStore,
) : ViewModel() {

    // ─── Onboarding / Navigation ──────────────────────────────────

    private val _onboardingStep = MutableStateFlow(OnboardingStep.LOADING)
    val onboardingStep: StateFlow<OnboardingStep> = _onboardingStep.asStateFlow()

    fun setOnboardingStep(step: OnboardingStep) { _onboardingStep.value = step }

    // ─── Seed words (in-memory for the new-wallet flow only) ──────

    private val _seedWords = MutableStateFlow<List<String>>(emptyList())
    val seedWords: StateFlow<List<String>> = _seedWords.asStateFlow()

    private val _seedLanguage = MutableStateFlow(MnemonicLanguage.ENGLISH)
    val seedLanguage: StateFlow<MnemonicLanguage> = _seedLanguage.asStateFlow()

    fun setSeedWords(words: List<String>) { _seedWords.value = words }
    fun setSeedLanguage(lang: MnemonicLanguage) { _seedLanguage.value = lang }

    // ─── User profile ─────────────────────────────────────────────

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _avatarBytes = MutableStateFlow<ByteArray?>(null)
    val avatarBytes: StateFlow<ByteArray?> = _avatarBytes.asStateFlow()

    private val _phone = MutableStateFlow<List<String>>(emptyList())
    val phone: StateFlow<List<String>> = _phone.asStateFlow()

    private val _email = MutableStateFlow<List<String>>(emptyList())
    val email: StateFlow<List<String>> = _email.asStateFlow()

    // ─── Store settings ───────────────────────────────────────────

    val storeName: StateFlow<String> = prefs.storeName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val storeBufferPercent: StateFlow<Double> = prefs.storeBufferPercent
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.05)

    val storeTipsEnabled: StateFlow<Boolean> = prefs.storeTipsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setStoreName(name: String) = viewModelScope.launch { prefs.setStoreName(name) }
    fun setStoreBufferPercent(p: Double) = viewModelScope.launch { prefs.setStoreBufferPercent(p) }
    fun setStoreTipsEnabled(v: Boolean) = viewModelScope.launch { prefs.setStoreTipsEnabled(v) }

    // ─── OTP / auth state ─────────────────────────────────────────

    private val _otpEmail = MutableStateFlow<String?>(null)
    val otpEmail: StateFlow<String?> = _otpEmail.asStateFlow()

    private val _otpPhone = MutableStateFlow<String?>(null)
    val otpPhone: StateFlow<String?> = _otpPhone.asStateFlow()

    fun setOtpEmail(v: String?) { _otpEmail.value = v }
    fun setOtpPhone(v: String?) { _otpPhone.value = v }

    // ─── Wallet address ───────────────────────────────────────────

    private val _walletAddress = MutableStateFlow("")
    val walletAddress: StateFlow<String> = _walletAddress.asStateFlow()

    fun setWalletAddress(address: String) {
        _walletAddress.value = address
    }

    // ─── Environment ──────────────────────────────────────────────

    private val _environment = MutableStateFlow(AppEnvironment.MAINNET)
    val environment: StateFlow<AppEnvironment> = _environment.asStateFlow()

    fun setEnvironment(env: AppEnvironment) { _environment.value = env }

    fun resolveEnvironment(identifier: String): AppEnvironment =
        Config.resolveEnvironment(identifier)

    // ─── Send-to draft (from deep link / QR scan) ─────────────────

    private val _sendToAddress = MutableStateFlow<String?>(null)
    val sendToAddress: StateFlow<String?> = _sendToAddress.asStateFlow()

    fun setSendToAddress(address: String?) { _sendToAddress.value = address }

    // ─── Toast ────────────────────────────────────────────────────

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            _toastMessage.value = null
        }
    }

    // ─── FAQ ──────────────────────────────────────────────────────

    private val _viewedFaqIds = MutableStateFlow<Set<String>>(emptySet())
    val viewedFaqIds: StateFlow<Set<String>> = _viewedFaqIds.asStateFlow()

    fun markFaqViewed(id: String) {
        _viewedFaqIds.update { it + id }
        viewModelScope.launch { profileService.markFaqViewed(id) }
    }

    // ─── Expert mode ──────────────────────────────────────────────

    val expertMode: StateFlow<Boolean> = prefs.expertMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setExpertMode(v: Boolean) = viewModelScope.launch { prefs.setExpertMode(v) }

    // ─── Bootstrap ────────────────────────────────────────────────

    /**
     * Called from the root composable on first composition.
     * Mirrors iOS RootView.task + AppDelegate.
     */
    fun bootstrap(isDemo: Boolean) {
        viewModelScope.launch {
            try {
                if (FirebaseAuth.getInstance().currentUser == null && !isDemo) {
                    _onboardingStep.value = OnboardingStep.SPLASH
                    return@launch
                }

                // Load cached avatar from DataStore (local — instant)
                prefs.avatarBase64.first()?.let { b64 ->
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    _avatarBytes.value = bytes
                }

                // Fetch profile — time-boxed so a bad connection can't block startup
                withTimeoutOrNull(8_000L) {
                    val pub = profileService.fetchPublicProfile()
                    val priv = profileService.fetchPrivateProfile()
                    pub?.let {
                        _displayName.value = it.displayName
                        _avatarUrl.value = it.avatarURL
                    }
                    priv?.let {
                        _phone.value = it.phone
                        _email.value = it.email
                        _viewedFaqIds.value = it.viewedFaqIds.toSet()
                    }
                } ?: Log.w(TAG, "bootstrap: profile fetch timed out, continuing")

                // Restore wallet from secure storage
                val saved = walletRepo.loadSeedWords()
                if (saved != null) {
                    val result = runCatching { walletRepo.importWallet(saved, _environment.value) }
                    result.getOrNull()?.onSuccess { address ->
                        _walletAddress.value = address
                        _onboardingStep.value = OnboardingStep.HOME
                        return@launch
                    }
                }

                _onboardingStep.value = OnboardingStep.WALLET_CHOICE
            } catch (e: Exception) {
                Log.e(TAG, "bootstrap error: ${e.message}", e)
                // Always unblock the UI — fall back to splash on any unexpected error
                _onboardingStep.value = OnboardingStep.SPLASH
            }
        }
    }

    // ─── Profile actions ──────────────────────────────────────────

    fun updateDisplayName(name: String) {
        _displayName.value = name
        viewModelScope.launch {
            try { profileService.updateDisplayName(name) }
            catch (e: Exception) { Log.e(TAG, "updateDisplayName: ${e.message}") }
        }
    }

    fun uploadAvatar(jpegBytes: ByteArray) {
        _avatarBytes.value = jpegBytes
        viewModelScope.launch {
            val b64 = android.util.Base64.encodeToString(jpegBytes, android.util.Base64.DEFAULT)
            prefs.setAvatarBase64(b64)
            val url = profileService.uploadAvatar(jpegBytes)
            if (url != null) _avatarUrl.value = url
        }
    }

    fun removeAvatar() {
        _avatarBytes.value = null
        _avatarUrl.value = null
        viewModelScope.launch { prefs.setAvatarBase64(null) }
    }

    fun removePhone(phone: String) {
        val newPhones = _phone.value.filter { it != phone }
        if (newPhones.isEmpty() && _email.value.isEmpty()) return
        _phone.value = newPhones
        viewModelScope.launch {
            try { profileService.removePhone(phone) }
            catch (e: Exception) { _phone.update { it + phone } }
        }
    }

    fun removeEmail(emailToRemove: String) {
        val newEmails = _email.value.filter { it.lowercase() != emailToRemove.lowercase() }
        if (newEmails.isEmpty() && _phone.value.isEmpty()) return
        _email.value = newEmails
        viewModelScope.launch {
            try { profileService.removeEmail(emailToRemove) }
            catch (e: Exception) { _email.update { it + emailToRemove } }
        }
    }

    // ─── Appearance preference (exposed from DataStore) ───────────

    val appearancePreference: StateFlow<String> = prefs.appearance
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    fun setAppearance(v: String) = viewModelScope.launch { prefs.setAppearance(v) }

    // ─── Wallet removal ───────────────────────────────────────────

    fun removeWallet() {
        _walletAddress.value = ""
        _seedWords.value = emptyList()
        viewModelScope.launch { walletRepo.clearWallet() }
        _onboardingStep.value = OnboardingStep.WALLET_CHOICE
    }

    /**
     * Signs out the current user without touching any server-side data.
     * Clears only the local session state (wallet seed, cached prefs, UI state)
     * so the user can sign back in and recover their profile.
     *
     * IMPORTANT: Do NOT call [nukeAccount] for a sign-out flow.
     * [nukeAccount] permanently deletes all Firebase/Firestore data and is
     * reserved exclusively for "Delete Account" scenarios.
     */
    fun signOut() {
        _walletAddress.value = ""
        _seedWords.value = emptyList()
        _displayName.value = ""
        _avatarUrl.value = null
        _avatarBytes.value = null
        _phone.value = emptyList()
        _email.value = emptyList()
        _viewedFaqIds.value = emptySet()
        viewModelScope.launch {
            walletRepo.clearWallet()
            prefs.setAvatarBase64(null)
            prefs.clearAuthPrefs()
        }
        _onboardingStep.value = OnboardingStep.SPLASH
    }

    fun nukeAccount() {
        viewModelScope.launch {
            removeWallet()
            profileService.deleteAvatarFolder()
            profileService.removeUserData()
            _displayName.value = ""
            _avatarUrl.value = null
            _avatarBytes.value = null
            _phone.value = emptyList()
            _email.value = emptyList()
            _viewedFaqIds.value = emptySet()
            prefs.setAvatarBase64(null)
            prefs.clearAuthPrefs()
            _onboardingStep.value = OnboardingStep.SPLASH
        }
    }

    // ─── Handle profile state after auth ─────────────────────────

    fun onPhoneConfirmed(phone: String) {
        if (!_phone.value.contains(phone)) {
            _phone.update { it + phone }
        }
    }

    fun onEmailConfirmed(emailAddr: String) {
        if (!_email.value.any { it.lowercase() == emailAddr.lowercase() }) {
            _email.update { it + emailAddr }
        }
    }
}
