package com.localtube.app.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.dataStore by preferencesDataStore(name = "localtube_settings")

data class AppSettings(
    val hasPin: Boolean,
    val dailyLimitMinutes: Int,
    val bedtimeEnabled: Boolean,
    val bedtimeStartMinutes: Int,
    val bedtimeEndMinutes: Int,
    val setupComplete: Boolean,
)

/**
 * Parent settings and the gate PIN.
 *
 * NOTE: the PIN is stored as a salted SHA-256 digest. DECISIONS.md calls for an
 * Android Keystore–backed hash for production; that is a planned hardening step.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val DAILY_LIMIT = intPreferencesKey("daily_limit_minutes")
        val BEDTIME_ON = booleanPreferencesKey("bedtime_enabled")
        val BEDTIME_START = intPreferencesKey("bedtime_start_minutes")
        val BEDTIME_END = intPreferencesKey("bedtime_end_minutes")
        val SETUP_DONE = booleanPreferencesKey("setup_complete")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            hasPin = p[Keys.PIN_HASH] != null,
            dailyLimitMinutes = p[Keys.DAILY_LIMIT] ?: 120,
            bedtimeEnabled = p[Keys.BEDTIME_ON] ?: false,
            bedtimeStartMinutes = p[Keys.BEDTIME_START] ?: (19 * 60 + 30),
            bedtimeEndMinutes = p[Keys.BEDTIME_END] ?: (7 * 60),
            setupComplete = p[Keys.SETUP_DONE] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        context.dataStore.edit {
            it[Keys.PIN_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
            it[Keys.PIN_HASH] = Base64.encodeToString(hash, Base64.NO_WRAP)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val p = context.dataStore.data.first()
        val saltB64 = p[Keys.PIN_SALT] ?: return false
        val hashB64 = p[Keys.PIN_HASH] ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        return hash(pin, salt).contentEquals(expected)
    }

    suspend fun setDailyLimit(minutes: Int) =
        context.dataStore.edit { it[Keys.DAILY_LIMIT] = minutes.coerceIn(0, 24 * 60) }

    suspend fun setBedtime(enabled: Boolean, startMinutes: Int, endMinutes: Int) =
        context.dataStore.edit {
            it[Keys.BEDTIME_ON] = enabled
            it[Keys.BEDTIME_START] = startMinutes
            it[Keys.BEDTIME_END] = endMinutes
        }

    suspend fun setSetupComplete() =
        context.dataStore.edit { it[Keys.SETUP_DONE] = true }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(pin.toByteArray(Charsets.UTF_8))
    }
}
