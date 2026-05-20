package com.yourcompany.kmptemplate.auth.domain.usecase

import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RefreshTokenUseCaseTest {

    private val repository = mock<OAuthRepository>()
    private val useCase = RefreshTokenUseCase(repository)

    @Test
    fun `delegates to repository refreshToken and returns Success`() = runTest {
        everySuspend { repository.refreshToken() } returns AppResult.Success(Unit)

        val result = useCase()

        result shouldBe AppResult.Success(Unit)
    }

    @Test
    fun `delegates to repository refreshToken and returns Failure`() = runTest {
        everySuspend { repository.refreshToken() } returns AppResult.Failure(CoreError.Unauthorized)

        val result = useCase()

        result shouldBe AppResult.Failure(CoreError.Unauthorized)
    }
}
