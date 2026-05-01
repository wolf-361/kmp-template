package com.yourcompany.kmptemplate.auth.domain.repository

import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.domain.model.OAuthToken
import com.yourcompany.kmptemplate.core.domain.AppResult
import kotlinx.coroutines.flow.Flow

interface OAuthRepository {
    val token: Flow<OAuthToken?>
    suspend fun login(code: String, codeVerifier: String, provider: OAuthProvider): AppResult<Unit>
    suspend fun logout(): AppResult<Unit>
    suspend fun refreshToken(): AppResult<Unit>
}
