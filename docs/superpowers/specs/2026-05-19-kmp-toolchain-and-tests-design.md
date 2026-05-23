# KMP Toolchain Bump, Module Rename & Auth Test Coverage

**Date:** 2026-05-19
**Branch:** feat/gpd9

---

## 1. Toolchain version bumps

### Changes to `gradle/libs.versions.toml`

| Key | Before | After | Notes |
|-----|--------|-------|-------|
| `kotlin` | `2.3.20` | `2.3.21` | SKIE 0.10.12 now supports 2.3.21; remove pinned comment |
| `ksp` | `2.3.7` | `2.3.8` | Latest in 2.3.x line; standalone semver, not tied to Kotlin |
| `skie` | `0.10.11` | `0.10.12` | Adds Kotlin 2.3.21 support |
| `koin-ksp-compiler` | *(new entry)* | `2.3.1` | Split from `koin-annotations`; `koin-ksp-compiler` artifact has NOT merged to 4.x versioning — still published on 2.x line |

The `koin-ksp-compiler` library entry in `[libraries]` must be updated to use the new dedicated version ref:

```toml
# versions
koin-ksp-compiler = "2.3.1"

# libraries
koin-ksp-compiler = { module = "io.insert-koin:koin-ksp-compiler", version.ref = "koin-ksp-compiler" }
```

The comment `# Koin and Koin Annotations merged versioning in 4.x` is updated to clarify that only `koin-annotations` merged; `koin-ksp-compiler` remains on 2.x.

---

## 2. Module rename: `composeApp` → `androidApp`

Aligns with the JetBrains new KMP default init structure (May 2026), which separates the shared library module from platform-specific app modules. The module is already architecturally correct (pure `android.application`, no KMP plugins); this is a naming-only change.

### Touch points

| File | Change |
|------|--------|
| `composeApp/` directory | Rename to `androidApp/` |
| `settings.gradle.kts` | `include(":composeApp")` → `include(":androidApp")` |
| `fastlane/Fastfile:20` | AAB path `composeApp/build/...` → `androidApp/build/...` |
| `scripts/create-feature.sh:13,75,77,82` | All `composeApp` path references → `androidApp` |
| `scripts/pre-commit.sh:29,30,31` | Staged-file prefix check and Gradle task → `:androidApp` |

No changes to `androidApp/build.gradle.kts` content — `namespace`, `applicationId`, and all dependencies are unchanged.

---

## 3. Auth stack test coverage

All new tests live in `commonTest` (pure KMP). No Robolectric/`androidHostTest` required — the auth stack has no direct Room or Android Context dependency.

### Test files

#### `auth/domain/usecase/LoginUseCaseTest.kt`
Covers:
- Success: repository returns token → stored, `Success(Unit)` returned
- Network failure: repository returns `Failure(CoreError.Network.*)` → propagated
- Storage failure: token store throws → wrapped as `Failure`

Uses: Mokkery mocks for `OAuthRepository` and token storage port.

#### `auth/domain/usecase/LogoutUseCaseTest.kt`
Covers:
- Success: clears stored token, returns `Success(Unit)`
- Repository error: propagated without swallowing

#### `auth/domain/usecase/RefreshTokenUseCaseTest.kt`
Covers:
- Success: new token stored and returned
- No existing token: returns appropriate `Failure`
- Network error during refresh: propagated

#### `auth/presentation/AuthViewModelTest.kt`
Extends `BaseViewModelTest`. Covers:
- Initial state is correct
- `Login` action → loading state → success state
- `Login` action → loading state → error effect emitted on failure
- `Logout` action → clears state, navigates away via effect

Uses Turbine via `awaitState` / `awaitEffect` helpers already on `BaseViewModelTest`.

#### `auth/data/repository/OAuthRepositoryImplTest.kt`
Covers:
- HTTP 200 → token parsed and returned as `Success`
- HTTP 401 → mapped to `Failure(AuthError.*)`
- Network timeout → mapped to `Failure(CoreError.Network.Timeout)`

Uses `FakeHttpClientEngine` already in `commonTest`.

#### `core/data/network/BearerAuthPluginTest.kt`
Covers:
- Token injected into `Authorization` header on authenticated requests
- 401 response triggers one refresh attempt and retries original request
- Refresh failure emits `GlobalUiEffect.SessionExpired` and does not retry

Uses `FakeHttpClientEngine` with configurable response sequences.

### No new dependencies required

All tooling (`kotest-assertions`, `turbine`, `kotlinx-coroutines-test`, `ktor-client-mock`, `koin-test`, `mokkery`) is already declared in `commonTest.dependencies`.

---

## Out of scope

- iOS-specific test targets (no `iosTest` source set added — common tests run on all targets via `commonTest`)
- `androidHostTest` additions (Room/Context tests already have `RoomTestBuilder`; no new Room tests in this spec)
- Any new features, refactors, or dependency additions beyond what is listed above
