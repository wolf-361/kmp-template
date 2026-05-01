package com.yourcompany.kmptemplate.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import com.yourcompany.kmptemplate.core.data.local.createDataStore
import com.yourcompany.kmptemplate.core.data.network.BearerAuthPlugin
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffectsHandler
import com.yourcompany.kmptemplate.core.data.network.NetworkClient
import com.yourcompany.kmptemplate.core.data.network.NetworkClientImpl
import com.yourcompany.kmptemplate.core.data.network.NetworkConfig
import com.yourcompany.kmptemplate.core.data.network.createHttpEngine
import com.yourcompany.kmptemplate.core.domain.TokenProvider
import com.yourcompany.kmptemplate.core.domain.TokenProviderImpl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import io.ktor.client.plugins.logging.Logger as KtorLogger

@Module
@ComponentScan("com.yourcompany.kmptemplate")
class CoreModule {

    @Single
    fun provideTokenProvider(): TokenProvider = TokenProviderImpl()

    @Single
    fun provideGlobalUiEffectsHandler(): GlobalUiEffectsHandler = GlobalUiEffectsHandler()

    @Single
    fun provideHttpClient(tokenProvider: TokenProvider): HttpClient =
        HttpClient(createHttpEngine()) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false })
            }
            install(BearerAuthPlugin) {
                this.tokenProvider = tokenProvider
            }
            install(HttpTimeout) {
                requestTimeoutMillis = NetworkConfig.REQUEST_TIMEOUT_MS
                connectTimeoutMillis = NetworkConfig.CONNECT_TIMEOUT_MS
            }
            install(DefaultRequest) {
                url(NetworkConfig.BASE_URL)
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

    @Single
    fun provideNetworkClient(
        httpClient: HttpClient,
        tokenProvider: TokenProvider,
        effectsHandler: GlobalUiEffectsHandler,
    ): NetworkClient = NetworkClientImpl(
        httpClient = httpClient,
        tokenProvider = tokenProvider,
        globalEffectsHandler = effectsHandler,
    )

    @Single
    fun provideDataStore(): DataStore<Preferences> = createDataStore()
}
