# KMP Toolchain Bump, Module Rename & Auth Test Coverage — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bump Kotlin/KSP/SKIE versions, fix the koin-ksp-compiler version split, rename `composeApp` → `androidApp`, and add full test coverage for the auth stack.

**Architecture:** Pure version-catalog changes for the toolchain bump; filesystem rename plus four config-file patches for the module rename; six new `commonTest` files covering the auth domain, presentation, data, and network layers using existing test infrastructure.

**Tech Stack:** Kotlin 2.3.21, KSP 2.3.8, SKIE 0.10.12, Koin Annotations 4.2.1 / koin-ksp-compiler 2.3.1, Kotest assertions, Turbine, Mokkery 3.x, Ktor MockEngine.

---

## File map

### Modified
- `gradle/libs.versions.toml` — version bumps + koin-ksp-compiler split
- `settings.gradle.kts` — rename `:composeApp` → `:androidApp`
- `fastlane/Fastfile` — AAB path update
- `scripts/create-feature.sh` — three path references
- `scripts/pre-commit.sh` — two module references

### Renamed
- `composeApp/` → `androidApp/`

### Created (tests)
- `shared/src/commonTest/kotlin/.../core/test/FakeTokenProvider.kt`
- `shared/src/commonTest/kotlin/.../core/test/FakeNetworkClient.kt`
- `shared/src/commonTest/kotlin/.../auth/domain/usecase/LoginUseCaseTest.kt`
- `shared/src/commonTest/kotlin/.../auth/domain/usecase/LogoutUseCaseTest.kt`
- `shared/src/commonTest/kotlin/.../auth/domain/usecase/RefreshTokenUseCaseTest.kt`
- `shared/src/commonTest/kotlin/.../auth/presentation/AuthViewModelTest.kt`
- `shared/src/commonTest/kotlin/.../auth/data/repository/OAuthRepositoryImplTest.kt`
- `shared/src/commonTest/kotlin/.../core/data/network/BearerAuthPluginTest.kt`
- `shared/src/commonTest/kotlin/.../core/data/network/NetworkClientImplTest.kt`

(Package prefix throughout: `com/yourcompany/kmptemplate`)

---

## Task 1: Toolchain version bumps

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Apply all version changes**

Open `gradle/libs.versions.toml` and make the following edits:

```toml
# [versions] — change these four lines:
kotlin                  = "2.3.21"   # was 2.3.20; SKIE 0.10.12 now supports 2.3.21
ksp                     = "2.3.8"    # was 2.3.7
skie                    = "0.10.12"  # was 0.10.11; adds Kotlin 2.3.21 support
koin-ksp-compiler       = "2.3.1"    # NEW: koin-ksp-compiler hasn't merged to 4.x — stays on 2.x
```

Also update the `koin-annotations` version comment line to:
```toml
# Koin 4.x merged versioning for koin-annotations jar only; koin-ksp-compiler stays on 2.x
koin                    = "4.2.1"
koin-annotations        = "4.2.1"
```

In the `[libraries]` section, change `koin-ksp-compiler` to use its own version ref:
```toml
# before:
koin-ksp-compiler = { module = "io.insert-koin:koin-ksp-compiler", version.ref = "koin-annotations" }

# after:
koin-ksp-compiler = { module = "io.insert-koin:koin-ksp-compiler", version.ref = "koin-ksp-compiler" }
```

- [ ] **Step 2: Verify KSP metadata generation resolves**

```bash
./gradlew :shared:kspCommonMainKotlinMetadata --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`. This was the exact task that failed with `Could not find io.insert-koin:koin-ksp-compiler:4.2.1`.

- [ ] **Step 3: Verify KMP compilation**

```bash
./gradlew :shared:compileReleaseKotlinAndroid :composeApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL` (module is still called `composeApp` at this point — rename happens in Task 2).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: bump Kotlin 2.3.21, KSP 2.3.8, SKIE 0.10.12; split koin-ksp-compiler version"
```

---

## Task 2: Rename `composeApp` → `androidApp`

**Files:**
- Rename: `composeApp/` → `androidApp/`
- Modify: `settings.gradle.kts`
- Modify: `fastlane/Fastfile`
- Modify: `scripts/create-feature.sh`
- Modify: `scripts/pre-commit.sh`

- [ ] **Step 1: Rename the directory**

```bash
git mv composeApp androidApp
```

- [ ] **Step 2: Update settings.gradle.kts**

Change:
```kotlin
include(":composeApp")
```
To:
```kotlin
include(":androidApp")
```

- [ ] **Step 3: Update Fastfile AAB path**

In `fastlane/Fastfile` line 20, change:
```ruby
aab: "composeApp/build/outputs/bundle/release/composeApp-release.aab",
```
To:
```ruby
aab: "androidApp/build/outputs/bundle/release/androidApp-release.aab",
```

- [ ] **Step 4: Update create-feature.sh**

Three changes in `scripts/create-feature.sh`:

Line 13 (comment) — change:
```bash
#   composeApp         — Compose screen skeleton
```
To:
```bash
#   androidApp         — Compose screen skeleton
```

Lines 75–77 (applicationId detection) — change:
```bash
APP_ID=$(grep 'applicationId' composeApp/build.gradle.kts \
    | grep -o '"[^"]*"' | tr -d '"' | head -1)
[[ -n "$APP_ID" ]] || die "Could not read applicationId from composeApp/build.gradle.kts"
```
To:
```bash
APP_ID=$(grep 'applicationId' androidApp/build.gradle.kts \
    | grep -o '"[^"]*"' | tr -d '"' | head -1)
[[ -n "$APP_ID" ]] || die "Could not read applicationId from androidApp/build.gradle.kts"
```

Line 82 (ANDROID_SRC path) — change:
```bash
ANDROID_SRC="composeApp/src/main/kotlin/${PKG_PATH}"
```
To:
```bash
ANDROID_SRC="androidApp/src/main/kotlin/${PKG_PATH}"
```

- [ ] **Step 5: Update pre-commit.sh**

Lines 29–31, change:
```bash
if echo "$STAGED_KT" | grep -q "^composeApp/"; then
  echo "pre-commit: running :composeApp unit tests..."
  ./gradlew :composeApp:testDebugUnitTest --daemon --quiet
fi
```
To:
```bash
if echo "$STAGED_KT" | grep -q "^androidApp/"; then
  echo "pre-commit: running :androidApp unit tests..."
  ./gradlew :androidApp:testDebugUnitTest --daemon --quiet
fi
```

- [ ] **Step 6: Verify the rename**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. The `:composeApp` task no longer exists; `:androidApp` takes its place.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: rename composeApp → androidApp (JetBrains KMP 2026 init structure)"
```

---

## Task 3: Add FakeTokenProvider and FakeNetworkClient test helpers

**Files:**
- Create: `shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/core/test/FakeTokenProvider.kt`
- Create: `shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/core/test/FakeNetworkClient.kt`

These helpers are required by Tasks 7, 8, and 9.

- [ ] **Step 1: Create FakeTokenProvider.kt**

```kotlin
package com.yourcompany.kmptemplate.core.test

import com.yourcompany.kmptemplate.core.domain.TokenProvider

class FakeTokenProvider : TokenProvider {
    var accessToken: String? = null
    var refreshToken: String? = null
    var expiresAt: Long = 0L
    var cleared = false

    override suspend fun getAccessToken() = accessToken
    override suspend fun getRefreshToken() = refreshToken
    override suspend fun getExpiresAt() = expiresAt

    override suspend fun saveTokens(access: String, refresh: String, expiresAt: Long) {
        this.accessToken = access
        this.refreshToken = refresh
        this.expiresAt = expiresAt
    }

    override suspend fun clearTokens() {
        accessToken = null
        refreshToken = null
        cleared = true
    }
}
```

> **Note:** `NetworkClientImpl.tryRefreshTokens()` calls `tokenProvider.saveTokens(access, refresh)` with only two arguments, but the `TokenProvider` interface declares three. If this causes a compilation error in `NetworkClientImpl`, add `expiresAt: Long = 0L` as a default to the interface declaration in `TokenProvider.kt`. The fake above implements the full interface correctly.

- [ ] **Step 2: Create FakeNetworkClient.kt**

```kotlin
package com.yourcompany.kmptemplate.core.test

import com.yourcompany.kmptemplate.core.data.network.NetworkClient
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.reflect.TypeInfo

class FakeNetworkClient : NetworkClient() {

    private val queue = ArrayDeque<AppResult<*>>()

    override val httpClient: HttpClient
        get() = error("FakeNetworkClient: httpClient is not available")

    fun enqueue(vararg results: AppResult<*>) {
        queue.addAll(results.toList())
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> coreRequest(
        typeInfo: TypeInfo,
        block: HttpRequestBuilder.() -> Unit,
        shouldTriggerLogout: Boolean,
    ): AppResult<T> = (queue.removeFirstOrNull()
        ?: AppResult.Failure(CoreError.Network.NoConnection)) as AppResult<T>
}
```

- [ ] **Step 3: Verify both files compile**

```bash
./gradlew :shared:compileTestKotlinAndroid
```

Expected: `BUILD SUCCESSFUL` with no errors.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/core/test/FakeTokenProvider.kt \
        shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/core/test/FakeNetworkClient.kt
git commit -m "test: add FakeTokenProvider and FakeNetworkClient test helpers"
```

---

## Task 4: LoginUseCaseTest

**Files:**
- Create: `shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/domain/usecase/LoginUseCaseTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.yourcompany.kmptemplate.auth.domain.usecase

import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.domain.port.OAuthFlowLauncher
import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LoginUseCaseTest {

    private val repository = mock<OAuthRepository>()
    private val flowLauncher = mock<OAuthFlowLauncher>()
    private val useCase = LoginUseCase(repository, flowLauncher)

    @Test
    fun `success - launcher returns code, repository succeeds, result is Success`() = runTest {
        everySuspend { flowLauncher.launch(any()) } returns "auth-code"
        everySuspend { repository.login(any(), any(), any()) } returns AppResult.Success(Unit)

        val result = useCase(OAuthProvider.GOOGLE, "client-id")

        result.shouldBeInstanceOf<AppResult.Success<Unit>>()
    }

    @Test
    fun `launcher throws - result wrapped as NoConnection failure`() = runTest {
        everySuspend { flowLauncher.launch(any()) } throws RuntimeException("flow cancelled")

        val result = useCase(OAuthProvider.GOOGLE, "client-id")

        result shouldBe AppResult.Failure(CoreError.Network.NoConnection)
    }

    @Test
    fun `repository returns Failure - failure is propagated`() = runTest {
        everySuspend { flowLauncher.launch(any()) } returns "auth-code"
        everySuspend { repository.login(any(), any(), any()) } returns AppResult.Failure(CoreError.Unauthorized)

        val result = useCase(OAuthProvider.GOOGLE, "client-id")

        result shouldBe AppResult.Failure(CoreError.Unauthorized)
    }
}
```

- [ ] **Step 2: Run and confirm all three tests pass**

```bash
./gradlew :shared:testDebugUnitTest --tests "*.LoginUseCaseTest"
```

Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/domain/usecase/LoginUseCaseTest.kt
git commit -m "test: add LoginUseCaseTest"
```

---

## Task 5: LogoutUseCaseTest and RefreshTokenUseCaseTest

**Files:**
- Create: `shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/domain/usecase/LogoutUseCaseTest.kt`
- Create: `shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/domain/usecase/RefreshTokenUseCaseTest.kt`

- [ ] **Step 1: Create LogoutUseCaseTest.kt**

```kotlin
package com.yourcompany.kmptemplate.auth.domain.usecase

import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LogoutUseCaseTest {

    private val repository = mock<OAuthRepository>()
    private val useCase = LogoutUseCase(repository)

    @Test
    fun `delegates to repository logout and returns Success`() = runTest {
        everySuspend { repository.logout() } returns AppResult.Success(Unit)

        val result = useCase()

        result shouldBe AppResult.Success(Unit)
    }

    @Test
    fun `repository Failure is propagated`() = runTest {
        everySuspend { repository.logout() } returns AppResult.Failure(CoreError.Network.NoConnection)

        val result = useCase()

        result shouldBe AppResult.Failure(CoreError.Network.NoConnection)
    }
}
```

- [ ] **Step 2: Create RefreshTokenUseCaseTest.kt**

```kotlin
package com.yourcompany.kmptemplate.auth.domain.usecase

import com.yourcompany.kmptemplate.auth.domain.repository.OAuthRepository
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RefreshTokenUseCaseTest {

    private val repository = mock<OAuthRepository>()
    private val useCase = RefreshTokenUseCase(repository)

    @Test
    fun `delegates to repository refreshToken and returns Success`() = runTest {
        everySuspend { repository.refreshToken() } returns AppResult.Success(Unit)

        val result = useCase()

        result shouldBe AppResult.Success(Unit)
    }

    @Test
    fun `delegates to repository refreshToken and returns Failure`() = runTest {
        everySuspend { repository.refreshToken() } returns AppResult.Failure(CoreError.Unauthorized)

        val result = useCase()

        result shouldBe AppResult.Failure(CoreError.Unauthorized)
    }
}
```

- [ ] **Step 3: Run both test classes**

```bash
./gradlew :shared:testDebugUnitTest --tests "*.LogoutUseCaseTest" --tests "*.RefreshTokenUseCaseTest"
```

Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/domain/usecase/LogoutUseCaseTest.kt \
        shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/domain/usecase/RefreshTokenUseCaseTest.kt
git commit -m "test: add LogoutUseCaseTest and RefreshTokenUseCaseTest"
```

---

## Task 6: AuthViewModelTest

**Files:**
- Create: `shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/presentation/AuthViewModelTest.kt`

**Context:** `AuthViewModel` extends `BaseViewModel` which is a `KoinComponent` — it injects `GlobalUiEffectsHandler` via Koin. Tests must start a Koin context providing that handler. Navigation effects flow through `GlobalUiEffectsHandler`, not through the VM's own `effects` flow (`AuthEffect` is an empty sealed interface).

- [ ] **Step 1: Create AuthViewModelTest.kt**

```kotlin
package com.yourcompany.kmptemplate.auth.presentation

import app.cash.turbine.test
import com.yourcompany.kmptemplate.auth.domain.errors.AuthError
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.domain.usecase.LoginUseCase
import com.yourcompany.kmptemplate.auth.domain.usecase.LogoutUseCase
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffect
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffectsHandler
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import com.yourcompany.kmptemplate.core.navigation.AuthDestination
import com.yourcompany.kmptemplate.core.navigation.TopLevelGraph
import com.yourcompany.kmptemplate.core.test.BaseViewModelTest
import com.yourcompany.kmptemplate.core.test.startTestKoin
import com.yourcompany.kmptemplate.core.test.stopTestKoin
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceUntilIdle
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthViewModelTest : BaseViewModelTest() {

    private val mockLoginUseCase = mock<LoginUseCase>()
    private val mockLogoutUseCase = mock<LogoutUseCase>()
    private lateinit var globalEffectsHandler: GlobalUiEffectsHandler
    private lateinit var viewModel: AuthViewModel

    @BeforeTest
    fun setUpKoin() {
        globalEffectsHandler = GlobalUiEffectsHandler()
        startTestKoin(module {
            single { globalEffectsHandler }
        })
        viewModel = AuthViewModel(mockLoginUseCase, mockLogoutUseCase)
    }

    @AfterTest
    fun tearDownKoin() {
        viewModel.onCleared()
        stopTestKoin()
    }

    @Test
    fun `initial state has isLoading false and no error`() = runTest {
        viewModel.awaitState { state ->
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `login success emits NavigateTo Dashboard`() = runTest {
        everySuspend { mockLoginUseCase.invoke(any(), any()) } returns AppResult.Success(Unit)

        globalEffectsHandler.effects.test {
            viewModel.onAction(AuthAction.LoginWith(OAuthProvider.GOOGLE))
            advanceUntilIdle()

            val effect = awaitItem()
            assertIs<GlobalUiEffect.NavigateTo>(effect)
            effect.destination shouldBe TopLevelGraph.Dashboard
        }
    }

    @Test
    fun `login failure sets error state and clears loading`() = runTest {
        everySuspend { mockLoginUseCase.invoke(any(), any()) } returns
            AppResult.Failure(CoreError.Network.NoConnection)

        viewModel.onAction(AuthAction.LoginWith(OAuthProvider.GOOGLE))
        advanceUntilIdle()

        viewModel.awaitState { state ->
            assertFalse(state.isLoading)
            assertNotNull(state.error)
        }
    }

    @Test
    fun `login OAuthCancelled clears loading without setting error`() = runTest {
        everySuspend { mockLoginUseCase.invoke(any(), any()) } returns
            AppResult.Failure(AuthError.OAuthCancelled)

        viewModel.onAction(AuthAction.LoginWith(OAuthProvider.GOOGLE))
        advanceUntilIdle()

        viewModel.awaitState { state ->
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `logout emits NavigateBackTo Login inclusive`() = runTest {
        everySuspend { mockLogoutUseCase.invoke() } returns AppResult.Success(Unit)

        globalEffectsHandler.effects.test {
            viewModel.onAction(AuthAction.Logout)
            advanceUntilIdle()

            val effect = awaitItem()
            assertIs<GlobalUiEffect.NavigateBackTo>(effect)
            effect.destination shouldBe AuthDestination.Login
            assertTrue(effect.inclusive)
        }
    }
}
```

- [ ] **Step 2: Run and confirm all five tests pass**

```bash
./gradlew :shared:testDebugUnitTest --tests "*.AuthViewModelTest"
```

Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/presentation/AuthViewModelTest.kt
git commit -m "test: add AuthViewModelTest"
```

---

## Task 7: OAuthRepositoryImplTest

**Files:**
- Create: `shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/data/repository/OAuthRepositoryImplTest.kt`

**Context:** `OAuthRepositoryImpl.init {}` launches a coroutine on `Dispatchers.Default` to bootstrap the stored token. With an empty `FakeTokenProvider` this is a no-op (no stored access token → returns immediately). `AuthResponse` is `internal` — accessible from `commonTest` since it's the same Gradle module.

- [ ] **Step 1: Create OAuthRepositoryImplTest.kt**

```kotlin
package com.yourcompany.kmptemplate.auth.data.repository

import com.yourcompany.kmptemplate.auth.data.remote.dto.AuthResponse
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import com.yourcompany.kmptemplate.core.test.FakeNetworkClient
import com.yourcompany.kmptemplate.core.test.FakeTokenProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OAuthRepositoryImplTest {

    private val fakeNetworkClient = FakeNetworkClient()
    private val fakeTokenProvider = FakeTokenProvider()
    private val repository = OAuthRepositoryImpl(fakeNetworkClient, fakeTokenProvider)

    @Test
    fun `login success persists token and returns Success`() = runTest {
        fakeNetworkClient.enqueue(
            AppResult.Success(
                AuthResponse(
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                    expiresIn = 3600L,
                )
            )
        )

        val result = repository.login("auth-code", "verifier", OAuthProvider.GOOGLE)

        result.shouldBeInstanceOf<AppResult.Success<Unit>>()
        assertEquals("access-token", fakeTokenProvider.accessToken)
        assertEquals("refresh-token", fakeTokenProvider.refreshToken)
    }

    @Test
    fun `login network failure propagates Failure`() = runTest {
        fakeNetworkClient.enqueue(AppResult.Failure(CoreError.Network.NoConnection))

        val result = repository.login("auth-code", "verifier", OAuthProvider.GOOGLE)

        result shouldBe AppResult.Failure(CoreError.Network.NoConnection)
        assertNull(fakeTokenProvider.accessToken)
    }

    @Test
    fun `logout clears token provider and returns Success`() = runTest {
        fakeTokenProvider.accessToken = "old-access"
        fakeTokenProvider.refreshToken = "old-refresh"

        val result = repository.logout()

        result shouldBe AppResult.Success(Unit)
        assertTrue(fakeTokenProvider.cleared)
        assertNull(fakeTokenProvider.accessToken)
    }

    @Test
    fun `refreshToken returns Unauthorized when no refresh token stored`() = runTest {
        fakeTokenProvider.refreshToken = null

        val result = repository.refreshToken()

        result shouldBe AppResult.Failure(CoreError.Unauthorized)
    }

    @Test
    fun `refreshToken delegates to networkClient when refresh token present`() = runTest {
        fakeTokenProvider.refreshToken = "stored-refresh"
        fakeNetworkClient.enqueue(AppResult.Success(Unit))

        val result = repository.refreshToken()

        result.shouldBeInstanceOf<AppResult.Success<Unit>>()
    }
}
```

- [ ] **Step 2: Run and confirm all five tests pass**

```bash
./gradlew :shared:testDebugUnitTest --tests "*.OAuthRepositoryImplTest"
```

Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/auth/data/repository/OAuthRepositoryImplTest.kt
git commit -m "test: add OAuthRepositoryImplTest"
```

---

## Task 8: BearerAuthPluginTest

**Files:**
- Create: `shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/core/data/network/BearerAuthPluginTest.kt`

- [ ] **Step 1: Create BearerAuthPluginTest.kt**

```kotlin
package com.yourcompany.kmptemplate.core.data.network

import com.yourcompany.kmptemplate.core.test.FakeTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BearerAuthPluginTest {

    @Test
    fun `injects Bearer token in Authorization header when access token present`() = runTest {
        val fakeTokenProvider = FakeTokenProvider().apply { accessToken = "my-token" }
        var capturedHeader: String? = null

        val client = HttpClient(MockEngine { request ->
            capturedHeader = request.headers[HttpHeaders.Authorization]
            respond("", HttpStatusCode.OK)
        }) {
            install(BearerAuthPlugin) {
                tokenProvider = fakeTokenProvider
            }
        }

        client.get("/test")

        assertEquals("Bearer my-token", capturedHeader)
    }

    @Test
    fun `does not inject Authorization header when no access token`() = runTest {
        val fakeTokenProvider = FakeTokenProvider() // accessToken is null
        var authHeaderPresent = false

        val client = HttpClient(MockEngine { request ->
            authHeaderPresent = request.headers.contains(HttpHeaders.Authorization)
            respond("", HttpStatusCode.OK)
        }) {
            install(BearerAuthPlugin) {
                tokenProvider = fakeTokenProvider
            }
        }

        client.get("/test")

        assertFalse(authHeaderPresent)
    }
}
```

- [ ] **Step 2: Run and confirm both tests pass**

```bash
./gradlew :shared:testDebugUnitTest --tests "*.BearerAuthPluginTest"
```

Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/core/data/network/BearerAuthPluginTest.kt
git commit -m "test: add BearerAuthPluginTest"
```

---

## Task 9: NetworkClientImplTest

**Files:**
- Create: `shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/core/data/network/NetworkClientImplTest.kt`

**Context:** Tests use a `MockEngine` with a response queue to simulate multi-step request sequences (initial request → token refresh → retry). The 401+refresh flow in `NetworkClientImpl` makes three sequential HTTP calls: the original request (→ 401), the refresh request to `/auth/refresh` (→ 200 with new tokens), and the retried original request (→ 200).

- [ ] **Step 1: Create NetworkClientImplTest.kt**

```kotlin
package com.yourcompany.kmptemplate.core.data.network

import app.cash.turbine.test
import com.yourcompany.kmptemplate.core.domain.AppResult
import com.yourcompany.kmptemplate.core.domain.CoreError
import com.yourcompany.kmptemplate.core.test.FakeTokenProvider
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NetworkClientImplTest {

    private val fakeTokenProvider = FakeTokenProvider()
    private val globalEffectsHandler = GlobalUiEffectsHandler()

    private fun buildClient(responses: List<Pair<HttpStatusCode, String>>): NetworkClientImpl {
        var callIndex = 0
        val engine = MockEngine { _ ->
            val (status, body) = responses[callIndex.coerceAtMost(responses.size - 1)]
            callIndex++
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        return NetworkClientImpl(httpClient, fakeTokenProvider, globalEffectsHandler)
    }

    @Test
    fun `successful 200 response returns Success`() = runTest {
        val client = buildClient(listOf(HttpStatusCode.OK to "{}"))

        val result = client.request<Unit> { url("/test") }

        assertIs<AppResult.Success<Unit>>(result)
    }

    @Test
    fun `401 with valid refresh token retries and returns Success`() = runTest {
        fakeTokenProvider.accessToken = "old-access"
        fakeTokenProvider.refreshToken = "stored-refresh"

        val client = buildClient(listOf(
            HttpStatusCode.Unauthorized to "",
            HttpStatusCode.OK to """{"access_token":"new-access","refresh_token":"new-refresh"}""",
            HttpStatusCode.OK to "{}",
        ))

        val result = client.request<Unit> { url("/test") }

        assertIs<AppResult.Success<Unit>>(result)
        assertEquals("new-access", fakeTokenProvider.accessToken)
    }

    @Test
    fun `401 with no refresh token emits Unauthorized effect and returns Failure`() = runTest {
        fakeTokenProvider.refreshToken = null

        val client = buildClient(listOf(HttpStatusCode.Unauthorized to ""))

        globalEffectsHandler.effects.test {
            val result = client.request<Unit> { url("/test") }

            result shouldBe AppResult.Failure(CoreError.Unauthorized)
            assertIs<GlobalUiEffect.Unauthorized>(awaitItem())
        }
    }

    @Test
    fun `unauthorizedRequest returns Unauthorized Failure without retrying on 401`() = runTest {
        val client = buildClient(listOf(HttpStatusCode.Unauthorized to ""))

        val result = client.unauthorizedRequest<Unit> { url("/auth/login") }

        result shouldBe AppResult.Failure(CoreError.Unauthorized)
    }

    @Test
    fun `unknown exception is mapped to NoConnection failure`() = runTest {
        val engine = MockEngine { throw RuntimeException("network unreachable") }
        val httpClient = HttpClient(engine) {}
        val client = NetworkClientImpl(httpClient, fakeTokenProvider, globalEffectsHandler)

        val result = client.request<Unit> { url("/test") }

        result shouldBe AppResult.Failure(CoreError.Network.NoConnection)
    }
}
```

- [ ] **Step 2: Run and confirm all five tests pass**

```bash
./gradlew :shared:testDebugUnitTest --tests "*.NetworkClientImplTest"
```

Expected: `BUILD SUCCESSFUL`, 5 tests passed.

- [ ] **Step 3: Run the full shared test suite to confirm no regressions**

```bash
./gradlew :shared:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, all tests passed.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonTest/kotlin/com/yourcompany/kmptemplate/core/data/network/NetworkClientImplTest.kt
git commit -m "test: add NetworkClientImplTest covering 401 refresh/retry flow"
```

---

## Done

At this point:
- Kotlin 2.3.21, KSP 2.3.8, SKIE 0.10.12 are active
- `koin-ksp-compiler` resolves correctly at 2.3.1
- The module is named `:androidApp` throughout
- Auth stack has full vertical test coverage: use cases, ViewModel, repository, plugin, network client
