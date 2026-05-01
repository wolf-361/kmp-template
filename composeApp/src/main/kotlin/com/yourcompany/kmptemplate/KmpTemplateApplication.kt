package com.yourcompany.kmptemplate

import android.app.Application
import com.yourcompany.kmptemplate.di.CoreModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class KmpTemplateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@KmpTemplateApplication)
            modules(CoreModule().module)
        }
    }
}
