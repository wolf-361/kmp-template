package com.yourcompany.kmptemplate.auth.presentation

import com.yourcompany.kmptemplate.auth.domain.errors.AuthError
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.domain.usecase.LoginUseCase
import com.yourcompany.kmptemplate.auth.domain.usecase.LogoutUseCase
import com.yourcompany.kmptemplate.core.domain.extensions.handle
import com.yourcompany.kmptemplate.core.navigation.AuthDestination
import com.yourcompany.kmptemplate.core.navigation.TopLevelGraph
import com.yourcompany.kmptemplate.core.presentation.BaseViewModel
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface AuthAction {
    data class LoginWith(val provider: OAuthProvider) : AuthAction
    data object Logout : AuthAction
}

sealed interface AuthEffect

@Factory
class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
) : BaseViewModel<AuthState, AuthAction, AuthEffect>(AuthState()) {

    override fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.LoginWith -> login(action.provider)
            is AuthAction.Logout -> logout()
        }
    }

    private fun login(provider: OAuthProvider) = viewModelScope.launch {
        setState { copy(isLoading = true, error = null) }
        // TODO: replace "TODO_CLIENT_ID" with BuildKonfig per-provider constant
        loginUseCase(provider = provider, clientId = "TODO_CLIENT_ID").handle {
            success { navigateTo(TopLevelGraph.Dashboard) }
            failure<AuthError.OAuthCancelled> { setState { copy(isLoading = false) } }
            catch { setState { copy(isLoading = false, error = it.toString()) } }
        }
    }

    private fun logout() = viewModelScope.launch {
        logoutUseCase()
        navigateBackTo(AuthDestination.Login, inclusive = true)
    }
}
