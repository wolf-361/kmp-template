package com.yourcompany.kmptemplate.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yourcompany.kmptemplate.settings.presentation.SettingsViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    SettingsScreen(state = state, onAction = viewModel::onAction)
}
