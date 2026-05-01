package com.yourcompany.kmptemplate.core.domain

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.yourcompany.kmptemplate.core.data.local.appContext

actual class TokenProviderImpl actual constructor() : TokenProvider {

    private val prefs by lazy {
        val ctx = checkNotNull(appContext) {
            "Assign `appContext` in Application.onCreate() before TokenProvider is first used"
        }
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx,
            "secure_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    override suspend fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)
    override suspend fun getExpiresAt(): Long = prefs.getLong(KEY_EXPIRES, 0L)

    override suspend fun saveTokens(access: String, refresh: String, expiresAt: Long) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .putLong(KEY_EXPIRES, expiresAt)
            .apply()
    }

    override suspend fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_EXPIRES)
            .apply()
    }

    private companion object {
        const val KEY_ACCESS = "auth_access_token"
        const val KEY_REFRESH = "auth_refresh_token"
        const val KEY_EXPIRES = "auth_expires_at"
    }
}
