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

class LogoutUseCaseTest {

    private val repository = mock<OAuthRepository>()
    private val useCase = LogoutUseCase(repository)

    @Test
    fun `delegates to repository logout and returns Success`() = runTest {
        everySuspend { repository.logout() } returns AppResult.Success(Unit)

        val result = useCase()

        result shouldBe AppResult.Success(Unit)
    }

    @Test
    fun `repository Failure is propagated`() = runTest {
        everySuspend { repository.logout() } returns AppResult.Failure(CoreError.Network.NoConnection)

        val result = useCase()

        result shouldBe AppResult.Failure(CoreError.Network.NoConnection)
    }
}
