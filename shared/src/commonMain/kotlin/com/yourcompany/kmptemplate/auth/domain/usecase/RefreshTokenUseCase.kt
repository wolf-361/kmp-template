package com.yourcompany.kmptemplate.auth.domain.usecase

import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.core.domain.AppResult

class RefreshTokenUseCase(private val repository: OAuthRepository) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refreshToken()
}
