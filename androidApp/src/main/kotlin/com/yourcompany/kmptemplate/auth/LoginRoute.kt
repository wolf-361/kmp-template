package com.yourcompany.kmptemplate.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yourcompany.kmptemplate.auth.presentation.AuthViewModel
import org.koin.compose.koinInject

@Composable
fun LoginRoute(viewModel: AuthViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    LoginScreen(state = state, onAction = viewModel::onAction)
}
