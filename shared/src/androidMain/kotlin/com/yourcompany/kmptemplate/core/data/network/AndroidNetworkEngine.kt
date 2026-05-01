package com.yourcompany.kmptemplate.core.data.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

actual fun createHttpEngine(): HttpClientEngine = Android.create()
