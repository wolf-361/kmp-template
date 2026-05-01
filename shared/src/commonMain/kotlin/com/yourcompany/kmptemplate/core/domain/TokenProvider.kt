package com.yourcompany.kmptemplate.core.domain

interface TokenProvider {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getExpiresAt(): Long
    suspend fun saveTokens(access: String, refresh: String, expiresAt: Long)
    suspend fun clearTokens()
}

expect class TokenProviderImpl() : TokenProvider
