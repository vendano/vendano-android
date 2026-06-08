package net.vendano.vendano_android.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching recent transactions.
 * Mirrors the TxRowViewModel / RawTx data shape from iOS.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val txHash: String,
    val date: Long,             // epoch millis
    val blockHeight: Int,
    val outgoing: Boolean,
    val amountLovelace: Long,
    val counterpartyAddress: String,
    val counterpartyName: String?,
    val counterpartyAvatarUrl: String?,
    val balanceAfterLovelace: Long,
    val walletAddress: String,  // which wallet address this cache belongs to
)
