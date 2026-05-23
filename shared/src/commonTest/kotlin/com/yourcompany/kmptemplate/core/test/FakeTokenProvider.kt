package com.yourcompany.kmptemplate.core.test

import com.yourcompany.kmptemplate.core.domain.TokenProvider

class FakeTokenProvider : TokenProvider {
    var accessToken: String? = null
    var refreshToken: String? = null
    var expiresAt: Long = 0L
    var cleared = false

    override suspend fun getAccessToken() = accessToken
    override suspend fun getRefreshToken() = refreshToken
    override suspend fun getExpiresAt() = expiresAt

    override suspend fun saveTokens(access: String, refresh: String, expiresAt: Long) {
        this.accessToken = access
        this.refreshToken = refresh
        this.expiresAt = expiresAt
    }

    override suspend fun clearTokens() {
        accessToken = null
        refreshToken = null
        cleared = true
    }
}
