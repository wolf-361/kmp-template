package com.yourcompany.kmptemplate.core.data.network

import app.cash.turbine.test
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import com.yourcompany.kmptemplate.core.test.FakeTokenProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.reflect.typeInfo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test

@Serializable
private data class TestPayload(val id: Int)

class NetworkClientImplTest {

    private val fakeTokenProvider = FakeTokenProvider()
    private val globalEffectsHandler = GlobalUiEffectsHandler()

    private fun jsonHeaders() = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))

    private fun buildClient(vararg handlers: MockRequestHandler): NetworkClientImpl {
        val queue = MockEngine.Queue()
        handlers.forEach { queue.enqueue(it) }
        val http = HttpClient(queue) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return NetworkClientImpl(
            httpClient = http,
            tokenProvider = fakeTokenProvider,
            globalEffectsHandler = globalEffectsHandler,
            tokenRefreshPath = NetworkConfig.TOKEN_REFRESH_PATH,
        )
    }

    @Test
    fun `200 response - returns Success with parsed body`() = runTest {
        val client = buildClient(
            { respond("""{"id":42}""", HttpStatusCode.OK, jsonHeaders()) },
        )

        val result = client.coreRequest<TestPayload>(typeInfo<TestPayload>(), { }, shouldTriggerLogout = false)

        result shouldBe AppResult.Success(TestPayload(42))
    }

    @Test
    fun `200 response for Unit type - returns Success without body parsing`() = runTest {
        val client = buildClient(
            { respond("", HttpStatusCode.OK) },
        )

        val result = client.request<Unit> { }

        result shouldBe AppResult.Success(Unit)
    }

    @Test
    fun `401 with logout trigger, refresh succeeds - retries and returns Success`() = runTest {
        fakeTokenProvider.refreshToken = "old-refresh"
        val client = buildClient(
            { respond("", HttpStatusCode.Unauthorized) },
            {
                respond(
                    """{"access_token":"new-access","refresh_token":"new-refresh"}""",
                    HttpStatusCode.OK,
                    jsonHeaders(),
                )
            },
            { respond("", HttpStatusCode.OK) },
        )

        val result = client.request<Unit> { }

        result shouldBe AppResult.Success(Unit)
        fakeTokenProvider.accessToken shouldBe "new-access"
        fakeTokenProvider.refreshToken shouldBe "new-refresh"
    }

    @Test
    fun `401 with logout trigger, refresh fails - clears tokens and emits Unauthorized effect`() = runTest {
        fakeTokenProvider.refreshToken = "old-refresh"
        val client = buildClient(
            { respond("", HttpStatusCode.Unauthorized) },
            { respond("", HttpStatusCode.Unauthorized) },
        )

        globalEffectsHandler.effects.test {
            val result = client.request<Unit> { }

            result shouldBe AppResult.Failure(CoreError.Unauthorized)
            fakeTokenProvider.cleared shouldBe true
            awaitItem() shouldBe GlobalUiEffect.Unauthorized
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `401 without logout trigger (unauthorizedRequest) - returns Unauthorized without refresh`() = runTest {
        fakeTokenProvider.refreshToken = "some-refresh"
        val client = buildClient(
            { respond("", HttpStatusCode.Unauthorized) },
        )

        val result = client.unauthorizedRequest<Unit> { }

        result shouldBe AppResult.Failure(CoreError.Unauthorized)
        fakeTokenProvider.cleared shouldBe false
    }

    @Test
    fun `500 response - returns BackendPayload failure with status code`() = runTest {
        val client = buildClient(
            { respond("""{"message":"Internal error"}""", HttpStatusCode.InternalServerError, jsonHeaders()) },
        )

        val result = client.request<Unit> { }

        result.shouldBeInstanceOf<AppResult.Failure>()
        val error = (result as AppResult.Failure).error.shouldBeInstanceOf<CoreError.Network.BackendPayload>()
        error.code shouldBe 500
    }

    @Test
    fun `network exception - returns NoConnection failure`() = runTest {
        val client = buildClient(
            { throw RuntimeException("connection refused") },
        )

        val result = client.request<Unit> { }

        result shouldBe AppResult.Failure(CoreError.Network.NoConnection)
    }
}
