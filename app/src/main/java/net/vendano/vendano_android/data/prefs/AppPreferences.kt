package net.vendano.vendano_android.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vendano_prefs")

/**
 * DataStore-backed preferences.
 * Android equivalent of UserDefaults in iOS AppState.swift.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    // ─── Keys ────────────────────────────────────────────────────

    private object Keys {
        val FIAT_CURRENCY      = stringPreferencesKey("VendanoFiatCurrency")
        val STORE_NAME         = stringPreferencesKey("VendanoStoreName")
        val STORE_PRICING_CURRENCY = stringPreferencesKey("VendanoStoreDefaultPricingCurrency")
        val STORE_BUFFER       = doublePreferencesKey("VendanoStoreBufferPercent")
        val STORE_TIPS_ENABLED = booleanPreferencesKey("VendanoStoreTipsEnabled")
        val EXPERT_MODE        = booleanPreferencesKey("VendanoExpertMode")
        val APPEARANCE         = stringPreferencesKey("appearancePreference")
        val USE_HOSKY_THEME    = booleanPreferencesKey("useHoskyTheme")
        val NOTIFICATIONS_PRIMER_DISMISSED = booleanPreferencesKey("notifications_primer_dismissed")
        val DID_SHOW_SEND_AUTH_PRIMER = booleanPreferencesKey("didShowSendAuthPrimer")
        val AVATAR_DATA        = stringPreferencesKey("avatarBase64")  // base64-encoded jpeg
        val EMAIL_FOR_LINK     = stringPreferencesKey("VendanoEmailForLink")
        val PHONE_VID          = stringPreferencesKey("phoneVID")
        val PHONE_NUMBER       = stringPreferencesKey("phoneNumber")
        val REINSTALL_UID      = stringPreferencesKey("reinstallUID")
    }

    // ─── Flows ──────────────────────────────────────────────────

    val fiatCurrency: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.FIAT_CURRENCY] ?: "USD" }

    val storeName: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.STORE_NAME] ?: "" }

    val storeBufferPercent: Flow<Double> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.STORE_BUFFER] ?: 0.05 }

    val storeTipsEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.STORE_TIPS_ENABLED] ?: true }

    val expertMode: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.EXPERT_MODE] ?: false }

    val appearance: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.APPEARANCE] ?: "system" }

    val useHoskyTheme: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.USE_HOSKY_THEME] ?: false }

    val notificationPrimerDismissed: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.NOTIFICATIONS_PRIMER_DISMISSED] ?: false }

    val didShowSendAuthPrimer: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.DID_SHOW_SEND_AUTH_PRIMER] ?: false }

    val avatarBase64: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.AVATAR_DATA] }

    val emailForLink: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.EMAIL_FOR_LINK] }

    val phoneVid: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.PHONE_VID] }

    val phoneNumber: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.PHONE_NUMBER] }

    val reinstallUid: Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.REINSTALL_UID] }

    // ─── Setters ─────────────────────────────────────────────────

    suspend fun setFiatCurrency(code: String) = edit { it[Keys.FIAT_CURRENCY] = code }
    suspend fun setStoreName(name: String) = edit { it[Keys.STORE_NAME] = name }
    suspend fun setStoreBufferPercent(p: Double) = edit { it[Keys.STORE_BUFFER] = p }
    suspend fun setStoreTipsEnabled(v: Boolean) = edit { it[Keys.STORE_TIPS_ENABLED] = v }
    suspend fun setExpertMode(v: Boolean) = edit { it[Keys.EXPERT_MODE] = v }
    suspend fun setAppearance(v: String) = edit { it[Keys.APPEARANCE] = v }
    suspend fun setUseHoskyTheme(v: Boolean) = edit { it[Keys.USE_HOSKY_THEME] = v }
    suspend fun setNotificationPrimerDismissed(v: Boolean) = edit { it[Keys.NOTIFICATIONS_PRIMER_DISMISSED] = v }
    suspend fun setDidShowSendAuthPrimer(v: Boolean) = edit { it[Keys.DID_SHOW_SEND_AUTH_PRIMER] = v }
    suspend fun setAvatarBase64(v: String?) = edit {
        if (v != null) it[Keys.AVATAR_DATA] = v else it.remove(Keys.AVATAR_DATA)
    }
    suspend fun setEmailForLink(v: String?) = edit {
        if (v != null) it[Keys.EMAIL_FOR_LINK] = v else it.remove(Keys.EMAIL_FOR_LINK)
    }
    suspend fun setPhoneVid(v: String?) = edit {
        if (v != null) it[Keys.PHONE_VID] = v else it.remove(Keys.PHONE_VID)
    }
    suspend fun setPhoneNumber(v: String?) = edit {
        if (v != null) it[Keys.PHONE_NUMBER] = v else it.remove(Keys.PHONE_NUMBER)
    }
    suspend fun setReinstallUid(v: String?) = edit {
        if (v != null) it[Keys.REINSTALL_UID] = v else it.remove(Keys.REINSTALL_UID)
    }

    suspend fun clearAuthPrefs() = edit {
        it.remove(Keys.EMAIL_FOR_LINK)
        it.remove(Keys.PHONE_VID)
        it.remove(Keys.PHONE_NUMBER)
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }
}
