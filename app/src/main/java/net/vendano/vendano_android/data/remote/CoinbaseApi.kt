package net.vendano.vendano_android.data.remote

import net.vendano.vendano_android.data.remote.dto.CoinbasePriceResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for Coinbase public price API.
 * Mirrors iOS CoinbaseService.swift.
 */
interface CoinbaseApi {

    @GET("prices/{pair}/spot")
    suspend fun getSpotPrice(
        @Path("pair") pair: String,   // e.g. "ADA-USD"
    ): Response<CoinbasePriceResponse>
}
