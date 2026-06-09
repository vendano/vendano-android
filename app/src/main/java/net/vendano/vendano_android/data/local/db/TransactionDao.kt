package net.vendano.vendano_android.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE walletAddress = :walletAddress ORDER BY blockHeight DESC, date DESC")
    fun observeTransactions(walletAddress: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE walletAddress = :walletAddress ORDER BY blockHeight DESC, date DESC LIMIT :limit")
    suspend fun getRecentTransactions(walletAddress: String, limit: Int = 20): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE walletAddress = :walletAddress")
    suspend fun clearForWallet(walletAddress: String)

    /**
     * Atomically replace all cached transactions for a wallet.
     *
     * Previously, callers called clearForWallet() followed by insertAll() as two
     * separate DAO calls without a wrapping transaction. A process kill or concurrent
     * refresh between those two calls would leave the cache permanently empty.
     * Using @Transaction guarantees both operations succeed or both are rolled back.
     */
    @Transaction
    suspend fun replaceTransactions(walletAddress: String, transactions: List<TransactionEntity>) {
        clearForWallet(walletAddress)
        insertAll(transactions)
    }

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM transactions WHERE walletAddress = :walletAddress")
    suspend fun countForWallet(walletAddress: String): Int
}
