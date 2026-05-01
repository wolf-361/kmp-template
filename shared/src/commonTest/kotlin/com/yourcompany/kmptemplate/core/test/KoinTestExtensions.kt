package com.yourcompany.kmptemplate.core.test

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

fun startTestKoin(vararg modules: Module) {
    startKoin { modules(*modules) }
}

fun stopTestKoin() = stopKoin()
