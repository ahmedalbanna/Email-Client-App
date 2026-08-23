package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Phase 2.1 — Encrypted credential storage.
 * Stores OAuth tokens / app-passwords per accountId.
 * Falls back to plain SharedPreferences on devices where
 * EncryptedSharedPreferences is unavailable (e.g. Robolectric).
 */
class CredentialStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "email_credentials_enc",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for tests / devices without Keystore
            context.getSharedPreferences("email_credentials_fallback", Context.MODE_PRIVATE)
        }
    }

    private fun key(accountId: Int, field: String) = "acc_${accountId}_$field"

    fun saveAuthToken(accountId: Int, token: String) {
        prefs.edit().putString(key(accountId, "token"), token).apply()
    }

    fun getAuthToken(accountId: Int): String? =
        prefs.getString(key(accountId, "token"), null)

    fun saveRefreshToken(accountId: Int, refreshToken: String) {
        prefs.edit().putString(key(accountId, "refresh"), refreshToken).apply()
    }

    fun getRefreshToken(accountId: Int): String? =
        prefs.getString(key(accountId, "refresh"), null)

    fun saveAppPassword(accountId: Int, password: String) {
        prefs.edit().putString(key(accountId, "app_pw"), password).apply()
    }

    fun getAppPassword(accountId: Int): String? =
        prefs.getString(key(accountId, "app_pw"), null)

    fun clearCredentials(accountId: Int) {
        prefs.edit()
            .remove(key(accountId, "token"))
            .remove(key(accountId, "refresh"))
            .remove(key(accountId, "app_pw"))
            .apply()
    }

    fun hasCredentials(accountId: Int): Boolean =
        getAuthToken(accountId) != null || getAppPassword(accountId) != null
}
