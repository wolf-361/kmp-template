package com.yourcompany.kmptemplate.auth.domain.usecase

import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.core.domain.AppResult

open class RefreshTokenUseCase(private val repository: OAuthRepository) {
    open suspend operator fun invoke(): AppResult<Unit> = repository.refreshToken()
}
