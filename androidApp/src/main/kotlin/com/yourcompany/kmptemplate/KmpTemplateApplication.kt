package com.yourcompany.kmptemplate

import android.app.Application
import com.yourcompany.kmptemplate.auth.data.AndroidOAuthFlowLauncher
import com.yourcompany.kmptemplate.auth.domain.port.OAuthFlowLauncher
import com.yourcompany.kmptemplate.core.data.local.appContext
import com.yourcompany.kmptemplate.di.AuthModule
import com.yourcompany.kmptemplate.di.CoreModule
import com.yourcompany.kmptemplate.di.SettingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

class KmpTemplateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        startKoin {
            androidLogger()
            androidContext(this@KmpTemplateApplication)
            modules(
                CoreModule().module,
                AuthModule().module,
                SettingsModule().module,
                module { single<OAuthFlowLauncher> { AndroidOAuthFlowLauncher() } },
            )
        }
    }
}
