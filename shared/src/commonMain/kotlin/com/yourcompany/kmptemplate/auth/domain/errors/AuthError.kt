package com.yourcompany.kmptemplate.auth.domain.errors

import com.yourcompany.kmptemplate.core.domain.AppError

sealed interface AuthError : AppError {
    data object OAuthCancelled : AuthError
    data object OAuthProviderError : AuthError
}
