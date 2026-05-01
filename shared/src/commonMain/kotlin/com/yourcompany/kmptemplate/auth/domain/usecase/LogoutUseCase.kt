package com.yourcompany.kmptemplate.auth.domain.usecase

import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository

class LogoutUseCase(private val repository: OAuthRepository) {
    // Clears tokens only — does NOT wipe the local database
    suspend operator fun invoke() = repository.logout()
}
