package com.uniquemakeover.admin.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_FILE = "unique_makeover_secure_prefs"
private const val KEY_PIN_HASH = "key_pin_hash"
private const val MASTER_KEY_ALIAS = "UniqueMakeoverMasterKey"
const val DEFAULT_PIN = "1234"

@Singleton
class PinLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pinHasher: PinHasher
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
            // Fallback for Robolectric or corrupted Keystore state (EC-AUTH-01.4)
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    fun verifyPin(pin: String): Boolean {
        val stored = getOrInitStoredPinHash()
        return pinHasher.verifyPin(pin, stored)
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
