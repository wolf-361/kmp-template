package com.yourcompany.kmptemplate.auth.domain.model

// TODO: clientId values come from BuildKonfig (per-platform, per-environment)
enum class OAuthProvider(val authorizationEndpoint: String, val scopes: List<String>) {
    GOOGLE(
        authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
        scopes = listOf("openid", "email", "profile"),
    ),
    APPLE(
        authorizationEndpoint = "https://appleid.apple.com/auth/authorize",
        scopes = listOf("name", "email"),
    ),
    MICROSOFT(
        authorizationEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
        scopes = listOf("openid", "email", "profile"),
    ),
    GITHUB(
        authorizationEndpoint = "https://github.com/login/oauth/authorize",
        scopes = listOf("user:email"),
    ),
}
