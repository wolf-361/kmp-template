package com.yourcompany.kmptemplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.yourcompany.kmptemplate.auth.data.AndroidOAuthFlowLauncher
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffectsHandler
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.presentation.SettingsViewModel
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val globalEffectsHandler: GlobalUiEffectsHandler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = koinInject()
            val settingsState by settingsViewModel.state.collectAsState()
            AppTheme(
                themeMode = settingsState.themeMode,
                useDynamicColor = settingsState.useDynamicColor,
            ) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    globalEffectsHandler = globalEffectsHandler,
                )
            }
        }
    }

    // When MainActivity returns to the foreground after a Custom Tab, the user closed the tab
    // without completing OAuth. deliverCancellation() is a no-op if code was already delivered.
    override fun onResume() {
        super.onResume()
        AndroidOAuthFlowLauncher.deliverCancellation()
    }
}
