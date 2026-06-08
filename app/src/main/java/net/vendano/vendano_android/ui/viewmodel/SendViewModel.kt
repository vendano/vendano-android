package net.vendano.vendano_android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.vendano.vendano_android.data.firebase.FirebaseProfileService
import net.vendano.vendano_android.domain.model.*
import net.vendano.vendano_android.domain.repository.WalletRepository
import net.vendano.vendano_android.util.Config
import net.vendano.vendano_android.util.isValidEmail
import javax.inject.Inject

private const val LOOKUP_DEBOUNCE_MS = 200L

/**
 * SendViewModel: manages all state for the Send screen.
 * Android equivalent of iOS SendView's @State properties + helper methods.
 */
@HiltViewModel
class SendViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val profileService: FirebaseProfileService,
) : ViewModel() {

    // ─── Send method ──────────────────────────────────────────────

    private val _sendMethod = MutableStateFlow(SendMethod.EMAIL)
    val sendMethod: StateFlow<SendMethod> = _sendMethod.asStateFlow()

    fun setSendMethod(m: SendMethod) {
        _sendMethod.value = m
        _recipient.value = null
    }

    // ─── Input fields ─────────────────────────────────────────────

    private val _emailInput = MutableStateFlow("")
    val emailInput: StateFlow<String> = _emailInput.asStateFlow()
    fun setEmail(v: String) { _emailInput.value = v; triggerLookup() }

    private val _dialCode = MutableStateFlow("+1")
    val dialCode: StateFlow<String> = _dialCode.asStateFlow()
    fun setDialCode(v: String) { _dialCode.value = v; triggerLookup() }

    private val _localNumber = MutableStateFlow("")
    val localNumber: StateFlow<String> = _localNumber.asStateFlow()
    fun setLocalNumber(v: String) { _localNumber.value = v; triggerLookup() }

    private val _addressInput = MutableStateFlow("")
    val addressInput: StateFlow<String> = _addressInput.asStateFlow()
    fun setAddress(v: String) { _addressInput.value = v; triggerLookup() }

    // Pre-fill from sendToAddress draft
    fun applyDraftAddress(address: String) {
        _sendMethod.value = SendMethod.ADDRESS
        _addressInput.value = address
        triggerLookup()
    }

    // ─── Amount ───────────────────────────────────────────────────

    private val _adaText = MutableStateFlow("")
    val adaText: StateFlow<String> = _adaText.asStateFlow()
    fun setAdaText(v: String) { _adaText.value = v }

    private val _includeTip = MutableStateFlow(false)
    val includeTip: StateFlow<Boolean> = _includeTip.asStateFlow()
    fun setIncludeTip(v: Boolean) { _includeTip.value = v; if (!v) _tipText.value = "" }

    private val _tipText = MutableStateFlow("")
    val tipText: StateFlow<String> = _tipText.asStateFlow()
    fun setTipText(v: String) { _tipText.value = v }

    // ─── Recipient lookup ─────────────────────────────────────────

    private val _recipient = MutableStateFlow<Recipient?>(null)
    val recipient: StateFlow<Recipient?> = _recipient.asStateFlow()

    private var lookupJob: Job? = null

    private fun triggerLookup() {
        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            delay(LOOKUP_DEBOUNCE_MS)
            performLookup()
        }
    }

    private suspend fun performLookup() {
        val handle = currentHandle() ?: run { _recipient.value = null; return }
        // Try Vendano Firebase first
        val rec = profileService.fetchRecipient(handle)
        if (rec != null) {
            _recipient.value = rec
            return
        }
        // Try ADA handle
        if (handle.startsWith("$") || (!handle.contains("@") && !handle.contains("+"))) {
            val env = AppEnvironment.MAINNET // use current env from AppViewModel in real integration
            val projectId = Config.blockfrostKey(env)
            walletRepo.resolveAdaHandle(handle, projectId, env).onSuccess { address ->
                if (address != null) {
                    _recipient.value = Recipient(
                        name = if (handle.startsWith("$")) handle.lowercase() else "$$handle".lowercase(),
                        avatarURL = null,
                        address = address,
                    )
                } else {
                    _recipient.value = null
                }
            }
        } else {
            _recipient.value = null
        }
    }

    private fun currentHandle(): String? = when (_sendMethod.value) {
        SendMethod.EMAIL -> _emailInput.value.takeIf { it.isValidEmail() }?.lowercase()
        SendMethod.PHONE -> {
            val digits = _localNumber.value.filter { it.isDigit() }
            if (digits.length in 6..15 && _dialCode.value.startsWith("+")) {
                "${_dialCode.value}$digits"
            } else null
        }
        SendMethod.ADDRESS -> _addressInput.value.trim().takeIf { it.isNotEmpty() }
    }

    // ─── Fee estimation ───────────────────────────────────────────

    private val _networkFee = MutableStateFlow(0.0)
    val networkFee: StateFlow<Double> = _networkFee.asStateFlow()

    private val _feeLoading = MutableStateFlow(false)
    val feeLoading: StateFlow<Boolean> = _feeLoading.asStateFlow()

    private val _feeError = MutableStateFlow<String?>(null)
    val feeError: StateFlow<String?> = _feeError.asStateFlow()

    fun recalcFee(walletVm: WalletViewModel) {
        val dest = (_recipient.value?.address ?: _addressInput.value).trim()
        val ada = _adaText.value.toDoubleOrNull() ?: 0.0
        if (ada <= 0 || dest.isEmpty()) {
            _networkFee.value = 0.0; _feeError.value = null; return
        }
        _feeLoading.value = true
        _feeError.value = null
        viewModelScope.launch {
            walletVm.estimateNetworkFee(dest, ada, _tipText.value.toDoubleOrNull() ?: 0.0)
                .onSuccess { fee -> _networkFee.value = fee; _feeError.value = null }
                .onFailure { e -> _feeError.value = e.message; _networkFee.value = 0.0 }
            _feeLoading.value = false
        }
    }

    // ─── Send ─────────────────────────────────────────────────────

    sealed class SendUiEvent {
        object Success : SendUiEvent()
        data class Error(val message: String) : SendUiEvent()
    }

    private val _sendEvent = MutableSharedFlow<SendUiEvent>()
    val sendEvent: SharedFlow<SendUiEvent> = _sendEvent.asSharedFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun send(env: AppEnvironment, onRefresh: () -> Unit) {
        val dest = (_recipient.value?.address ?: _addressInput.value).trim()
        val ada = _adaText.value.toDoubleOrNull() ?: return
        val tip = _tipText.value.toDoubleOrNull() ?: 0.0

        _isSending.value = true
        viewModelScope.launch {
            walletRepo.sendTransaction(dest, ada, tip, env)
                .onSuccess { txHash ->
                    _isSending.value = false
                    onRefresh()
                    _sendEvent.emit(SendUiEvent.Success)
                    profileService.recordTransaction(dest, ada, txHash)
                }
                .onFailure { e ->
                    _isSending.value = false
                    _sendEvent.emit(SendUiEvent.Error(e.message ?: "Send failed"))
                }
        }
    }

    // ─── Validation helpers ───────────────────────────────────────

    val recipientOk: Boolean
        get() = when (_sendMethod.value) {
            SendMethod.EMAIL -> _emailInput.value.isValidEmail()
            SendMethod.PHONE -> {
                val digits = _localNumber.value.filter { it.isDigit() }
                digits.length in 6..15 && _dialCode.value.startsWith("+")
            }
            SendMethod.ADDRESS -> {
                val trimmed = _addressInput.value.trim()
                trimmed.looksLikeCardanoAddress() || !(_recipient.value?.address.isNullOrEmpty())
            }
        }

    fun amountOk(walletVm: WalletViewModel): Boolean {
        val ada = _adaText.value.toDoubleOrNull() ?: 0.0
        val tip = _tipText.value.toDoubleOrNull() ?: 0.0
        val total = ada + _networkFee.value + walletVm.effectiveAppFee(ada) + tip
        val max = walletVm.spendableAda.value ?: walletVm.adaBalance.value
        return ada > 0 && total <= max
    }

    private fun String.looksLikeCardanoAddress(): Boolean =
        startsWith("addr") || startsWith("stake")
}
