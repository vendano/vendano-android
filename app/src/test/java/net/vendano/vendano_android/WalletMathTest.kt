package net.vendano.vendano_android

import net.vendano.vendano_android.domain.cardano.WalletMath
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [WalletMath] — the only fully pure utility class in the codebase.
 *
 * These tests deliberately have no Android / Firebase dependencies so they run
 * on the local JVM without an emulator.
 */
class WalletMathTest {

    // ─── ADA ↔ lovelace conversions ──────────────────────────────────────────

    @Test
    fun `adaToLovelace converts whole ADA amounts correctly`() {
        assertEquals(1_000_000uL, WalletMath.adaToLovelace(1.0))
        assertEquals(5_000_000uL, WalletMath.adaToLovelace(5.0))
        assertEquals(1_000_000_000uL, WalletMath.adaToLovelace(1000.0))
    }

    @Test
    fun `adaToLovelace returns 0 for non-positive input`() {
        assertEquals(0uL, WalletMath.adaToLovelace(0.0))
        assertEquals(0uL, WalletMath.adaToLovelace(-1.0))
    }

    @Test
    fun `lovelaceToAda is the inverse of adaToLovelace for whole ADA`() {
        val ada = 42.0
        val roundtrip = WalletMath.lovelaceToAda(WalletMath.adaToLovelace(ada))
        assertEquals(ada, roundtrip, 1e-9)
    }

    @Test
    fun `lovelaceToAda handles fractional ADA`() {
        // 1.5 ADA = 1_500_000 lovelace
        assertEquals(1.5, WalletMath.lovelaceToAda(1_500_000uL), 1e-9)
    }

    // ─── Vendano fee logic ────────────────────────────────────────────────────

    @Test
    fun `vendanoFeeLovelace returns 0 when send amount is 0`() {
        assertEquals(0uL, WalletMath.vendanoFeeLovelace(0.0, 0.01))
    }

    @Test
    fun `vendanoFeeLovelace returns 0 when fee percent is 0`() {
        assertEquals(0uL, WalletMath.vendanoFeeLovelace(10.0, 0.0))
    }

    @Test
    fun `vendanoFeeLovelace waives fee when result is below MIN_FEE_OUTPUT`() {
        // 0.5 ADA × 1 % = 0.005 ADA = 5_000 lovelace < 1_000_000 (MIN_FEE_OUTPUT) → waived
        assertEquals(0uL, WalletMath.vendanoFeeLovelace(0.5, 0.01))
    }

    @Test
    fun `vendanoFeeLovelace collects fee when result meets MIN_FEE_OUTPUT`() {
        // 200 ADA × 1 % = 2 ADA = 2_000_000 lovelace ≥ 1_000_000 → collected
        val fee = WalletMath.vendanoFeeLovelace(200.0, 0.01)
        assertTrue("Expected fee ≥ 1_000_000, got $fee", fee >= 1_000_000uL)
    }

    @Test
    fun `vendanoFeeAda matches lovelace overload`() {
        val sendAda = 150.0
        val percent = 0.02
        val viaAda = WalletMath.vendanoFeeAda(sendAda, percent)
        val viaLovelace = WalletMath.lovelaceToAda(
            WalletMath.vendanoFeeLovelace(sendAda, percent)
        )
        assertEquals(viaLovelace, viaAda, 1e-9)
    }

}
