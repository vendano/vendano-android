package net.vendano.vendano_android.util

import java.security.MessageDigest
import java.util.Base64

/**
 * Extension functions shared across the app. Mirrors iOS String-extension.swift etc.
 */

/** SHA-256 hash of a UTF-8 string, returned as Base64 – used for Firestore handle lookups. */
fun String.sha256Base64(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(this.lowercase().toByteArray(Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(hash)
}

/** Encode string to hex bytes – used for ADA Handle and HOSKY unit construction. */
val String.hexEncoded: String
    get() = toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }

/** Strip all non-digit characters (for phone number normalization). */
fun String.digitsOnly(): String = filter { it.isDigit() }

/** Sanitize a decimal input string: allow only digits and one decimal point. */
fun String.sanitizeDecimal(maxFractionDigits: Int = 6): String {
    var filtered = filter { it.isDigit() || it == '.' }
    val parts = filtered.split('.')
    if (parts.size > 1) {
        val intPart = parts[0]
        var fracPart = parts[1]
        if (fracPart.length > maxFractionDigits) {
            fracPart = fracPart.take(maxFractionDigits)
        }
        filtered = "$intPart.$fracPart"
    }
    if (filtered.startsWith('.')) filtered = "0$filtered"
    return filtered
}

/** Validate email with a simple @ + . check (mirrors iOS). */
fun String.isValidEmail(): Boolean = contains('@') && contains('.')

/** Check if string looks like a Cardano bech32 address. */
fun String.looksLikeCardanoAddress(): Boolean =
    startsWith("addr") || startsWith("stake")

/** Format double as ADA string with 2 decimal places. */
fun Double.formatAda(fractionDigits: Int = 2): String =
    "%.${fractionDigits}f".format(this)

/** Format lovelace Long to ADA string. */
fun ULong.lovelaceToAdaString(fractionDigits: Int = 6): String =
    (this.toLong() / 1_000_000.0).let { "%.${fractionDigits}f".format(it) }
