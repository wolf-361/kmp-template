package com.yourcompany.kmptemplate.auth.domain.usecase

import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.domain.port.OAuthFlowLauncher
import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LoginUseCaseTest {

    private val repository = mock<OAuthRepository>()
    private val flowLauncher = mock<OAuthFlowLauncher>()
    private val useCase = LoginUseCase(repository, flowLauncher)

    @Test
    fun `success - launcher returns code, repository succeeds, result is Success`() = runTest {
        everySuspend { flowLauncher.launch(any()) } returns "auth-code"
        everySuspend { repository.login(any(), any(), any()) } returns AppResult.Success(Unit)

        val result = useCase(OAuthProvider.GOOGLE, "client-id")

        result.shouldBeInstanceOf<AppResult.Success<Unit>>()
    }

    @Test
    fun `launcher throws - result wrapped as NoConnection failure`() = runTest {
        everySuspend { flowLauncher.launch(any()) } throws RuntimeException("flow cancelled")

        val result = useCase(OAuthProvider.GOOGLE, "client-id")

        result shouldBe AppResult.Failure(CoreError.Network.NoConnection)
    }

    @Test
    fun `repository returns Failure - failure is propagated`() = runTest {
        everySuspend { flowLauncher.launch(any()) } returns "auth-code"
        everySuspend { repository.login(any(), any(), any()) } returns AppResult.Failure(CoreError.Unauthorized)

        val result = useCase(OAuthProvider.GOOGLE, "client-id")

        result shouldBe AppResult.Failure(CoreError.Unauthorized)
    }
}
