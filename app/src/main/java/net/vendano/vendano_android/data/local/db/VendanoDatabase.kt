package net.vendano.vendano_android.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for Vendano.
 * Android equivalent of a lightweight persistence layer (iOS had no CoreData – this
 * is used only for transaction history caching to survive process kills and
 * to avoid Blockfrost round-trips on every home screen open).
 */
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class VendanoDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DATABASE_NAME = "vendano.db"

        @Volatile
        private var instance: VendanoDatabase? = null

        fun getInstance(context: Context): VendanoDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VendanoDatabase::class.java,
                    DATABASE_NAME,
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
