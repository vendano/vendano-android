package net.vendano.vendano_android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Blockfrost REST API response DTOs.
 * These match the shapes used in iOS WalletService.swift.
 */

// GET /addresses/{address}/transactions
data class AddressTxDto(
    @SerializedName("tx_hash") val txHash: String,
)

// GET /txs/{hash}/utxos
data class TxUtxoResponseDto(
    val inputs: List<TxUtxoEntryDto>,
    val outputs: List<TxUtxoEntryDto>,
)

data class TxUtxoEntryDto(
    val address: String,
    val amount: List<TxAmountDto>,
)

data class TxAmountDto(
    val unit: String,       // "lovelace" or asset unit
    val quantity: String,   // decimal string
)

// GET /txs/{hash}
data class TxInfoDto(
    @SerializedName("block_time") val blockTime: Long,
    @SerializedName("block_height") val blockHeight: Int,
)

// GET /accounts/{stakeAddress}
data class AccountInfoDto(
    @SerializedName("controlled_amount") val controlledAmount: String,
)

// GET /accounts/{stakeAddress}/addresses/assets
data class AssetRowDto(
    val unit: String,
    val quantity: String,
)

// GET /addresses/{address}
data class AddressInfoDto(
    @SerializedName("stake_address") val stakeAddress: String?,
)

// GET /assets/{unit}/addresses
data class AssetHolderDto(
    val address: String,
    val quantity: String,
)

// Coinbase price API
data class CoinbasePriceResponse(
    val data: CoinbasePriceData,
)

data class CoinbasePriceData(
    val amount: String,
)

// Blockfrost metadata for NFT
data class AssetMetadataDto(
    @SerializedName("onchain_metadata") val onchainMetadata: Map<String, Any>?,
    val metadata: Map<String, Any>?,
)
