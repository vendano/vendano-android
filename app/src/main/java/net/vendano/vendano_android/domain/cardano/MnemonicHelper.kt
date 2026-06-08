package net.vendano.vendano_android.domain.cardano

import net.vendano.vendano_android.domain.model.MnemonicLanguage

/**
 * BIP39 mnemonic utilities.
 * Android equivalent of iOS MnemonicText.swift + MnemonicLanguage.swift.
 *
 * The bloxbean cardano-client-lib includes BIP39 support via
 * com.bloxbean.cardano.client.crypto.bip39.MnemonicCode.
 */
object MnemonicHelper {

    /**
     * Normalize raw mnemonic input: NFKD, lowercase, split on whitespace.
     * Mirrors iOS MnemonicText.tokenize().
     */
    fun tokenize(raw: String): List<String> {
        val normalized = raw
            .replace("\u3000", " ")        // CJK ideographic space → ASCII space
            .lowercase()
        return normalized
            .split(Regex("\\s+"))
            .map { it.trim().trimStart('\'', '\u2018', '\u2019', '\u201c', '\u201d') }
            .filter { it.isNotEmpty() }
    }

    /**
     * Validate that a list of words matches a known BIP39 word list length.
     * Full checksum validation is done by bloxbean during wallet creation.
     */
    fun isPlausibleMnemonic(words: List<String>): Boolean =
        words.size in listOf(12, 15, 18, 21, 24)

    /**
     * Resolve the display separator for a language.
     */
    fun displaySeparator(language: MnemonicLanguage): String =
        if (language == MnemonicLanguage.JAPANESE) "\u3000" else " "

    /**
     * Join words with the appropriate separator for display/storage.
     */
    fun joinWords(words: List<String>, language: MnemonicLanguage = MnemonicLanguage.ENGLISH): String =
        words.joinToString(displaySeparator(language))
}
