package com.yourcompany.kmptemplate.core.data.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.Logger as KtorLogger

expect fun createHttpEngine(): HttpClientEngine

fun createNetworkClient(): HttpClient = HttpClient(createHttpEngine()) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            },
        )
    }
    install(Logging) {
        level = LogLevel.INFO
        logger = object : KtorLogger {
            override fun log(message: String) {
                Logger.i("HttpClient") { message }
            }
        }
    }
}
