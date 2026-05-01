package com.yourcompany.kmptemplate.auth.domain.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class OAuthToken(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long,
) {
    val isExpired: Boolean
        get() = Clock.System.now().toEpochMilliseconds() > expiresAt
}
