package net.vendano.vendano_android.domain.repository

import android.util.Log
import com.bloxbean.cardano.client.api.model.Amount
import com.bloxbean.cardano.client.api.model.Utxo
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService
import com.bloxbean.cardano.client.function.helper.SignerProviders
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder
import com.bloxbean.cardano.client.quicktx.Tx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.vendano.vendano_android.data.firebase.FirebaseProfileService
import net.vendano.vendano_android.data.local.db.TransactionDao
import net.vendano.vendano_android.data.local.db.TransactionEntity
import net.vendano.vendano_android.data.remote.BlockfrostApi
import net.vendano.vendano_android.data.secure.SecureKeyStore
import net.vendano.vendano_android.domain.cardano.CardanoWallet
import net.vendano.vendano_android.domain.cardano.WalletMath
import net.vendano.vendano_android.domain.model.AppEnvironment
import net.vendano.vendano_android.domain.model.NFT
import net.vendano.vendano_android.domain.model.RawTx
import net.vendano.vendano_android.domain.model.TxIO
import net.vendano.vendano_android.domain.model.TxRowViewModel
import net.vendano.vendano_android.util.Config
import net.vendano.vendano_android.util.hexEncoded
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WalletRepositoryImpl"

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val transactionDao: TransactionDao,
    private val profileService: FirebaseProfileService,
) : WalletRepository {

    // In-memory wallet session – cleared on clearWallet()
    private var activeWallet: CardanoWallet? = null

    // In-memory UTxO cache (cleared on wallet change or after send)
    // In bloxbean 0.7.x, Utxo is at com.bloxbean.cardano.client.api.model.Utxo
    private var cachedUtxos: List<Utxo> = emptyList()

    // Stake address cache
    private var cachedStakeAddress: String? = null

    // ─── Backend API factory (env-aware) ────────────────────────

    private fun buildBlockfrostApi(env: AppEnvironment): BlockfrostApi {
        val baseUrl = Config.blockfrostBaseUrl(env)
        val projectId = Config.blockfrostKey(env)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("project_id", projectId)
                    .build()
                chain.proceed(req)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BlockfrostApi::class.java)
    }

    private fun buildBackendService(env: AppEnvironment): BFBackendService =
        BFBackendService(Config.blockfrostBaseUrl(env), Config.blockfrostKey(env))

    // ─── UTxO helper ──────────────────────────────────────────────
    // Uses DefaultUtxoSupplier.getAll() which returns List<Utxo> directly
    // and handles pagination internally (no Result wrapper to unpack).

    private fun fetchUtxosForAddress(
        backendService: BFBackendService,
        address: String,
    ): List<Utxo> = try {
        DefaultUtxoSupplier(backendService.utxoService).getAll(address)
    } catch (e: Exception) {
        Log.w(TAG, "getUtxos failed for $address: ${e.message}")
        emptyList()
    }

    // ─── Wallet lifecycle ─────────────────────────────────────────

    override suspend fun importWallet(words: List<String>, env: AppEnvironment): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val wallet = CardanoWallet(words, env)
                val backendService = buildBackendService(env)
                wallet.discoverAddresses(backendService)
                activeWallet = wallet
                cachedUtxos = emptyList()
                cachedStakeAddress = null
                Result.success(wallet.primaryAddress)
            } catch (e: Exception) {
                Log.e(TAG, "importWallet failed: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun saveSeedWords(words: List<String>) {
        secureKeyStore.saveSeedWords(words)
    }

    override suspend fun loadSeedWords(): List<String>? =
        secureKeyStore.loadSeedWords()

    override suspend fun clearWallet() {
        secureKeyStore.clearSeedWords()
        activeWallet = null
        cachedUtxos = emptyList()
        cachedStakeAddress = null
    }

    // ─── Balances ─────────────────────────────────────────────────

    override suspend fun fetchBalances(
        walletAddress: String,
        stakeAddress: String,
        projectId: String,
        env: AppEnvironment,
    ): Result<Triple<ULong, ULong?, Double>> = withContext(Dispatchers.IO) {
        try {
            val wallet = activeWallet ?: return@withContext Result.failure(
                IllegalStateException("No wallet loaded"),
            )
            val api = buildBlockfrostApi(env)
            val backendService = buildBackendService(env)

            // Collect UTxOs from all known addresses
            val allAddressUtxos = wallet.allAddresses.flatMap { addr ->
                fetchUtxosForAddress(backendService, addr)
            }
            cachedUtxos = allAddressUtxos

            val totalLovelace = allAddressUtxos.sumOf { utxo ->
                utxo.amount.firstOrNull { it.unit == "lovelace" }?.quantity?.toLong() ?: 0L
            }.toULong()

            // Stake account totals
            val stakeAccountResp = api.getAccountInfo(stakeAddress)
            val stakingTotal: ULong? = if (stakeAccountResp.isSuccessful) {
                stakeAccountResp.body()?.controlledAmount?.toLongOrNull()?.toULong()
            } else null

            // HOSKY balance
            val hoskyUnit = Config.HOSKY_POLICY_ID + "HOSKY".hexEncoded
            val assetsResp = api.getAccountAssets(stakeAddress, 100)
            val hoskyQty = if (assetsResp.isSuccessful) {
                assetsResp.body()
                    ?.firstOrNull { it.unit == hoskyUnit }
                    ?.quantity?.toDoubleOrNull() ?: 0.0
            } else 0.0

            Result.success(Triple(totalLovelace, stakingTotal, hoskyQty))
        } catch (e: Exception) {
            Log.e(TAG, "fetchBalances failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun fetchAdaPrice(fiatCode: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val pair = "ADA-$fiatCode"
            val request = Request.Builder()
                .url("https://api.coinbase.com/v2/prices/$pair/spot")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty body"))
            val gson = com.google.gson.Gson()
            val parsed = gson.fromJson(
                body,
                net.vendano.vendano_android.data.remote.dto.CoinbasePriceResponse::class.java,
            )
            val price = parsed.data.amount.toDoubleOrNull()
                ?: return@withContext Result.failure(Exception("Parse error"))
            Result.success(price)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Transactions ─────────────────────────────────────────────

    override suspend fun fetchTransactions(
        walletAddress: String,
        allAddresses: List<String>,
        projectId: String,
        env: AppEnvironment,
    ): Result<List<RawTx>> = withContext(Dispatchers.IO) {
        try {
            val api = buildBlockfrostApi(env)

            val txListResp = api.getAddressTransactions(walletAddress, 20, 1, "desc")
            if (!txListResp.isSuccessful) return@withContext Result.failure(
                Exception("TX list fetch failed: ${txListResp.code()}"),
            )
            val hashes = txListResp.body()?.take(20)?.map { it.txHash } ?: emptyList()

            val rawTxs = hashes.mapNotNull { hash ->
                try {
                    val utxoResp = api.getTxUtxos(hash)
                    val infoResp = api.getTxInfo(hash)
                    if (!utxoResp.isSuccessful || !infoResp.isSuccessful) return@mapNotNull null

                    val utxoBody = utxoResp.body()!!
                    val infoBody = infoResp.body()!!

                    fun flatten(entries: List<net.vendano.vendano_android.data.remote.dto.TxUtxoEntryDto>): List<TxIO> =
                        entries.mapNotNull { e ->
                            val lovelace = e.amount.firstOrNull { it.unit == "lovelace" }
                                ?.quantity?.toLongOrNull()?.toULong() ?: return@mapNotNull null
                            TxIO(e.address, lovelace)
                        }

                    RawTx(
                        hash = hash,
                        date = Date(infoBody.blockTime * 1000L),
                        blockHeight = infoBody.blockHeight,
                        inputs = flatten(utxoBody.inputs),
                        outputs = flatten(utxoBody.outputs),
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse tx $hash: ${e.message}")
                    null
                }
            }

            // Cache in Room
            val myAddresses = allAddresses.toSet()
            val entities = rawTxs.mapNotNull { tx ->
                val myInputSum = tx.inputs.filter { it.address in myAddresses }.sumOf { it.amount }
                val myOutputSum = tx.outputs.filter { it.address in myAddresses }.sumOf { it.amount }
                val netLovelace = myOutputSum.toLong() - myInputSum.toLong()
                if (netLovelace == 0L) return@mapNotNull null
                val outgoing = netLovelace < 0
                val movedLovelace = Math.abs(netLovelace)
                val counterparty = if (outgoing) {
                    tx.outputs.map { it.address }.firstOrNull { it !in myAddresses }
                } else {
                    tx.inputs.map { it.address }.firstOrNull { it !in myAddresses }
                } ?: "Unknown"

                TransactionEntity(
                    txHash = tx.hash,
                    date = tx.date.time,
                    blockHeight = tx.blockHeight,
                    outgoing = outgoing,
                    amountLovelace = movedLovelace,
                    counterpartyAddress = counterparty,
                    counterpartyName = null,
                    counterpartyAvatarUrl = null,
                    balanceAfterLovelace = 0L,
                    walletAddress = walletAddress,
                )
            }
            transactionDao.replaceTransactions(walletAddress, entities)

            Result.success(rawTxs)
        } catch (e: Exception) {
            Log.e(TAG, "fetchTransactions failed: ${e.message}")
            Result.failure(e)
        }
    }

    override fun observeTransactions(walletAddress: String): Flow<List<TxRowViewModel>> =
        transactionDao.observeTransactions(walletAddress).map { entities ->
            entities.map { e ->
                TxRowViewModel(
                    id = e.txHash,
                    date = Date(e.date),
                    outgoing = e.outgoing,
                    amount = WalletMath.lovelaceToAda(e.amountLovelace.toULong()),
                    counterpartyAddress = e.counterpartyAddress,
                    name = e.counterpartyName,
                    avatarURL = e.counterpartyAvatarUrl,
                    balanceAfter = WalletMath.lovelaceToAda(e.balanceAfterLovelace.toULong()),
                )
            }
        }

    // ─── Send ─────────────────────────────────────────────────────

    override suspend fun sendTransaction(
        toAddress: String,
        sendAda: Double,
        tipAda: Double,
        env: AppEnvironment,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val wallet = activeWallet ?: return@withContext Result.failure(
                IllegalStateException("No wallet loaded"),
            )
            val backendService = buildBackendService(env)

            val vendanoFeeAda = WalletMath.vendanoFeeAda(sendAda, Config.VENDANO_APP_FEE_PERCENT)

            // Build transaction with QuickTxBuilder – handles UTxO selection, fee calculation,
            // and change output automatically using the configured Blockfrost backend.
            var tx = Tx()
                .payToAddress(toAddress, Amount.ada(sendAda))

            if (vendanoFeeAda > 0.0) {
                tx = tx.payToAddress(Config.vendanoFeeAddress(env), Amount.ada(vendanoFeeAda))
            }
            if (tipAda > 0.0) {
                tx = tx.payToAddress(Config.vendanoDeveloperAddress(env), Amount.ada(tipAda))
            }
            tx = tx.from(wallet.primaryAddress)

            val result = QuickTxBuilder(backendService)
                .compose(tx)
                .withSigner(SignerProviders.signerFrom(wallet.account))
                .complete()

            if (result.isSuccessful) {
                cachedUtxos = emptyList()
                Result.success(result.value ?: "")
            } else {
                Result.failure(Exception(result.toString()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendTransaction failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun estimateNetworkFee(
        toAddress: String,
        sendAda: Double,
        tipAda: Double,
        env: AppEnvironment,
    ): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val wallet = activeWallet ?: return@withContext Result.failure(
                IllegalStateException("No wallet loaded"),
            )
            val backendService = buildBackendService(env)

            val utxos = if (cachedUtxos.isNotEmpty()) cachedUtxos
            else fetchUtxosForAddress(backendService, wallet.primaryAddress)
                .also { cachedUtxos = it }

            val sendLovelace = WalletMath.adaToLovelace(sendAda)
            val vendanoFee = WalletMath.vendanoFeeLovelace(sendAda, Config.VENDANO_APP_FEE_PERCENT)
            val tipLovelace = WalletMath.adaToLovelace(tipAda)
            val fee = wallet.estimateFee(utxos, sendLovelace, vendanoFee, tipLovelace)
            Result.success(WalletMath.lovelaceToAda(fee))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun maxSendableAda(
        toAddress: String,
        tipAda: Double,
        env: AppEnvironment,
    ): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val wallet = activeWallet ?: return@withContext Result.failure(
                IllegalStateException("No wallet loaded"),
            )
            val backendService = buildBackendService(env)

            val utxos = if (cachedUtxos.isNotEmpty()) cachedUtxos
            else fetchUtxosForAddress(backendService, wallet.primaryAddress)
                .also { cachedUtxos = it }

            val tipLovelace = WalletMath.adaToLovelace(tipAda)
            val maxLovelace = wallet.maxSendableLovelace(utxos, tipLovelace)
            Result.success(WalletMath.lovelaceToAda(maxLovelace))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── ADA Handle ───────────────────────────────────────────────

    private val handleCache = mutableMapOf<String, Pair<String, Long>>()

    override suspend fun resolveAdaHandle(
        handle: String,
        projectId: String,
        env: AppEnvironment,
    ): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val name = handle.trimStart('$').lowercase()
            val cached = handleCache[name]
            if (cached != null && System.currentTimeMillis() < cached.second) {
                return@withContext Result.success(cached.first)
            }
            val unit = Config.ADA_HANDLE_POLICY_ID + name.hexEncoded
            val api = buildBlockfrostApi(env)
            val resp = api.getAssetHolders(unit, 100, "desc")
            if (!resp.isSuccessful) return@withContext Result.success(null)
            val holder = resp.body()
                ?.firstOrNull { (it.quantity.toLongOrNull() ?: 0L) > 0L }
                ?: return@withContext Result.success(null)
            val address = holder.address
            handleCache[name] = address to (System.currentTimeMillis() + 3_600_000L)
            Result.success(address)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Stake address ────────────────────────────────────────────

    override suspend fun fetchStakeAddress(
        paymentAddress: String,
        projectId: String,
        env: AppEnvironment,
    ): Result<String?> = withContext(Dispatchers.IO) {
        if (cachedStakeAddress != null) return@withContext Result.success(cachedStakeAddress)
        try {
            val api = buildBlockfrostApi(env)
            val resp = api.getAddressInfo(paymentAddress)
            val stake = if (resp.isSuccessful) resp.body()?.stakeAddress else null
            cachedStakeAddress = stake
            Result.success(stake)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── NFTs ─────────────────────────────────────────────────────

    override suspend fun fetchNFTs(
        walletAddress: String,
        projectId: String,
        env: AppEnvironment,
    ): Result<List<NFT>> = withContext(Dispatchers.IO) {
        try {
            val backendService = buildBackendService(env)
            // In bloxbean 0.7.x, getUtxos returns List<Utxo> directly
            val utxos = fetchUtxosForAddress(backendService, walletAddress)

            val nfts = mutableListOf<NFT>()
            val api = buildBlockfrostApi(env)

            for (utxo in utxos) {
                for (amount in utxo.amount) {
                    if (amount.unit == "lovelace") continue
                    val metaResp = api.getAssetMetadata(amount.unit)
                    if (!metaResp.isSuccessful) continue
                    val meta = metaResp.body()?.onchainMetadata ?: continue
                    val name = (meta["name"] as? String) ?: amount.unit.takeLast(20)
                    val imageUrl = meta["image"] as? String
                    val ipfsUrl = imageUrl?.replace("ipfs://", "https://ipfs.io/ipfs/")
                    val description = meta["description"] as? String
                    nfts.add(
                        NFT(
                            id = amount.unit,
                            name = name,
                            imageURL = ipfsUrl,
                            description = description,
                            traits = null,
                        ),
                    )
                }
            }
            Result.success(nfts)
        } catch (e: Exception) {
            Log.e(TAG, "fetchNFTs failed: ${e.message}")
            Result.failure(e)
        }
    }
}
