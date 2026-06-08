package net.vendano.vendano_android.util

import net.vendano.vendano_android.BuildConfig
import net.vendano.vendano_android.domain.model.AppEnvironment

/**
 * Central configuration object. Mirrors iOS Config.swift.
 *
 * Build-time constants come from BuildConfig fields set in app/build.gradle.kts.
 * In production, inject real values via local.properties (gitignored) or CI secrets.
 */
object Config {

    const val VENDANO_APP_FEE_PERCENT: Double = 0.01
    const val VENDANO_APP_FEE_PERCENT_FORMATTED: String = "1%"

    // Minimum lovelace output to warrant a Vendano fee UTxO (1 ADA)
    const val MIN_FEE_OUTPUT_LOVELACE: ULong = 1_000_000uL

    // HOSKY native asset policy ID
    const val HOSKY_POLICY_ID = "a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235"
    const val ADA_HANDLE_POLICY_ID = "f0ff48bbb7bbe9d59a40f1ce90e9e9d0ff5002ec48f232b49ca0fb9a"

    fun blockfrostBaseUrl(env: AppEnvironment): String = when {
        env.isMainnet -> "https://cardano-mainnet.blockfrost.io/api/v0/"
        else -> "https://cardano-preprod.blockfrost.io/api/v0/"
    }

    fun blockfrostKey(env: AppEnvironment): String = when (env) {
        AppEnvironment.TESTNET -> BuildConfig.BLOCKFROST_KEY_TESTNET
            .ifBlank { BuildConfig.BLOCKFROST_KEY }
        else -> BuildConfig.BLOCKFROST_KEY
    }

    fun vendanoFeeAddress(env: AppEnvironment): String = when {
        env == AppEnvironment.TESTNET -> BuildConfig.VENDANO_WALLET_TESTNET
        else -> BuildConfig.VENDANO_WALLET
    }

    fun vendanoDeveloperAddress(env: AppEnvironment): String = when {
        env == AppEnvironment.TESTNET -> BuildConfig.DEV_WALLET_TESTNET
        else -> BuildConfig.DEV_WALLET
    }

    fun resolveEnvironment(identifier: String): AppEnvironment {
        return when {
            identifier.lowercase() == "apple@vendano.net" -> AppEnvironment.APP_STORE_REVIEW
            identifier.lowercase().endsWith("@test.vendano.net") -> AppEnvironment.TESTNET
            else -> AppEnvironment.MAINNET
        }
    }

    const val COINBASE_BASE_URL = "https://api.coinbase.com/v2/"
}
