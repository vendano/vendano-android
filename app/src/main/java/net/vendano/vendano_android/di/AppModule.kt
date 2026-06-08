package net.vendano.vendano_android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.vendano.vendano_android.data.local.db.VendanoDatabase
import net.vendano.vendano_android.data.local.db.TransactionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideVendanoDatabase(@ApplicationContext context: Context): VendanoDatabase =
        VendanoDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideTransactionDao(db: VendanoDatabase): TransactionDao =
        db.transactionDao()
}
