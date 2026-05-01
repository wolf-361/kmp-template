# KMP Production Template — Architecture

> **Stack:** AGP 9.2 · Kotlin 2.3 · Compose Multiplatform 1.9

---

## Table of Contents

1. [Philosophy](#1-philosophy)
2. [Module Structure](#2-module-structure)
3. [Tech Stack](#3-tech-stack)
4. [Layer Architecture](#4-layer-architecture)
5. [Feature Module Layout](#5-feature-module-layout)
6. [Mapper Responsibilities](#6-mapper-responsibilities)
7. [Data Flow & Error Handling](#7-data-flow--error-handling)
8. [MVI Pattern](#8-mvi-pattern)
9. [Navigation](#9-navigation)
10. [Resource Management & I18n](#10-resource-management--i18n)
11. [Storage Strategy](#11-storage-strategy)
12. [Logging](#12-logging)
13. [Testing Standards](#13-testing-standards)
14. [CI/CD Pipeline](#14-cicd-pipeline)
15. [Golden Rules](#15-golden-rules)

---

## 1. Philosophy

**Feature-First Clean Architecture on Kotlin Multiplatform.** → [ADR-009](adrs/ADR-009-architecture-pattern.md)

- **The brain is shared.** All business logic, DI, ViewModels, use cases, repositories, and data sources live in `shared/commonMain`.
- **The face is native.** UI is coded natively: Jetpack Compose for Android, SwiftUI for iOS.
- **Offline-first by default.** The database is the single source of truth. Network syncs into the DB; the DB feeds the domain. → [ADR-003](adrs/ADR-003-local-storage.md)

```
┌─────────────────────────────────────────┐
│             Native UI Layer             │
│   Android (Compose)    iOS (SwiftUI)    │
└──────────────┬──────────────┬───────────┘
               │  State       │  Actions
┌──────────────▼──────────────▼───────────┐
│         Shared / Presentation           │
│  ViewModel (State · Action · Effect)    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           Shared / Domain               │
│   UseCases · Models · Repository (i/f)  │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│            Shared / Data                │
│  RepositoryImpl · Room DB · Ktor API    │
└─────────────────────────────────────────┘
```

---

## 2. Module Structure

→ [ADR-004](adrs/ADR-004-module-structure.md)

```
root/
├── shared/          ← KMP module: ALL shared code
│   └── src/
│       ├── commonMain/
│       ├── commonTest/
│       ├── androidMain/
│       ├── androidUnitTest/
│       └── iosMain/
├── composeApp/      ← Android app: Compose UI only
└── iosApp/          ← Xcode project: SwiftUI (SKIE binding)
```

Feature boundaries are enforced by package structure. The upgrade path to Gradle feature modules is documented in ADR-004 and becomes relevant beyond ~15 features or when CI build time exceeds 3 minutes.

---

## 3. Tech Stack

Every library choice is backed by an ADR.

| Category | Library | Version | ADR |
|:---|:---|:---|:---|
| **Language** | Kotlin | 2.3.20 | [ADR-014](adrs/ADR-014-build-toolchain.md) |
| **Build** | AGP | 9.2.x | [ADR-014](adrs/ADR-014-build-toolchain.md) |
| **UI framework** | Compose Multiplatform | 1.10.x | [ADR-014](adrs/ADR-014-build-toolchain.md) |
| **DI** | Koin + Koin Annotations | 4.2.x (versions aligned) | [ADR-007](adrs/ADR-007-dependency-injection.md) |
| **Network** | Ktor (Route pattern) | 3.4.x | [ADR-008](adrs/ADR-008-networking.md) |
| **Database** | Room KMP | 2.8.x | [ADR-003](adrs/ADR-003-local-storage.md) |
| **Preferences** | DataStore | 1.2.x | [ADR-003](adrs/ADR-003-local-storage.md) |
| **Logging** | Kermit | 2.0.x | [ADR-002](adrs/ADR-002-logging.md) |
| **Resources** | Compose Resources | 1.9.x | [ADR-001](adrs/ADR-001-resource-management.md) |
| **Dates** | kotlinx-datetime | 0.7.x | [ADR-014](adrs/ADR-014-build-toolchain.md) |
| **Async** | Coroutines + Flow | 1.10.x | [ADR-014](adrs/ADR-014-build-toolchain.md) |
| **Swift bridge** | SKIE (Touchlab) | 0.10.x | [ADR-012](adrs/ADR-012-swift-interop.md) |
| **Serialization** | kotlinx-serialization | 1.10.x | [ADR-014](adrs/ADR-014-build-toolchain.md) |
| **Code quality** | Detekt + Spotless | 1.23 / 8.x | [ADR-013](adrs/ADR-013-code-quality.md) |
| **Testing** | Mokkery + Kotest + Turbine | 3.x / 6.x / 1.2.x | [ADR-011](adrs/ADR-011-testing-stack.md) |

### AGP 9.2 Hard Requirements

- Gradle **9.4.1** or higher
- `compileSdk` **36** minimum
- `namespace` mandatory in every `build.gradle.kts`
- Java **17** source/target (`compileOptions`)
- All deprecated AGP 8.x DSL replaced with 9.x equivalents

---

## 4. Layer Architecture

→ [ADR-009](adrs/ADR-009-architecture-pattern.md)

Each feature is a vertical slice with three layers: **data**, **domain**, **presentation**.

```
Presentation  →  ViewModel observes State; sends Actions; emits Effects
Domain        →  UseCase orchestrates Repository calls; returns AppResult<T>
Data          →  RepositoryImpl: Ktor (remote) + Room (local) → Domain models
```

### Clean Boundaries

- `domain` has **zero** framework imports — only `kotlinx.datetime`, `kotlinx.coroutines`, and project-internal models.
- `data` may import Room, Ktor, and DataStore. Never Compose.
- `presentation` may import Compose Resources (`Res.string.*`) for resolving strings via `StringProvider`. Never Room or Ktor.

---

## 5. Feature Module Layout

→ [ADR-009](adrs/ADR-009-architecture-pattern.md)

```
features/
└── auth/
    ├── AuthModule.kt                 ← Koin module
    ├── data/
    │   ├── local/
    │   │   ├── AuthDao.kt
    │   │   └── UserEntity.kt
    │   ├── remote/
    │   │   ├── AuthRoutes.kt             ← string constants (internal)
    │   │   ├── dto/
    │   │   │   └── UserDto.kt
    │   │   ├── requests/
    │   │   │   └── LoginRequest.kt
    │   │   └── responses/
    │   │       └── AuthResponse.kt
    │   ├── mappers/
    │   │   ├── AuthSyncMapper.kt     ← Dto ↔ Entity
    │   │   └── AuthEntityMapper.kt   ← Entity ↔ Domain
    │   └── AuthRepositoryImpl.kt
    ├── domain/
    │   ├── models/
    │   │   ├── User.kt
    │   │   └── errors/
    │   │       └── AuthError.kt
    │   ├── usecases/
    │   │   ├── LoginUseCase.kt
    │   │   ├── RegisterUseCase.kt
    │   │   └── LogoutUseCase.kt
    │   ├── validation/
    │   │   └── AuthValidator.kt
    │   └── AuthRepository.kt         ← Interface
    └── presentation/
        ├── contract/
        │   ├── AuthState.kt
        │   ├── AuthAction.kt
        │   ├── AuthEffect.kt
        │   └── AuthSheetState.kt     ← Sub-state for sheets/dialogs
        ├── mappers/
        │   └── AuthUiMappers.kt      ← AuthError → String ONLY
        ├── fixtures/
        │   └── AuthFixtures.kt
        └── AuthViewModel.kt
```

**No `utils/` or `helpers/` folders anywhere.** Extension functions live in domain-specific files:
- `core/domain/extensions/StringExtensions.kt`
- `core/domain/extensions/DateTimeExtensions.kt`

---

## 6. Mapper Responsibilities

→ [ADR-009](adrs/ADR-009-architecture-pattern.md)

### Rule: one direction, one responsibility

| Mapper | From → To | Lives In | Called By |
|:---|:---|:---|:---|
| **SyncMapper** | `Dto ↔ Entity` | `data/mappers/` | Repository (during network sync) |
| **EntityMapper** | `Entity ↔ Domain` | `data/mappers/` | Repository (when exposing Flows) |
| **UiMapper** | `DomainError → String` | `presentation/mappers/` | ViewModel only |

Never create a "Domain → PresentationModel" mapper unless the UI genuinely requires a flattened projection. The domain model goes directly into State.

---

## 7. Data Flow & Error Handling

→ [ADR-010](adrs/ADR-010-error-handling.md)

### A. AppResult

```kotlin
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()
}
```

### B. The `handle` DSL

```kotlin
loginUseCase(email, password).handle {
    success { user ->
        navigateTo(TopLevelGraph.Dashboard)
    }
    failure<AuthError.InvalidCredentials> {
        updateState { it.copy(passwordError = S.err_invalid_credentials) }
    }
    catch { error ->
        updateState { it.copy(globalError = error.asString()) }
    }
}
```

Network calls use string-constant routes co-located with the feature's data layer. The request shape is defined inline via a lambda:

```kotlin
// features/auth/data/remote/AuthRoutes.kt
internal object AuthRoutes {
    private const val BASE = "auth"
    const val LOGIN   = "$BASE/login"
    const val REFRESH = "$BASE/refresh"
    const val LOGOUT  = "$BASE/logout"
}

// Repository — login uses unauthorizedRequest (no 401→logout trigger)
networkClient.unauthorizedRequest<AuthResponse> {
    method = HttpMethod.Post
    url(AuthRoutes.LOGIN)
    setBody(LoginRequest(email, password))
}.map { it.toDomain() }
```

### C. Error Hierarchy

```
AppError (sealed)
├── CoreError
│   ├── Network (Timeout, NoConnection, BackendPayload(code, message))
│   ├── DataCorruption
│   ├── Database
│   └── Unauthorized
└── [Feature]Error    e.g. AuthError, CourseError
    ├── InvalidCredentials
    └── …
```

### D. Guard Pattern (ViewModel)

```
1. Local validation (Guard)  → if invalid: updateState + return
2. Show loading              → updateState { it.copy(isLoading = true) }
3. Call UseCase              → coroutine launch
4. Handle AppResult          → .handle { success {…} failure<E> {…} catch {…} }
5. Hide loading              → updateState { it.copy(isLoading = false) }
```

---

## 8. MVI Pattern

→ [ADR-009](adrs/ADR-009-architecture-pattern.md)

Every screen is governed by three contracts in `presentation/contract/`:

| Contract | Role | Constraint |
|:---|:---|:---|
| `XState` | Immutable snapshot of screen data | Contains `String?` for errors — never raw exceptions or resource references |
| `XAction` | User intent | Sealed interface; one class per distinct user gesture |
| `XEffect` | One-shot side effect | **UI-level events only** (dialogs, animations, scroll). Never navigation. |

**Navigation is never in `XEffect`.** Call `navigateTo(destination)` or `navigateBack()` directly on `BaseViewModel`. See [ADR-005](adrs/ADR-005-navigation.md).

```kotlin
data class AuthState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val globalError: String? = null,
) : UiState

sealed interface AuthAction : UiAction {
    data class OnEmailChanged(val value: String) : AuthAction
    data class OnPasswordChanged(val value: String) : AuthAction
    data object OnSubmit : AuthAction
}

// AuthEffect contains ONLY UI-level one-shot events — never navigation
sealed interface AuthEffect : UiEffect {
    data class ShowResetDialog(val email: String) : AuthEffect
    data object ShakePasswordField : AuthEffect
}
```

---

## 9. Navigation

→ [ADR-005](adrs/ADR-005-navigation.md)

All routes live in `shared/src/commonMain/…/core/navigation/Destinations.kt` as `@Serializable` sealed interface members. Navigation is called directly on `BaseViewModel`:

```kotlin
// Inside any ViewModel — no XEffect required
navigateTo(TopLevelGraph.Dashboard)
navigateBack()
navigateBackTo(AuthDestination.Login, inclusive = true)
```

Internally these emit on a `GlobalUiEffect` SharedFlow observed once at the app root (Android `NavHost`, iOS root `NavigationStack`). Feature code never interacts with `GlobalUiEffect` directly.

```kotlin
sealed interface AuthDestination : Destination {
    @Serializable data object Login : AuthDestination
    @Serializable data class PasswordReset(val email: String) : AuthDestination
}
```

---

## 10. Resource Management & I18n

→ [ADR-001](adrs/ADR-001-resource-management.md)

### S — synchronous string access

`val S = Res.string` (generated by Libres) exposes every string as a plain Kotlin property. ViewModels access strings with `S.key_name` — no coroutine, no Koin, no setup. The Compose UI layer receives only concrete `String` values.

```kotlin
// ViewModel
failure<AuthError.InvalidCredentials> {
    updateState { it.copy(emailError = S.err_invalid_credentials) }
}
```

```kotlin
// State — plain String, no wrappers
data class AuthState(val emailError: String? = null) : UiState
```

```kotlin
// Xcode Preview — plain String, no resource system involved
#Preview { LoginView(state: AuthState(emailError: "Invalid email")) }
```

See [ADR-001](adrs/ADR-001-resource-management.md) for platform implementations and the optional per-feature `AuthStrings` ergonomic wrapper.

### Resource file location

```
shared/src/commonMain/composeResources/
├── values/strings.xml           ← default (English)
├── values-fr/strings.xml        ← French
└── drawable/
```

---

## 11. Storage Strategy

→ [ADR-003](adrs/ADR-003-local-storage.md)

| Data type | Tool |
|:---|:---|
| Relational / structured | Room KMP |
| App preferences (reactive) | DataStore (Preferences) |
| Auth tokens / secrets | Keychain (iOS) / EncryptedSharedPreferences (Android) |
| Binary files / images | Platform filesystem (path stored in Room) |

### Logout contract (CRITICAL)

On `logout()`:
- **Delete:** access token, refresh token, session expiry.
- **Retain:** last-used email, user preferences, cached content.
- **Never call** `dataStore.edit { it.clear() }` or `roomDb.clearAllTables()` on logout.

---

## 12. Logging

→ [ADR-002](adrs/ADR-002-logging.md)

Kermit is wrapped behind an `AppLogger` interface for testability. Platform writers:
- Android: `LogcatWriter`
- iOS: `OSLogWriter`
- Tests: `PrintLogger` (stdout — logs are always visible, never silenced)
- Crash reporting (optional): `CrashlyticsLogWriter`

---

## 13. Testing Standards

→ [ADR-011](adrs/ADR-011-testing-stack.md)

### Stack

| Purpose | Library |
|:---|:---|
| Mocking | Mokkery |
| Assertions | Kotest Assertions |
| Flow testing | Turbine |
| Coroutines | kotlinx-coroutines-test |
| DI validation | Koin Test |
| Network mocks | Ktor MockEngine |
| DB testing | Room In-Memory |

### Naming convention

```kotlin
@Test
fun `should emit loading state when login is triggered`() = runTest { … }
```

Format: `` `should [expected behavior] when [condition]` ``

### AAA pattern — one Act per test

```kotlin
@Test
fun `should navigate to dashboard when login succeeds`() = runTest {
    // Arrange
    val viewModel = createViewModel()
    every { loginUseCase(any(), any()) } returns AppResult.Success(fakeUser)

    // Act
    viewModel.onAction(AuthAction.OnSubmit)

    // Assert — navigation emits on globalEffects, never on feature effects
    viewModel.globalEffects.test {
        awaitItem() shouldBe GlobalUiEffect.NavigateTo(TopLevelGraph.Dashboard)
    }
}
```

### Source set mapping

| Test type | Source set | Runs on |
|:---|:---|:---|
| Business logic | `commonTest` | JVM + iOS simulator |
| Android platform code | `androidUnitTest` | JVM |
| iOS platform code | Xcode Unit Tests | iOS simulator |

---

## 14. CI/CD Pipeline

→ [ADR-006](adrs/ADR-006-ci-cd.md)

| Workflow | Trigger | Runner |
|:---|:---|:---|
| `ci.yml` | PR to `main` or `dev` | `ubuntu-latest` |
| `release-android.yml` | Tag `v*` on `main` | `ubuntu-latest` |
| `release-ios.yml` | Tag `v*` on `main` | `macos-latest` |

Deployment is handled by **Fastlane** lanes called from GitHub Actions. Branch protection via **GitHub Rulesets** on both `main` and `dev` — free on all plans including private repositories (see ADR-006).

Local pre-commit: runs `spotlessCheck detekt detektDomainImports detektPresentationImports` always; unit tests only for staged Kotlin files in affected modules.

---

## 15. Golden Rules

1. **No `java.*` in `shared/commonMain`.** Use `kotlinx.datetime`. Enforced by Detekt `ForbiddenImport`.
2. **No `utils/` or `helpers/` packages.** Extension functions live in domain-specific files under the owning feature or `core/domain/extensions/`.
3. **Systematic mapping.** `Dto → Entity → Domain → State`. No skipping layers.
4. **All ViewModels extend `BaseViewModel`.** No raw `ViewModel()` inheritance.
5. **State holds `String?` for errors — resolved in the ViewModel via `S`.** Never `Exception`, never `StringResource`, never an un-resolved bridge object.
6. **Navigation is never in `XEffect`.** Use `navigateTo()` / `navigateBack()` on `BaseViewModel` directly.
7. **Logout clears tokens only.** `clearAll()` / `deleteAll()` requires an explicit "Delete account" user action.
8. **Local-first writes for domain data.** Auth/identity operations are network-first.
9. **`DataStore` instances are singletons.** Always bind with `single` in Koin Annotations — never `factory`.
10. **Tests always log to console.** Use `PrintLogger` — never silence logs in tests.
11. **`namespace` required in every `build.gradle.kts` module** (AGP 9.x hard requirement).
12. **No Compose imports in domain or data layers.** Only `presentation/` may reference `Res.string.*` via `StringProvider`.
