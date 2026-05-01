package com.yourcompany.kmptemplate.core.data.network

import co.touchlab.kermit.Logger
import com.yourcompany.kmptemplate.core.domain.AppError
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import com.yourcompany.kmptemplate.core.domain.TokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.contentType
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.util.reflect.TypeInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class InternalRefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
private data class InternalTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
private data class BackendErrorBody(val message: String = "Unknown error")

class NetworkClientImpl(
    override val httpClient: HttpClient,
    private val tokenProvider: TokenProvider,
    private val globalEffectsHandler: GlobalUiEffectsHandler,
    private val tokenRefreshPath: String = NetworkConfig.TOKEN_REFRESH_PATH,
) : NetworkClient() {

    private val refreshMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> coreRequest(
        typeInfo: TypeInfo,
        block: HttpRequestBuilder.() -> Unit,
        shouldTriggerLogout: Boolean,
    ): AppResult<T> = try {
        val response = httpClient.request { block() }

        when {
            response.status.isSuccess() -> {
                val data = if (typeInfo.type == Unit::class) Unit as T else response.body(typeInfo)
                AppResult.Success(data)
            }

            response.status == HttpStatusCode.Unauthorized && shouldTriggerLogout -> {
                val refreshed = tryRefreshTokens()
                if (refreshed) {
                    val retry = httpClient.request { block() }
                    if (retry.status.isSuccess()) {
                        val data = if (typeInfo.type == Unit::class) Unit as T else retry.body(typeInfo)
                        AppResult.Success(data)
                    } else {
                        AppResult.Failure(extractError(retry.status.value, retry.body()))
                    }
                } else {
                    tokenProvider.clearTokens()
                    globalEffectsHandler.emit(GlobalUiEffect.Unauthorized)
                    AppResult.Failure(CoreError.Unauthorized)
                }
            }

            response.status == HttpStatusCode.Unauthorized ->
                AppResult.Failure(CoreError.Unauthorized)

            else ->
                AppResult.Failure(extractError(response.status.value, response.body()))
        }
    } catch (e: SerializationException) {
        Logger.e("NetworkClient") { "Deserialization error: ${e.message}" }
        AppResult.Failure(CoreError.DataCorruption)
    } catch (e: HttpRequestTimeoutException) {
        AppResult.Failure(CoreError.Network.Timeout)
    } catch (e: Exception) {
        AppResult.Failure(CoreError.Network.NoConnection)
    }

    private suspend fun tryRefreshTokens(): Boolean = refreshMutex.withLock {
        val refreshToken = tokenProvider.getRefreshToken() ?: return@withLock false

        val result = unauthorizedRequest<InternalTokenResponse> {
            method = HttpMethod.Post
            url(tokenRefreshPath)
            contentType(ContentType.Application.Json)
            setBody(InternalRefreshRequest(refreshToken))
        }

        when (result) {
            is AppResult.Success -> {
                tokenProvider.saveTokens(result.data.accessToken, result.data.refreshToken)
                true
            }
            is AppResult.Failure -> false
        }
    }

    private fun extractError(statusCode: Int, rawBody: String): AppError = try {
        val body = json.decodeFromString<BackendErrorBody>(rawBody)
        CoreError.Network.BackendPayload(statusCode, body.message)
    } catch (_: Exception) {
        CoreError.Network.BackendPayload(statusCode, "HTTP $statusCode")
    }
}
