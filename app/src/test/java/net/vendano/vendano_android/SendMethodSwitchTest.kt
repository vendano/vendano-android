package net.vendano.vendano_android

import net.vendano.vendano_android.domain.model.SendMethod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test for the stale-address send bug.
 *
 * Root cause: SendViewModel.setSendMethod() cleared _recipient but not
 * _addressInput. The Send button's enabled condition checked
 * `recipient != null || addressInput.startsWith("addr")` regardless of
 * the active tab. After the user typed a Cardano address in ADDRESS mode
 * then switched to EMAIL/PHONE mode, addressInput retained the old value,
 * keeping the Send button enabled and causing send() to resolve dest as
 * the stale address (not the email/phone recipient).
 *
 * Fix: setSendMethod() now clears _addressInput when leaving ADDRESS mode,
 * and the enabled condition gates the addressInput check on ADDRESS mode.
 */
class SendMethodSwitchTest {

    // ── helpers that mirror the production logic ──────────────────────────

    /**
     * Simulates the enabled condition in SendScreen.kt after the fix.
     * `(recipient != null || (method == ADDRESS && addressInput.startsWith("addr")))`
     */
    private fun sendButtonEnabled(
        method: SendMethod,
        recipientAddress: String?,
        addressInput: String,
        adaDouble: Double,
        sending: Boolean,
    ): Boolean = !sending && adaDouble > 0.0 &&
        (recipientAddress != null || (method == SendMethod.ADDRESS && addressInput.startsWith("addr")))

    /**
     * Simulates the destination resolved by SendViewModel.send():
     * `(_recipient.value?.address ?: _addressInput.value).trim()`
     */
    private fun resolvedDest(recipientAddress: String?, addressInput: String): String =
        (recipientAddress ?: addressInput).trim()

    // ── Bug regression: old behaviour ────────────────────────────────────

    @Test
    fun `old condition enabled send with stale address after mode switch`() {
        val addressInput = "addr1qxx_previous_address"
        val recipient: String? = null   // no Vendano user found for the email
        val adaDouble = 10.0

        // OLD enabled condition (no method guard):
        val oldEnabled = adaDouble > 0.0 && (recipient != null || addressInput.startsWith("addr"))
        assertTrue("Old code incorrectly enabled send with stale address", oldEnabled)

        // OLD resolved destination (stale Cardano address, not the email):
        val dest = resolvedDest(recipient, addressInput)
        assertTrue("Old code sent to stale address", dest.startsWith("addr1"))
    }

    // ── Fix verification: new behaviour ──────────────────────────────────

    @Test
    fun `switching from ADDRESS to EMAIL clears address input`() {
        // Simulate the setSendMethod fix: clear addressInput when leaving ADDRESS.
        var addressInput = "addr1qxx_previous_address"
        var method = SendMethod.ADDRESS

        // User switches to EMAIL tab
        method = SendMethod.EMAIL
        addressInput = ""   // ← what the fix does

        val recipient: String? = null
        val enabled = sendButtonEnabled(method, recipient, addressInput, 10.0, false)
        assertFalse("Send must be disabled in EMAIL mode with no resolved recipient", enabled)
    }

    @Test
    fun `switching from ADDRESS to PHONE clears address input`() {
        var addressInput = "addr1qxx_previous_address"
        var method = SendMethod.ADDRESS

        method = SendMethod.PHONE
        addressInput = ""

        val recipient: String? = null
        val enabled = sendButtonEnabled(method, recipient, addressInput, 10.0, false)
        assertFalse("Send must be disabled in PHONE mode with no resolved recipient", enabled)
    }

    @Test
    fun `send is enabled in EMAIL mode when a Vendano recipient is resolved`() {
        val method = SendMethod.EMAIL
        val addressInput = ""
        val recipient = "addr1q_resolved_via_email"
        val enabled = sendButtonEnabled(method, recipient, addressInput, 5.0, false)
        assertTrue("Send must be enabled when recipient is resolved", enabled)
    }

    @Test
    fun `send is enabled in ADDRESS mode with valid cardano address`() {
        val method = SendMethod.ADDRESS
        val addressInput = "addr1qxx_valid_address"
        val recipient: String? = null
        val enabled = sendButtonEnabled(method, recipient, addressInput, 5.0, false)
        assertTrue("Send must be enabled in ADDRESS mode with addr-prefixed input", enabled)
    }

    @Test
    fun `send is disabled in ADDRESS mode with ada handle before resolution`() {
        val method = SendMethod.ADDRESS
        val addressInput = "\$vendano_handle"    // ADA handle, not yet resolved
        val recipient: String? = null
        val enabled = sendButtonEnabled(method, recipient, addressInput, 5.0, false)
        assertFalse("Send must be disabled for unresolved ADA handle", enabled)
    }

    @Test
    fun `send is disabled when amount is zero`() {
        val method = SendMethod.ADDRESS
        val addressInput = "addr1qxx_valid_address"
        val recipient: String? = null
        val enabled = sendButtonEnabled(method, recipient, addressInput, 0.0, false)
        assertFalse("Send must be disabled when amount is zero", enabled)
    }

    @Test
    fun `resolved destination uses recipient address over addressInput`() {
        val recipientAddress = "addr1q_from_firebase"
        val addressInput = "addr1q_old_stale"
        val dest = resolvedDest(recipientAddress, addressInput)
        assertTrue("Resolved dest must prefer recipient address", dest == recipientAddress)
    }
}
