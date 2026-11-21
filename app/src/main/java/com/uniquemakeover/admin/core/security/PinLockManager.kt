package com.uniquemakeover.admin.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.uniquemakeover.admin.core.util.TimeProvider
import com.uniquemakeover.admin.feature.auth.domain.model.LockoutState
import com.uniquemakeover.admin.feature.auth.domain.model.PinResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_FILE = "unique_makeover_secure_prefs"
private const val KEY_PIN_HASH = "key_pin_hash"
private const val MASTER_KEY_ALIAS = "UniqueMakeoverMasterKey"
private const val TICK_INTERVAL_MILLIS = 1000L
const val DEFAULT_PIN = "1234"

val KEY_FAIL_COUNT = intPreferencesKey("failed_pin_attempts")
val KEY_LOCKOUT_TIMESTAMP = longPreferencesKey("lockout_until_elapsed_realtime")

@Singleton
class PinLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pinHasher: PinHasher,
    private val lockoutPolicy: LockoutPolicy,
    private val timeProvider: TimeProvider,
    private val dataStore: DataStore<Preferences>
) {
    private val prefs: SharedPreferences by lazy {
        createSecurePreferences()
    }

    private fun createSecurePreferences(): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    suspend fun verifyPin(pin: String): PinResult {
        val currentElapsed = timeProvider.elapsedRealtime()
        val preferences = dataStore.data.first()
        val failCount = preferences[KEY_FAIL_COUNT] ?: 0
        val lastFailElapsed = preferences[KEY_LOCKOUT_TIMESTAMP] ?: 0L

        val lockout = lockoutPolicy.evaluate(failCount, lastFailElapsed, currentElapsed)
        if (lockout is LockoutState.Locked) {
            return PinResult.Locked(lockout.remainingSeconds)
        }

        val stored = getOrInitStoredPinHash()
        val isCorrect = pinHasher.verifyPin(pin, stored)

        return if (isCorrect) {
            resetFailCount()
            PinResult.Success
        } else {
            handleFailedAttempt(failCount, currentElapsed)
        }
    }

    private suspend fun handleFailedAttempt(
        currentFailCount: Int,
        currentElapsed: Long
    ): PinResult {
        val effectiveCount = if (currentFailCount >= LockoutPolicy.MAX_FAIL_ATTEMPTS) 0 else currentFailCount
        val newFailCount = effectiveCount + 1
        dataStore.edit { preferences ->
            preferences[KEY_FAIL_COUNT] = newFailCount
            if (newFailCount >= LockoutPolicy.MAX_FAIL_ATTEMPTS) {
                preferences[KEY_LOCKOUT_TIMESTAMP] = currentElapsed
            }
        }

        return if (newFailCount >= LockoutPolicy.MAX_FAIL_ATTEMPTS) {
            PinResult.Locked(LockoutPolicy.LOCKOUT_SECONDS)
        } else {
            PinResult.Invalid
        }
    }

    suspend fun resetFailCount() {
        dataStore.edit { preferences ->
            preferences[KEY_FAIL_COUNT] = 0
            preferences[KEY_LOCKOUT_TIMESTAMP] = 0L
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeLockoutState(): Flow<LockoutState> {
        return dataStore.data.flatMapLatest { preferences ->
            val failCount = preferences[KEY_FAIL_COUNT] ?: 0
            val lastFail = preferences[KEY_LOCKOUT_TIMESTAMP] ?: 0L
            val currentElapsed = timeProvider.elapsedRealtime()
            val initial = lockoutPolicy.evaluate(failCount, lastFail, currentElapsed)

            if (initial is LockoutState.Unlocked) {
                flowOf<LockoutState>(LockoutState.Unlocked)
            } else {
                flow<LockoutState> {
                    while (true) {
                        val now = timeProvider.elapsedRealtime()
                        val state = lockoutPolicy.evaluate(failCount, lastFail, now)
                        emit(state)
                        if (state is LockoutState.Unlocked) {
                            resetFailCount()
                            break
                        }
                        delay(TICK_INTERVAL_MILLIS)
                    }
                }
            }
        }
    }

    fun isDefaultPin(): Boolean {
        val stored = getOrInitStoredPinHash()
        return pinHasher.verifyPin(DEFAULT_PIN, stored)
    }

    fun setPin(newPin: String) {
        val hash = pinHasher.hashPin(newPin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    private fun getOrInitStoredPinHash(): String {
        val existing = prefs.getString(KEY_PIN_HASH, null)
        if (existing != null) return existing

        val defaultHash = pinHasher.hashPin(DEFAULT_PIN)
        prefs.edit().putString(KEY_PIN_HASH, defaultHash).apply()
        return defaultHash
    }
}
