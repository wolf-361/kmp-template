package com.yourcompany.kmptemplate.auth.data.repository

import com.yourcompany.kmptemplate.auth.data.remote.AuthRoutes
import com.yourcompany.kmptemplate.auth.data.remote.dto.AuthResponse
import com.yourcompany.kmptemplate.auth.data.remote.dto.OAuthRequest
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.domain.model.OAuthToken
import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.core.data.network.NetworkClient
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import com.yourcompany.kmptemplate.core.domain.TokenProvider
import com.yourcompany.kmptemplate.core.domain.toUnit
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

class OAuthRepositoryImpl(private val networkClient: NetworkClient, private val tokenProvider: TokenProvider) :
    OAuthRepository {

    private val _token = MutableStateFlow<OAuthToken?>(null)
    override val token: Flow<OAuthToken?> = _token

    init {
        CoroutineScope(Dispatchers.Default).launch { bootstrapToken() }
    }

    override suspend fun login(code: String, codeVerifier: String, provider: OAuthProvider): AppResult<Unit> =
        networkClient.unauthorizedRequest<AuthResponse> {
            method = HttpMethod.Post
            url(AuthRoutes.OAUTH)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                OAuthRequest(
                    code = code,
                    provider = provider.name.lowercase(),
                    codeVerifier = codeVerifier,
                    redirectUri = redirectUri(provider),
                ),
            )
        }.also { result ->
            if (result is AppResult.Success) persistToken(result.data)
        }.toUnit()

    // Clears tokens only — local database is intentionally untouched (see ADR-003)
    override suspend fun logout(): AppResult<Unit> {
        tokenProvider.clearTokens()
        _token.value = null
        return AppResult.Success(Unit)
    }

    override suspend fun refreshToken(): AppResult<Unit> {
        if (tokenProvider.getRefreshToken() == null) return AppResult.Failure(CoreError.Unauthorized)
        // NetworkClientImpl handles the actual refresh call via tryRefreshTokens().
        // Force a request that will hit 401 → trigger the refresh flow.
        return networkClient.request<Unit> { url("/auth/me") }
    }

    private suspend fun bootstrapToken() {
        val access = tokenProvider.getAccessToken() ?: return
        val refresh = tokenProvider.getRefreshToken()
        val expiresAt = tokenProvider.getExpiresAt()
        _token.value = OAuthToken(access, refresh, expiresAt)
    }

    private suspend fun persistToken(response: AuthResponse) {
        val expiresAt = Clock.System.now().toEpochMilliseconds() + response.expiresIn * 1_000L
        tokenProvider.saveTokens(response.accessToken, response.refreshToken, expiresAt)
        _token.value = OAuthToken(response.accessToken, response.refreshToken, expiresAt)
    }

    private fun redirectUri(provider: OAuthProvider): String =
        "kmptemplate://oauth/${provider.name.lowercase()}/callback"
}
