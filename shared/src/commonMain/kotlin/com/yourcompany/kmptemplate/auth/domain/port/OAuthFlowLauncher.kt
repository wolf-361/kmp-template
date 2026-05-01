package com.yourcompany.kmptemplate.auth.domain.port

interface OAuthFlowLauncher {
    // Launches the platform-specific OAuth browser flow and returns the authorization code
    // from the redirect URI. Implementations: AndroidOAuthFlowLauncher (Custom Tabs),
    // IOSOAuthFlowLauncher (ASWebAuthenticationSession).
    suspend fun launch(authUrl: String): String
}
