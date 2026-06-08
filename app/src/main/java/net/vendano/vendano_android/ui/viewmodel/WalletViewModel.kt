package net.vendano.vendano_android.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.vendano.vendano_android.data.prefs.AppPreferences
import net.vendano.vendano_android.domain.cardano.WalletMath
import net.vendano.vendano_android.domain.model.*
import net.vendano.vendano_android.domain.repository.WalletRepository
import net.vendano.vendano_android.util.Config
import javax.inject.Inject

private const val TAG = "WalletViewModel"

/**
 * Wallet data ViewModel.
 * Android equivalent of iOS WalletService.swift (observable properties).
 *
 * Manages: ADA balance, HOSKY balance, staking total, spendable ADA,
 * fiat rate, recent transactions, and on-chain refresh.
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    // ─── Balances ─────────────────────────────────────────────────

    private val _adaBalance = MutableStateFlow(0.0)
    val adaBalance: StateFlow<Double> = _adaBalance.asStateFlow()

    private val _totalAdaBalance = MutableStateFlow<Double?>(null)
    val totalAdaBalance: StateFlow<Double?> = _totalAdaBalance.asStateFlow()

    private val _hoskyBalance = MutableStateFlow(0.0)
    val hoskyBalance: StateFlow<Double> = _hoskyBalance.asStateFlow()

    private val _spendableAda = MutableStateFlow<Double?>(null)
    val spendableAda: StateFlow<Double?> = _spendableAda.asStateFlow()

    // ─── Price ────────────────────────────────────────────────────

    private val _adaFiatRate = MutableStateFlow<Double?>(null)
    val adaFiatRate: StateFlow<Double?> = _adaFiatRate.asStateFlow()

    val fiatCurrency: StateFlow<FiatCurrency> = prefs.fiatCurrency
        .map { code -> FiatCurrency.values().firstOrNull { it.code == code } ?: FiatCurrency.USD }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FiatCurrency.USD)

    fun setFiatCurrency(c: FiatCurrency) = viewModelScope.launch { prefs.setFiatCurrency(c.code) }

    // ─── Refresh state ────────────────────────────────────────────

    private val _checkingTxs = MutableStateFlow(false)
    val checkingTxs: StateFlow<Boolean> = _checkingTxs.asStateFlow()

    private val _recentTxs = MutableStateFlow<List<TxRowViewModel>>(emptyList())
    val recentTxs: StateFlow<List<TxRowViewModel>> = _recentTxs.asStateFlow()

    // ─── Wallet address (set by AppViewModel after wallet load) ───

    private val _walletAddressState = MutableStateFlow("")
    val walletAddress: StateFlow<String> = _walletAddressState.asStateFlow()

    private var _walletAddress: String = ""
        set(value) { field = value; _walletAddressState.value = value }
    private var _stakeAddress: String = ""
    private var _allAddresses: List<String> = emptyList()
    private var _environment: AppEnvironment = AppEnvironment.MAINNET

    fun configure(
        walletAddress: String,
        stakeAddress: String,
        allAddresses: List<String>,
        env: AppEnvironment,
    ) {
        _walletAddress = walletAddress
        _stakeAddress = stakeAddress
        _allAddresses = allAddresses
        _environment = env
    }

    // ─── Refresh ─────────────────────────────────────────────────

    fun refreshOnChainData() {
        if (_walletAddress.isEmpty()) return
        viewModelScope.launch {
            _checkingTxs.value = true
            try {
                // 1. Balances
                refreshBalances()
                // 2. Price
                loadPrice()
                // 3. Transactions
                fetchTransactions()
            } catch (e: Exception) {
                Log.e(TAG, "refreshOnChainData failed: ${e.message}")
            } finally {
                _checkingTxs.value = false
            }
        }
    }

    suspend fun refreshBalances() {
        if (_walletAddress.isEmpty()) return
        val projectId = Config.blockfrostKey(_environment)
        walletRepo.fetchBalances(
            walletAddress = _walletAddress,
            stakeAddress = _stakeAddress,
            projectId = projectId,
            env = _environment,
        ).onSuccess { (utxoLovelace, stakingLovelace, hosky) ->
            _adaBalance.value = WalletMath.lovelaceToAda(utxoLovelace)
            _totalAdaBalance.value = stakingLovelace?.let { WalletMath.lovelaceToAda(it) }
            _hoskyBalance.value = hosky

            // Max spendable (conservative estimate: adaBalance - 2 ADA headroom)
            val spendable = maxOf(0.0, _adaBalance.value - 2.0)
            _spendableAda.value = spendable
        }.onFailure { e ->
            Log.e(TAG, "refreshBalances failed: ${e.message}")
        }
    }

    suspend fun loadPrice() {
        val pair = fiatCurrency.value.code
        walletRepo.fetchAdaPrice(pair).onSuccess { rate ->
            _adaFiatRate.value = rate
        }.onFailure { e ->
            Log.w(TAG, "loadPrice failed: ${e.message}")
        }
    }

    private suspend fun fetchTransactions() {
        if (_walletAddress.isEmpty()) return
        val projectId = Config.blockfrostKey(_environment)
        walletRepo.fetchTransactions(
            walletAddress = _walletAddress,
            allAddresses = _allAddresses,
            projectId = projectId,
            env = _environment,
        ).onSuccess { rawTxs ->
            val myAddresses = (_allAddresses + _walletAddress).toSet()
            var running = _adaBalance.value

            val vms = rawTxs.mapNotNull { tx ->
                val myInputSum = tx.inputs.filter { it.address in myAddresses }.sumOf { it.amount }
                val myOutputSum = tx.outputs.filter { it.address in myAddresses }.sumOf { it.amount }
                val netLovelace = myOutputSum.toLong() - myInputSum.toLong()
                val netAda = WalletMath.lovelaceToAda(netLovelace)
                if (netAda == 0.0) return@mapNotNull null

                val outgoing = netAda < 0
                val movedAda = kotlin.math.abs(netAda)

                val balanceAfter = running
                running -= netAda

                val counterparty = if (outgoing) {
                    tx.outputs.map { it.address }.firstOrNull { it !in myAddresses }
                } else {
                    tx.inputs.map { it.address }.firstOrNull { it !in myAddresses }
                } ?: "Unknown"

                TxRowViewModel(
                    id = tx.hash,
                    date = tx.date,
                    outgoing = outgoing,
                    amount = movedAda,
                    counterpartyAddress = counterparty,
                    name = null,
                    avatarURL = null,
                    balanceAfter = balanceAfter,
                )
            }

            _recentTxs.value = vms
        }.onFailure { e ->
            Log.e(TAG, "fetchTransactions failed: ${e.message}")
        }
    }

    // ─── Fee computation ──────────────────────────────────────────

    suspend fun estimateNetworkFee(toAddress: String, sendAda: Double, tipAda: Double): Result<Double> =
        walletRepo.estimateNetworkFee(toAddress, sendAda, tipAda, _environment)

    suspend fun maxSendableAda(toAddress: String, tipAda: Double): Result<Double> =
        walletRepo.maxSendableAda(toAddress, tipAda, _environment)

    // ─── Helpers ─────────────────────────────────────────────────

    fun effectiveAppFee(sendAda: Double): Double = WalletMath.effectiveAppFeeAda(sendAda)

    fun clearCache() {
        _recentTxs.value = emptyList()
        _adaBalance.value = 0.0
        _totalAdaBalance.value = null
        _hoskyBalance.value = 0.0
        _spendableAda.value = null
    }
}
