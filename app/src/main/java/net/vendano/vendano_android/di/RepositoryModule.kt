package net.vendano.vendano_android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.vendano.vendano_android.domain.repository.WalletRepository
import net.vendano.vendano_android.domain.repository.WalletRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        impl: WalletRepositoryImpl,
    ): WalletRepository
}
