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
     * Atomically replaces all cached transactions for a wallet. The @Transaction
     * annotation wraps both the DELETE and the INSERT in a single SQLite transaction
     * so a failure between the two operations cannot leave the table empty.
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
