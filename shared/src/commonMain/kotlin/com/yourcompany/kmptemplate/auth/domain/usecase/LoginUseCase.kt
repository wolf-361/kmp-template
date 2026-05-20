package com.yourcompany.kmptemplate.auth.domain.usecase

import com.yourcompany.kmptemplate.auth.data.remote.pkce.PkceGenerator
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.domain.port.OAuthFlowLauncher
import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError

class LoginUseCase(private val repository: OAuthRepository, private val flowLauncher: OAuthFlowLauncher) {
    suspend operator fun invoke(provider: OAuthProvider, clientId: String): AppResult<Unit> {
        val verifier = PkceGenerator.generateVerifier()
        val challenge = PkceGenerator.generateChallenge(verifier)
        val redirectUri = "kmptemplate://oauth/${provider.name.lowercase()}/callback"

        val authUrl = buildString {
            append(provider.authorizationEndpoint)
            append("?response_type=code")
            append("&client_id=$clientId")
            append("&redirect_uri=$redirectUri")
            append("&scope=${provider.scopes.joinToString("%20")}")
            append("&code_challenge=$challenge")
            append("&code_challenge_method=S256")
        }

        return runCatching {
            val code = flowLauncher.launch(authUrl)
            repository.login(code = code, codeVerifier = verifier, provider = provider)
        }.getOrElse { AppResult.Failure(CoreError.Network.NoConnection) }
    }
}
