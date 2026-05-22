package com.yourcompany.kmptemplate.auth

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.presentation.AuthAction
import com.yourcompany.kmptemplate.auth.presentation.AuthState
import com.yourcompany.kmptemplate.core.ui.components.AppHeader
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

@Composable
fun LoginScreen(state: AuthState, onAction: (AuthAction) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppHeader(text = "YourApp")
            Spacer(Modifier.height(48.dp))
            Text(text = "Sign in", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(32.dp))

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else {
                OAuthButton("Continue with Google", OAuthProvider.GOOGLE, onAction)
                Spacer(Modifier.height(12.dp))
                OAuthButton("Continue with Apple", OAuthProvider.APPLE, onAction)
                Spacer(Modifier.height(12.dp))
                OAuthButton("Continue with Microsoft", OAuthProvider.MICROSOFT, onAction)
                Spacer(Modifier.height(12.dp))
                OAuthButton("Continue with GitHub", OAuthProvider.GITHUB, onAction)
            }

            AnimatedVisibility(visible = state.error != null) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun OAuthButton(label: String, provider: OAuthProvider, onAction: (AuthAction) -> Unit) {
    OutlinedButton(
        onClick = { onAction(AuthAction.LoginWith(provider)) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(label)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenLightPreview() {
    AppTheme(ThemeMode.LIGHT) { LoginScreen(state = AuthState(), onAction = {}) }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenDarkPreview() {
    AppTheme(ThemeMode.DARK) { LoginScreen(state = AuthState(), onAction = {}) }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenLoadingPreview() {
    AppTheme(ThemeMode.LIGHT) { LoginScreen(state = AuthState(isLoading = true), onAction = {}) }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    AppTheme(ThemeMode.LIGHT) {
        LoginScreen(state = AuthState(error = "Authentication failed. Please try again."), onAction = {})
    }
}
