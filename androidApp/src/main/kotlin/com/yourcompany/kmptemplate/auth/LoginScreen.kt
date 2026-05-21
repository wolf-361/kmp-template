package com.yourcompany.kmptemplate.auth

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.presentation.AuthAction
import com.yourcompany.kmptemplate.auth.presentation.AuthState
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

@Composable
fun LoginScreen(state: AuthState, onAction: (AuthAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Sign in", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(40.dp))

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

        state.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun OAuthButton(label: String, provider: OAuthProvider, onAction: (AuthAction) -> Unit) {
    OutlinedButton(
        onClick = { onAction(AuthAction.LoginWith(provider)) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenLightPreview() {
    AppTheme(ThemeMode.LIGHT) {
        LoginScreen(state = AuthState(), onAction = {})
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenDarkPreview() {
    AppTheme(ThemeMode.DARK) {
        LoginScreen(state = AuthState(), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenLoadingPreview() {
    AppTheme(ThemeMode.LIGHT) {
        LoginScreen(state = AuthState(isLoading = true), onAction = {})
    }
}
