package com.yourcompany.kmptemplate.di

import com.yourcompany.kmptemplate.auth.data.repository.OAuthRepositoryImpl
import com.yourcompany.kmptemplate.auth.domain.port.OAuthFlowLauncher
import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.auth.domain.usecase.LoginUseCase
import com.yourcompany.kmptemplate.auth.domain.usecase.LogoutUseCase
import com.yourcompany.kmptemplate.auth.domain.usecase.RefreshTokenUseCase
import com.yourcompany.kmptemplate.core.data.network.NetworkClient
import com.yourcompany.kmptemplate.core.domain.TokenProvider
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class AuthModule {

    @Single
    fun provideOAuthRepository(networkClient: NetworkClient, tokenProvider: TokenProvider): OAuthRepository =
        OAuthRepositoryImpl(networkClient, tokenProvider)

    // OAuthFlowLauncher is registered per-platform when starting Koin
    // (AndroidOAuthFlowLauncher / IOSOAuthFlowLauncher)

    @Factory
    fun provideLoginUseCase(repository: OAuthRepository, launcher: OAuthFlowLauncher): LoginUseCase =
        LoginUseCase(repository, launcher)

    @Factory
    fun provideLogoutUseCase(repository: OAuthRepository): LogoutUseCase = LogoutUseCase(repository)

    @Factory
    fun provideRefreshTokenUseCase(repository: OAuthRepository): RefreshTokenUseCase = RefreshTokenUseCase(repository)
}
