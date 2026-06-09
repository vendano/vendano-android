package net.vendano.vendano_android.data.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted secure storage backed by EncryptedSharedPreferences.
 * Android equivalent of iOS KeychainWrapper.standard.
 *
 * Stores seed words JSON so the wallet can be restored across app launches.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "vendano_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    // ─── Seed words ────────────────────────────────────────────

    fun saveSeedWords(words: List<String>) {
        // Use commit() (synchronous) rather than apply() (async) so the seed is
        // durably written before control returns to the caller. apply() queues the write
        // on a background thread: if the process is killed (battery death, OS kill) in the
        // window between apply() returning and the write completing, the seed is lost and
        // the wallet becomes permanently unrecoverable.
        prefs.edit().putString(KEY_SEED_WORDS, gson.toJson(words)).commit()
    }

    fun loadSeedWords(): List<String>? {
        val json = prefs.getString(KEY_SEED_WORDS, null) ?: return null
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun clearSeedWords() {
        prefs.edit().remove(KEY_SEED_WORDS).apply()
    }

    fun hasSeedWords(): Boolean = prefs.contains(KEY_SEED_WORDS)

    // ─── Generic helpers ────────────────────────────────────────

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SEED_WORDS = "seedWords"
    }
}
