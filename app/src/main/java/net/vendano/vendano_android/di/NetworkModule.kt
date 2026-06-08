package net.vendano.vendano_android.di

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.vendano.vendano_android.data.remote.BlockfrostApi
import net.vendano.vendano_android.data.remote.CoinbaseApi
import net.vendano.vendano_android.domain.model.AppEnvironment
import net.vendano.vendano_android.util.Config
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BlockfrostRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoinbaseRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson() = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    @BlockfrostRetrofit
    fun provideBlockfrostOkHttp(
        logging: HttpLoggingInterceptor,
    ): OkHttpClient {
        // The project_id header is injected at the repository level so we
        // can swap environments at runtime without rebuilding the client.
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @CoinbaseRetrofit
    fun provideCoinbaseOkHttp(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    @BlockfrostRetrofit
    fun provideBlockfrostRetrofit(
        @BlockfrostRetrofit client: OkHttpClient,
        gson: com.google.gson.Gson,
    ): Retrofit {
        // Base URL defaults to mainnet; the repository adds the project_id header per-request.
        val baseUrl = Config.blockfrostBaseUrl(AppEnvironment.MAINNET)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @CoinbaseRetrofit
    fun provideCoinbaseRetrofit(
        @CoinbaseRetrofit client: OkHttpClient,
        gson: com.google.gson.Gson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(Config.COINBASE_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideBlockfrostApi(@BlockfrostRetrofit retrofit: Retrofit): BlockfrostApi =
        retrofit.create(BlockfrostApi::class.java)

    @Provides
    @Singleton
    fun provideCoinbaseApi(@CoinbaseRetrofit retrofit: Retrofit): CoinbaseApi =
        retrofit.create(CoinbaseApi::class.java)
}
