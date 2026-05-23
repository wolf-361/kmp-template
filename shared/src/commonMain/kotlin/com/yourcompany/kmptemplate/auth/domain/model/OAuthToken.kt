package com.yourcompany.kmptemplate.auth.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class OAuthToken(val accessToken: String, val refreshToken: String?, val expiresAt: Long) {
    val isExpired: Boolean
        get() = Clock.System.now().toEpochMilliseconds() > expiresAt
}
