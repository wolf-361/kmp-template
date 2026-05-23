package com.yourcompany.kmptemplate.core.data.network

import com.yourcompany.kmptemplate.core.domain.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey

class BearerAuthPlugin private constructor(private val tokenProvider: TokenProvider) {

    class Config {
        lateinit var tokenProvider: TokenProvider
    }

    companion object Plugin : HttpClientPlugin<Config, BearerAuthPlugin> {
        override val key = AttributeKey<BearerAuthPlugin>("BearerAuth")

        override fun prepare(block: Config.() -> Unit) = BearerAuthPlugin(Config().apply(block).tokenProvider)

        override fun install(plugin: BearerAuthPlugin, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                val token = plugin.tokenProvider.getAccessToken()
                if (token != null) {
                    context.headers[HttpHeaders.Authorization] = "Bearer $token"
                }
                proceed()
            }
        }
    }
}
