package com.yourcompany.kmptemplate.core.data.network

// TODO: Replace with BuildKonfig values per environment (debug / staging / prod)
object NetworkConfig {
    const val BASE_URL = "https://api.example.com"
    const val TOKEN_REFRESH_PATH = "/auth/refresh"
    const val REQUEST_TIMEOUT_MS = 30_000L
    const val CONNECT_TIMEOUT_MS = 10_000L
}
