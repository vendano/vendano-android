package net.vendano.vendano_android.domain.cardano

import net.vendano.vendano_android.util.Config

/**
 * Stateless Cardano lovelace math utilities.
 * Android equivalent of iOS VendanoWalletMath.swift.
 */
object WalletMath {

    const val LOVELACE_PER_ADA: Double = 1_000_000.0
    const val MIN_FEE_OUTPUT_LOVELACE: ULong = 1_000_000uL

    fun adaToLovelace(ada: Double): ULong {
        if (ada <= 0) return 0uL
        return (ada * LOVELACE_PER_ADA).toULong()
    }

    fun lovelaceToAda(lovelace: ULong): Double =
        lovelace.toLong() / LOVELACE_PER_ADA

    fun lovelaceToAda(lovelace: Long): Double =
        lovelace / LOVELACE_PER_ADA

    /**
     * Vendano service fee in lovelace for a given send ADA amount.
     * Waives the fee if the result would be below the minimum UTxO threshold.
     */
    fun vendanoFeeLovelace(forSendAda: Double, percent: Double): ULong {
        if (forSendAda <= 0 || percent <= 0) return 0uL
        val lovelace = adaToLovelace(forSendAda * percent)
        return if (lovelace >= MIN_FEE_OUTPUT_LOVELACE) lovelace else 0uL
    }

    fun vendanoFeeLovelace(forSendLovelace: ULong, percent: Double): ULong =
        vendanoFeeLovelace(lovelaceToAda(forSendLovelace), percent)

    fun vendanoFeeAda(forSendAda: Double, percent: Double): Double =
        lovelaceToAda(vendanoFeeLovelace(forSendAda, percent))

    /** App-level Vendano fee using the global fee percent. */
    fun effectiveAppFeeAda(sendAda: Double): Double =
        vendanoFeeAda(sendAda, Config.VENDANO_APP_FEE_PERCENT)
}
