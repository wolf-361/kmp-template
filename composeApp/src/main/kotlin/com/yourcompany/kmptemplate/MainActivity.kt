package com.yourcompany.kmptemplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.rememberNavController
import com.yourcompany.kmptemplate.auth.data.AndroidOAuthFlowLauncher
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffectsHandler
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val globalEffectsHandler: GlobalUiEffectsHandler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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
