package com.yourcompany.kmptemplate.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.presentation.AuthAction
import com.yourcompany.kmptemplate.auth.presentation.AuthViewModel
import org.koin.java.KoinJavaComponent.getKoin

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = remember { getKoin().get() },
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Sign in",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(40.dp))

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
        } else {
            OAuthButton("Continue with Google", OAuthProvider.GOOGLE, viewModel)
            Spacer(Modifier.height(12.dp))
            OAuthButton("Continue with Apple", OAuthProvider.APPLE, viewModel)
            Spacer(Modifier.height(12.dp))
            OAuthButton("Continue with Microsoft", OAuthProvider.MICROSOFT, viewModel)
            Spacer(Modifier.height(12.dp))
            OAuthButton("Continue with GitHub", OAuthProvider.GITHUB, viewModel)
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
private fun OAuthButton(label: String, provider: OAuthProvider, viewModel: AuthViewModel) {
    OutlinedButton(
        onClick = { viewModel.onAction(AuthAction.LoginWith(provider)) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}
