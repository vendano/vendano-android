package net.vendano.vendano_android.domain.repository

import kotlinx.coroutines.flow.Flow
import net.vendano.vendano_android.domain.model.AppEnvironment
import net.vendano.vendano_android.domain.model.NFT
import net.vendano.vendano_android.domain.model.RawTx
import net.vendano.vendano_android.domain.model.Recipient
import net.vendano.vendano_android.domain.model.TxRowViewModel

/**
 * Wallet domain repository interface.
 * Implemented by WalletRepositoryImpl which orchestrates the Blockfrost API,
 * bloxbean CardanoWallet, Room cache, and SecureKeyStore.
 */
interface WalletRepository {

    // ─── Wallet lifecycle ─────────────────────────────────────────

    /** Import (or restore) a wallet from BIP39 mnemonic words. Returns primary address. */
    suspend fun importWallet(words: List<String>, env: AppEnvironment): Result<String>

    /** Persist seed words to EncryptedSharedPreferences. */
    suspend fun saveSeedWords(words: List<String>)

    /** Load seed words from secure storage. Returns null if none saved. */
    suspend fun loadSeedWords(): List<String>?

    /** Wipe all wallet data from secure storage + cache. */
    suspend fun clearWallet()

    // ─── Balances ─────────────────────────────────────────────────

    /**
     * Fetch ADA balance (lovelace), staking total (lovelace), and HOSKY balance.
     * Returns (utxoAda, stakingTotal, hosky).
     */
    suspend fun fetchBalances(
        walletAddress: String,
        stakeAddress: String,
        projectId: String,
        env: AppEnvironment,
    ): Result<Triple<ULong, ULong?, Double>>

    /** Fetch ADA/fiat spot price from Coinbase. */
    suspend fun fetchAdaPrice(fiatCode: String): Result<Double>

    // ─── Transactions ─────────────────────────────────────────────

    /** Fetch recent transactions for a wallet address from Blockfrost. */
    suspend fun fetchTransactions(
        walletAddress: String,
        allAddresses: List<String>,
        projectId: String,
        env: AppEnvironment,
    ): Result<List<RawTx>>

    /** Observe cached transaction rows from Room. */
    fun observeTransactions(walletAddress: String): Flow<List<TxRowViewModel>>

    // ─── Send ─────────────────────────────────────────────────────

    /**
     * Build, sign, and submit a payment transaction.
     * Returns the on-chain tx hash.
     */
    suspend fun sendTransaction(
        toAddress: String,
        sendAda: Double,
        tipAda: Double,
        env: AppEnvironment,
    ): Result<String>

    /** Estimate the Cardano network fee for a transaction. */
    suspend fun estimateNetworkFee(
        toAddress: String,
        sendAda: Double,
        tipAda: Double,
        env: AppEnvironment,
    ): Result<Double>

    /** Compute max sendable ADA from the current UTxO set. */
    suspend fun maxSendableAda(toAddress: String, tipAda: Double, env: AppEnvironment): Result<Double>

    // ─── ADA Handle resolution ────────────────────────────────────

    suspend fun resolveAdaHandle(handle: String, projectId: String, env: AppEnvironment): Result<String?>

    // ─── Stake address lookup ─────────────────────────────────────

    suspend fun fetchStakeAddress(paymentAddress: String, projectId: String, env: AppEnvironment): Result<String?>

    // ─── NFTs ─────────────────────────────────────────────────────

    suspend fun fetchNFTs(walletAddress: String, projectId: String, env: AppEnvironment): Result<List<NFT>>
}
