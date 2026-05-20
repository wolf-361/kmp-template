package com.yourcompany.kmptemplate

import com.yourcompany.kmptemplate.auth.data.IOSOAuthFlowLauncher
import com.yourcompany.kmptemplate.auth.domain.port.OAuthFlowLauncher
import com.yourcompany.kmptemplate.auth.presentation.AuthViewModel
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffectsHandler
import com.yourcompany.kmptemplate.di.AuthModule
import com.yourcompany.kmptemplate.di.CoreModule
import com.yourcompany.kmptemplate.di.SettingsModule
import com.yourcompany.kmptemplate.settings.presentation.SettingsViewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.mp.KoinPlatform

fun initKoin() {
    startKoin {
        modules(
            CoreModule().module,
            AuthModule().module,
            SettingsModule().module,
            module { single<OAuthFlowLauncher> { IOSOAuthFlowLauncher() } },
        )
    }
}

// Convenience accessors for the iOS app layer — avoids raw Koin API from Swift.
fun getGlobalUiEffectsHandler(): GlobalUiEffectsHandler = KoinPlatform.getKoin().get()

fun getAuthViewModel(): AuthViewModel = KoinPlatform.getKoin().get()

fun getSettingsViewModel(): SettingsViewModel = KoinPlatform.getKoin().get()
