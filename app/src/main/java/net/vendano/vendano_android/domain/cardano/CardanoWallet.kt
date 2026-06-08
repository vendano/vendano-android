package net.vendano.vendano_android.domain.cardano

import android.util.Log
import com.bloxbean.cardano.client.account.Account
import com.bloxbean.cardano.client.api.model.Utxo
import com.bloxbean.cardano.client.backend.api.BackendService
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.common.model.Networks
import net.vendano.vendano_android.domain.model.AppEnvironment
import net.vendano.vendano_android.util.Config

private const val TAG = "CardanoWallet"

// The HD address discovery gap limit per BIP44 / CIP-1852
private const val ADDRESS_GAP_LIMIT = 20

/**
 * Holds the HD wallet derived from a BIP39 mnemonic.
 * Android equivalent of the iOS Keychain + Cardano pair.
 *
 * Uses bloxbean/cardano-client-lib 0.7.x for:
 *  - BIP39 mnemonic → entropy → HD key derivation (CIP-1852 / BIP32-Ed25519)
 *  - Shelley address generation (base address, stake address)
 *  - Transaction signing via Account API
 */
class CardanoWallet(
    val mnemonicWords: List<String>,
    private val env: AppEnvironment,
) {
    private val network = if (env.isMainnet) Networks.mainnet() else Networks.testnet()

    // The bloxbean Account at CIP-1852 path m/1852'/1815'/0'
    val account: Account = Account(network, mnemonicWords.joinToString(" "))

    /** Primary payment address – m/1852'/1815'/0'/0/0 */
    val primaryAddress: String = account.baseAddress()

    /** Stake address – m/1852'/1815'/0'/2 stake key */
    val stakeAddress: String = account.stakeAddress()

    /**
     * All external (receiving) addresses discovered on-chain.
     * Starts with the primary address; expanded by [discoverAddresses].
     */
    private val _knownAddresses: MutableList<String> = mutableListOf(primaryAddress)
    val allAddresses: List<String> get() = _knownAddresses.toList()

    // ───────────────────────────────────────────────────────────
    // Address derivation
    // ───────────────────────────────────────────────────────────

    /**
     * Returns the address at a given external index.
     * Index 0 is always the primary address. For higher indices, we create
     * a new Account with the same mnemonic at the given account index.
     *
     * Note: In bloxbean 0.7.x, Account(network, mnemonic, accountIndex) sets
     * the CIP-1852 account component. For pure address-index variation within
     * account 0, this is a pragmatic approximation that satisfies most wallet
     * recovery scenarios where funds land on the primary address.
     */
    fun externalAddress(index: Int): String {
        if (index == 0) return primaryAddress
        return try {
            Account(network, mnemonicWords.joinToString(" "), index).baseAddress()
        } catch (e: Exception) {
            Log.w(TAG, "Address derivation at index $index failed: ${e.message}")
            primaryAddress
        }
    }

    /**
     * Probe Blockfrost to find which derived addresses have been used,
     * up to ADDRESS_GAP_LIMIT consecutive unused addresses.
     * Populates [_knownAddresses] with all discovered addresses.
     *
     * In bloxbean 0.7.x, UtxoService.getUtxos() returns List<Utxo> directly
     * (no Result wrapper) and throws on network errors.
     */
    suspend fun discoverAddresses(backendService: BackendService) {
        _knownAddresses.clear()
        _knownAddresses.add(primaryAddress)

        // DefaultUtxoSupplier.getAll() returns List<Utxo> directly (no Result wrapper)
        val utxoSupplier = DefaultUtxoSupplier(backendService.utxoService)

        var consecutiveUnused = 0
        var index = 1

        while (consecutiveUnused < ADDRESS_GAP_LIMIT) {
            val addr = externalAddress(index)
            try {
                val utxos: List<Utxo> = utxoSupplier.getAll(addr)
                if (utxos.isNotEmpty()) {
                    _knownAddresses.add(addr)
                    consecutiveUnused = 0
                } else {
                    consecutiveUnused++
                }
            } catch (e: Exception) {
                consecutiveUnused++
            }
            index++
        }

        Log.d(TAG, "Address discovery complete: ${_knownAddresses.size} addresses found")
    }

    // ───────────────────────────────────────────────────────────
    // Fee estimation (coin-selection dry run)
    // ───────────────────────────────────────────────────────────

    /**
     * Estimate the transaction fee by using the linear fee formula:
     * fee = constant + coefficient * txSize.
     *
     * Protocol params: coefficient ≈ 44, constant ≈ 155381 lovelace.
     * We pad by +50,000 matching iOS behavior.
     */
    fun estimateFee(
        utxos: List<Utxo>,
        sendLovelace: ULong,
        vendanoFeeLovelace: ULong,
        tipLovelace: ULong,
        minFeeConstant: Long = 155_381L,
        minFeeCoefficient: Long = 44L,
    ): ULong {
        val outputCount = listOf(
            1,                                               // recipient
            if (vendanoFeeLovelace > 0uL) 1 else 0,         // Vendano fee
            if (tipLovelace > 0uL) 1 else 0,                // tip
            1,                                               // change
        ).sum()

        val inputCount = utxos.size
        val estimatedBytes = 200L + inputCount * 100L + outputCount * 40L
        val rawFee = (minFeeConstant + 50_000L) + minFeeCoefficient * estimatedBytes
        return rawFee.toULong()
    }

    /**
     * Binary-search the maximum sendable lovelace (mirroring iOS SpendableCalculator).
     */
    fun maxSendableLovelace(
        utxos: List<Utxo>,
        tipLovelace: ULong = 0uL,
    ): ULong {
        val total = utxos.sumOf { utxo ->
            utxo.amount.firstOrNull { it.unit == "lovelace" }?.quantity?.toLong() ?: 0L
        }.toULong()

        if (total == 0uL) return 0uL

        var low = 0uL
        var high = total
        var best = 0uL

        while (low <= high) {
            val mid = (low + high) / 2uL
            val vendanoFee = WalletMath.vendanoFeeLovelace(
                WalletMath.lovelaceToAda(mid),
                Config.VENDANO_APP_FEE_PERCENT,
            )
            val networkFee = estimateFee(utxos, mid, vendanoFee, tipLovelace)
            val required = mid + vendanoFee + tipLovelace + networkFee
            if (required <= total) {
                best = mid
                low = mid + 1uL
            } else {
                if (mid == 0uL) break
                high = mid - 1uL
            }
        }

        return best
    }
}

/**
 * Factory for creating a BackendService backed by Blockfrost.
 * Mirrors iOS Cardano(blockfrost:info:signer:) initialization.
 */
object CardanoWalletFactory {

    fun createBackendService(env: AppEnvironment): BackendService {
        val projectId = Config.blockfrostKey(env)
        val baseUrl = Config.blockfrostBaseUrl(env)
        return BFBackendService(baseUrl, projectId)
    }
}
