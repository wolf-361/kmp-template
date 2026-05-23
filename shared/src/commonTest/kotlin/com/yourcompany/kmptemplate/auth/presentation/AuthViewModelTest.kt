package com.yourcompany.kmptemplate.auth.presentation

import app.cash.turbine.test
import com.yourcompany.kmptemplate.auth.domain.errors.AuthError
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.domain.usecase.LoginUseCase
import com.yourcompany.kmptemplate.auth.domain.usecase.LogoutUseCase
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffect
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffectsHandler
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import com.yourcompany.kmptemplate.core.navigation.AuthDestination
import com.yourcompany.kmptemplate.core.navigation.TopLevelGraph
import com.yourcompany.kmptemplate.core.test.BaseViewModelTest
import com.yourcompany.kmptemplate.core.test.startTestKoin
import com.yourcompany.kmptemplate.core.test.stopTestKoin
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AuthViewModelTest : BaseViewModelTest() {

    private val loginUseCase = mock<LoginUseCase>()
    private val logoutUseCase = mock<LogoutUseCase>()
    private val globalUiEffectsHandler = GlobalUiEffectsHandler()
    private lateinit var viewModel: AuthViewModel

    @BeforeTest
    fun setUp() {
        startTestKoin(module { single { globalUiEffectsHandler } })
        viewModel = AuthViewModel(loginUseCase, logoutUseCase)
    }

    @AfterTest
    fun tearDown() {
        viewModel.onCleared()
        stopTestKoin()
    }

    @Test
    fun `initial state has isLoading false and no error`() = runTest {
        viewModel.state.value shouldBe AuthState()
    }

    @Test
    fun `LoginWith success - navigates to Dashboard`() = runTest {
        everySuspend { loginUseCase(any(), any()) } returns AppResult.Success(Unit)

        globalUiEffectsHandler.effects.test {
            viewModel.onAction(AuthAction.LoginWith(OAuthProvider.GOOGLE))
            awaitItem() shouldBe GlobalUiEffect.NavigateTo(TopLevelGraph.Dashboard)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LoginWith sets isLoading true then clears it on OAuthCancelled`() = runTest {
        everySuspend { loginUseCase(any(), any()) } returns AppResult.Failure(AuthError.OAuthCancelled)

        viewModel.state.test {
            awaitItem() shouldBe AuthState()
            viewModel.onAction(AuthAction.LoginWith(OAuthProvider.GOOGLE))
            awaitItem() shouldBe AuthState(isLoading = true)
            awaitItem() shouldBe AuthState(isLoading = false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LoginWith unhandled failure - state shows error and clears loading`() = runTest {
        everySuspend { loginUseCase(any(), any()) } returns AppResult.Failure(CoreError.Unauthorized)

        viewModel.state.test {
            awaitItem() shouldBe AuthState()
            viewModel.onAction(AuthAction.LoginWith(OAuthProvider.GOOGLE))
            awaitItem() shouldBe AuthState(isLoading = true)
            val errorState = awaitItem()
            errorState.isLoading shouldBe false
            errorState.error shouldBe CoreError.Unauthorized.toString()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Logout - navigates back to Login`() = runTest {
        everySuspend { logoutUseCase() } returns AppResult.Success(Unit)

        globalUiEffectsHandler.effects.test {
            viewModel.onAction(AuthAction.Logout)
            awaitItem() shouldBe GlobalUiEffect.NavigateBackTo(AuthDestination.Login, inclusive = true)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
