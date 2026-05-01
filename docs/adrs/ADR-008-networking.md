# ADR-008: Network Client — Ktor with Abstract NetworkClient

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The project needs an HTTP client that:
- Works in `commonMain` with platform-specific engines.
- Cleanly distinguishes authenticated requests (trigger Unauthorized flow on 401) from unauthenticated requests (return error as-is on 401).
- Keeps route definitions co-located with feature data layers.
- Is testable without a real server.
- Documents the end-to-end Unauthorized flow.

---

## Decision Drivers

1. KMP-native — single client configuration in `commonMain`.
2. **Two request modes** — `request<T>()` (authenticated, triggers logout on 401) and `unauthorizedRequest<T>()` (no logout trigger). Most endpoints use `request()`; auth endpoints use `unauthorizedRequest()`.
3. Routes are **string constants** co-located with the feature — not a generic data class shared across all features.
4. The actual request shape (method, body, headers) is defined inline via a lambda, keeping maximum flexibility.
5. Abstract base class with `coreRequest()` implementation point — single place for error handling, logging, and 401 logic.
6. Mockable via `MockEngine`.

---

## Decision Outcome

**Use Ktor 3.x with an abstract `NetworkClient` base class.**

### Abstract NetworkClient

```kotlin
// core/network/NetworkClient.kt
abstract class NetworkClient {

    protected abstract val httpClient: HttpClient

    protected abstract suspend fun <T> coreRequest(
        deserializer: DeserializationStrategy<T>,
        block: HttpRequestBuilder.() -> Unit,
        shouldTriggerLogout: Boolean,
    ): AppResult<T>

    suspend inline fun <reified T> request(
        noinline block: HttpRequestBuilder.() -> Unit,
    ): AppResult<T> = coreRequest(serializer(), block, shouldTriggerLogout = true)

    suspend inline fun <reified T> unauthorizedRequest(
        noinline block: HttpRequestBuilder.() -> Unit,
    ): AppResult<T> = coreRequest(serializer(), block, shouldTriggerLogout = false)
}
```

- `request<T>()` — sends Bearer token; on 401 triggers token refresh; on refresh failure emits `GlobalUiEffect.Unauthorized` and clears tokens.
- `unauthorizedRequest<T>()` — sends no token; 401 returns `AppResult.Failure(CoreError.Unauthorized)` silently, **no Unauthorized effect emitted, no logout**.

---

### NetworkClientImpl

```kotlin
@Single
class NetworkClientImpl(
    override val httpClient: HttpClient,
    private val logger: AppLogger,
    private val tokenProvider: TokenProvider,
    private val globalEffectsHandler: GlobalUiEffectsHandler,
) : NetworkClient() {

    private val refreshMutex = Mutex()

    override suspend fun <T> coreRequest(
        deserializer: DeserializationStrategy<T>,
        block: HttpRequestBuilder.() -> Unit,
        shouldTriggerLogout: Boolean,
    ): AppResult<T> = try {
        val response = httpClient.request { block() }

        if (response.status == HttpStatusCode.Unauthorized && shouldTriggerLogout) {
            val refreshed = tryRefreshTokens()
            if (!refreshed) {
                tokenProvider.clearTokens()
                globalEffectsHandler.emit(GlobalUiEffect.Unauthorized)
                return AppResult.Failure(CoreError.Unauthorized)
            }
            val retryResponse = httpClient.request { block() }
            AppResult.Success(retryResponse.body(deserializer))
        } else if (response.status == HttpStatusCode.Unauthorized) {
            AppResult.Failure(CoreError.Unauthorized)
        } else {
            AppResult.Success(response.body(deserializer))
        }
    } catch (e: SerializationException) {
        logger.e(TAG) { "Deserialization error: ${e.message}" }
        AppResult.Failure(CoreError.DataCorruption)
    } catch (e: HttpRequestTimeoutException) {
        AppResult.Failure(CoreError.Network.Timeout)
    } catch (e: ClientRequestException) {
        AppResult.Failure(extractBackendError(e))
    } catch (e: Exception) {
        AppResult.Failure(CoreError.Network.NoConnection)
    }

    private suspend fun tryRefreshTokens(): Boolean = refreshMutex.withLock {
        val refreshToken = tokenProvider.getRefreshToken() ?: return@withLock false
        val result = unauthorizedRequest<TokenResponse> {
            method = HttpMethod.Post
            url(AuthRoutes.REFRESH)
            setBody(RefreshRequest(refreshToken))
        }
        when (result) {
            is AppResult.Success -> {
                tokenProvider.saveTokens(result.data.accessToken, result.data.refreshToken)
                true
            }
            is AppResult.Failure -> false
        }
    }

    private suspend fun extractBackendError(e: ClientRequestException): AppError =
        try {
            val body = e.response.body<BackendErrorDto>()
            CoreError.Network.BackendPayload(e.response.status.value, body.message)
        } catch (_: Exception) {
            CoreError.Network.BackendPayload(e.response.status.value, e.message ?: "Unknown error")
        }
}
```

**Mutex guarantee:** `refreshMutex` ensures that if multiple requests simultaneously receive a 401, only one refresh call is made. Others wait and reuse the new token.

---

### HttpClient configuration

```kotlin
// core/di/NetworkModule.kt
@Module
class NetworkModule {
    @Single
    fun provideHttpClient(
        engine: HttpClientEngine,
        tokenProvider: TokenProvider,
    ): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(BearerAuthPlugin) {
            this.tokenProvider = tokenProvider
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) = appLogger.d("Ktor") { message }
            }
            level = LogLevel.BODY
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }
}
```

### BearerAuthPlugin (custom)

Rather than using Ktor's built-in `Auth` plugin (which couples the refresh logic into the client config), a dedicated `BearerAuthPlugin` injects the token into the request pipeline. The refresh logic lives in `NetworkClientImpl.tryRefreshTokens()`.

```kotlin
// core/network/BearerAuthPlugin.kt
class BearerAuthPlugin(private val tokenProvider: TokenProvider) {
    companion object Plugin : HttpClientPlugin<Config, BearerAuthPlugin> {
        override val key = AttributeKey<BearerAuthPlugin>("BearerAuth")

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
```

---

### Feature Routes — string constants

Routes are simple `object` declarations with string constants, co-located with the feature's data layer:

```kotlin
// features/auth/data/remote/AuthRoutes.kt
internal object AuthRoutes {
    private const val BASE = "auth"
    const val LOGIN            = "$BASE/login"
    const val REGISTER         = "$BASE/register"
    const val LOGOUT           = "$BASE/logout"
    const val REFRESH          = "$BASE/refresh"
    const val FORGOT_PASSWORD  = "$BASE/forgot-password"
    const val RESET_PASSWORD   = "$BASE/reset-password"
    const val OAUTH            = "$BASE/oauth"
}

// features/users/data/remote/UserRoutes.kt
internal object UserRoutes {
    private const val BASE = "users"
    const val ME             = "$BASE/me"
    const val AVATAR         = "$BASE/me/avatar"
    const val CHANGE_PASSWORD = "$BASE/change-password"
    fun profile(id: String)  = "$BASE/$id"
}
```

---

### Repository usage

```kotlin
@Single
@Binds(binds = [AuthRepository::class])
class AuthRepositoryImpl(
    private val networkClient: NetworkClient,
    private val tokenProvider: TokenProvider,
    private val authDao: AuthDao,
) : AuthRepository {

    // Login: no token sent, 401 does NOT trigger logout
    override suspend fun login(email: String, password: String): AppResult<User> =
        networkClient.unauthorizedRequest<AuthResponse> {
            method = HttpMethod.Post
            url(AuthRoutes.LOGIN)
            setBody(LoginRequest(email, password))
        }.map { it.toDomain() }

    // Current user: token sent, 401 triggers logout
    override suspend fun getCurrentUser(): AppResult<User> =
        networkClient.request<UserResponse> {
            method = HttpMethod.Get
            url(UserRoutes.ME)
        }.map { it.toDomain() }

    // Logout: fire-and-forget — token sent but 401 does NOT re-trigger logout
    override suspend fun logout(): AppResult<Unit> =
        networkClient.unauthorizedRequest<Unit> {
            method = HttpMethod.Post
            url(AuthRoutes.LOGOUT)
        }
}
```

---

### 401 Unauthorized — end-to-end flow

```
1. Repository calls networkClient.request { ... }
2. BearerAuthPlugin injects Bearer token
3. Server returns 401
4. coreRequest() detects 401 with shouldTriggerLogout = true
5. refreshMutex.withLock { tryRefreshTokens() }
   5a. Refresh SUCCESS → save new tokens → retry original request → return result
   5b. Refresh FAILURE (401 or network error):
       → tokenProvider.clearTokens()
       → globalEffectsHandler.emit(GlobalUiEffect.Unauthorized)
       → return AppResult.Failure(CoreError.Unauthorized)
6. Android NavHost / iOS RootView observes Unauthorized effect
7. Pop back stack → navigate to AuthDestination.Login
```

For `unauthorizedRequest()`: step 4 short-circuits to `return AppResult.Failure(CoreError.Unauthorized)` — no refresh, no logout, no global effect.

---

### MockEngine in tests

```kotlin
val mockEngine = MockEngine { request ->
    when {
        request.url.encodedPath.endsWith(AuthRoutes.LOGIN) ->
            respond(
                content = """{"accessToken":"test-token","refreshToken":"test-refresh"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        else -> respondError(HttpStatusCode.NotFound)
    }
}
val testClient = NetworkClientImpl(
    httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json() } },
    logger = PrintLogger(),
    tokenProvider = FakeTokenProvider(),
    globalEffectsHandler = FakeGlobalEffectsHandler(),
)
```

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Single generic `request<T>(route: Route)` | One entry point cannot distinguish auth vs non-auth without a param; route data class is less flexible than a lambda |
| Ktor built-in `Auth` plugin | Couples refresh logic into client config; harder to test and reason about |
| Verb-based API (`get()`, `post()`, etc.) | Four methods instead of two; request shape is not self-describing |
| Retrofit | JVM/Android-only |
