package com.yourcompany.kmptemplate.core.data.network

import com.yourcompany.kmptemplate.core.test.FakeTokenProvider
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class BearerAuthPluginTest {

    private val fakeTokenProvider = FakeTokenProvider()

    private fun buildClient() = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                respond("", HttpStatusCode.OK, request.headers)
            }
        }
        install(BearerAuthPlugin) { tokenProvider = fakeTokenProvider }
    }

    @Test
    fun `token present - Authorization header is added to request`() = runTest {
        fakeTokenProvider.accessToken = "my-access-token"

        val capturedHeaders = mutableListOf<String?>()
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    capturedHeaders += request.headers[HttpHeaders.Authorization]
                    respond("", HttpStatusCode.OK)
                }
            }
            install(BearerAuthPlugin) { tokenProvider = fakeTokenProvider }
        }

        client.get("http://localhost/test")

        capturedHeaders.first() shouldBe "Bearer my-access-token"
        client.close()
    }

    @Test
    fun `token null - Authorization header is not added`() = runTest {
        fakeTokenProvider.accessToken = null

        val capturedHeaders = mutableListOf<String?>()
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    capturedHeaders += request.headers[HttpHeaders.Authorization]
                    respond("", HttpStatusCode.OK)
                }
            }
            install(BearerAuthPlugin) { tokenProvider = fakeTokenProvider }
        }

        client.get("http://localhost/test")

        capturedHeaders.first() shouldBe null
        client.close()
    }
}
