package com.yourcompany.kmptemplate.auth.data.repository

import app.cash.turbine.test
import com.yourcompany.kmptemplate.auth.data.remote.dto.AuthResponse
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import com.yourcompany.kmptemplate.core.test.FakeNetworkClient
import com.yourcompany.kmptemplate.core.test.FakeTokenProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class OAuthRepositoryImplTest {

    private val fakeNetworkClient = FakeNetworkClient()
    private val fakeTokenProvider = FakeTokenProvider()

    private fun createRepository() = OAuthRepositoryImpl(fakeNetworkClient, fakeTokenProvider)

    @Test
    fun `login success - tokens are persisted and result is Success`() = runTest {
        fakeNetworkClient.enqueue(
            AppResult.Success(AuthResponse("access123", "refresh456", expiresIn = 3600L)),
        )
        val repo = createRepository()

        val result = repo.login(code = "auth-code", codeVerifier = "verifier", provider = OAuthProvider.GOOGLE)

        result shouldBe AppResult.Success(Unit)
        fakeTokenProvider.accessToken shouldBe "access123"
        fakeTokenProvider.refreshToken shouldBe "refresh456"
    }

    @Test
    fun `login failure - result propagates and tokens are not saved`() = runTest {
        fakeNetworkClient.enqueue(AppResult.Failure(CoreError.Network.NoConnection))
        val repo = createRepository()

        val result = repo.login(code = "auth-code", codeVerifier = "verifier", provider = OAuthProvider.GOOGLE)

        result shouldBe AppResult.Failure(CoreError.Network.NoConnection)
        fakeTokenProvider.accessToken shouldBe null
    }

    @Test
    fun `login success - token flow receives new token`() = runTest {
        fakeNetworkClient.enqueue(
            AppResult.Success(AuthResponse("access123", "refresh456", expiresIn = 3600L)),
        )
        val repo = createRepository()

        repo.login(code = "auth-code", codeVerifier = "verifier", provider = OAuthProvider.GOOGLE)

        repo.token.test {
            val token = awaitItem()
            token shouldNotBe null
            token?.accessToken shouldBe "access123"
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `logout - clears token provider and returns Success`() = runTest {
        fakeTokenProvider.accessToken = "existing"
        fakeTokenProvider.refreshToken = "existing-refresh"
        val repo = createRepository()

        val result = repo.logout()

        result shouldBe AppResult.Success(Unit)
        fakeTokenProvider.cleared shouldBe true
    }

    @Test
    fun `refreshToken with no refresh token - returns Unauthorized failure`() = runTest {
        fakeTokenProvider.refreshToken = null
        val repo = createRepository()

        val result = repo.refreshToken()

        result shouldBe AppResult.Failure(CoreError.Unauthorized)
    }

    @Test
    fun `refreshToken with refresh token present - delegates to network and returns result`() = runTest {
        fakeTokenProvider.refreshToken = "refresh-token"
        fakeNetworkClient.enqueue(AppResult.Success(Unit))
        val repo = createRepository()

        val result = repo.refreshToken()

        result shouldBe AppResult.Success(Unit)
    }
}
