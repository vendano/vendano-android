package net.vendano.vendano_android.data.remote

import net.vendano.vendano_android.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for Blockfrost REST API.
 * Mirrors the URL patterns used in iOS WalletService.swift.
 */
interface BlockfrostApi {

    // ─── Addresses ────────────────────────────────────────────────

    @GET("addresses/{address}")
    suspend fun getAddressInfo(
        @Path("address") address: String,
    ): Response<AddressInfoDto>

    @GET("addresses/{address}/transactions")
    suspend fun getAddressTransactions(
        @Path("address") address: String,
        @Query("count") count: Int = 20,
        @Query("page") page: Int = 1,
        @Query("order") order: String = "desc",
    ): Response<List<AddressTxDto>>

    // ─── Transactions ─────────────────────────────────────────────

    @GET("txs/{hash}/utxos")
    suspend fun getTxUtxos(
        @Path("hash") hash: String,
    ): Response<TxUtxoResponseDto>

    @GET("txs/{hash}")
    suspend fun getTxInfo(
        @Path("hash") hash: String,
    ): Response<TxInfoDto>

    // ─── Accounts / Staking ───────────────────────────────────────

    @GET("accounts/{stakeAddress}")
    suspend fun getAccountInfo(
        @Path("stakeAddress") stakeAddress: String,
    ): Response<AccountInfoDto>

    @GET("accounts/{stakeAddress}/addresses/assets")
    suspend fun getAccountAssets(
        @Path("stakeAddress") stakeAddress: String,
        @Query("count") count: Int = 100,
    ): Response<List<AssetRowDto>>

    // ─── Assets / Handles ─────────────────────────────────────────

    @GET("assets/{unit}/addresses")
    suspend fun getAssetHolders(
        @Path("unit") unit: String,
        @Query("count") count: Int = 100,
        @Query("order") order: String = "desc",
    ): Response<List<AssetHolderDto>>

    @GET("assets/{unit}")
    suspend fun getAssetMetadata(
        @Path("unit") unit: String,
    ): Response<AssetMetadataDto>
}
