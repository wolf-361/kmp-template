# ADR-007: Dependency Injection — Koin with Annotations

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The project needs a dependency injection framework that:
- Works in `commonMain` — shared ViewModels, UseCases, and Repositories must be injectable.
- Is compatible with Android and iOS (via SKIE).
- Minimises boilerplate — ideally removes manual `module { }` DSL blocks.
- Supports a modular, per-feature DI setup.
- Verifies the DI graph at compile time or test time.

---

## Decision Drivers

1. KMP-native — modules declared in `commonMain`, no platform duplication.
2. Annotation-driven — KSP generates bindings; no manual `singleOf`/`factoryOf` lines per class.
3. Compile-time safety — annotations catch missing bindings before runtime (via KSP errors or `checkModules`).
4. Per-feature module isolation.
5. `BaseViewModel` must access shared services (e.g., `StringProvider`) without requiring them as constructor parameters on every ViewModel.

---

## Considered Options

### Option A: Koin DSL only

```kotlin
val authModule = module {
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    factoryOf(::LoginUseCase)
    viewModelOf(::AuthViewModel)
}
```

**Cons:** Every new class requires a manual DSL entry; easy to forget when adding a new UseCase; no compile-time verification.

---

### Option B: Koin with Annotations + KSP — **CHOSEN**

Koin Annotations (`koin-annotations` + `koin-ksp-compiler`) generates Koin module code at KSP time from `@Single`, `@Factory`, `@KoinViewModel`, and `@Module` annotations. KSP is already required for Room, so no extra Gradle plugin is needed.

**Pros:**
- Near-zero manual DI boilerplate per class.
- KSP verifies bindings at compile time — missing dependencies are build errors, not runtime crashes.
- `@Module @ComponentScan` auto-discovers all annotated classes in a package.
- Fully compatible with `checkModules { }` for integration tests.

**Cons:**
- Additional `koin-annotations` artifact.
- `kspCommonMainMetadata` configuration required in `shared/build.gradle.kts`.

---

## Decision Outcome

**Use Koin 4.x + Koin Annotations 2.x.**

### Gradle setup

```kotlin
// shared/build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version libs.versions.ksp.get()
}

commonMain.dependencies {
    api(libs.koin.core)
    api(libs.koin.annotations)
}

dependencies {
    // KSP processes commonMain metadata — generates module code
    kspCommonMainMetadata(libs.koin.ksp.compiler)
}
```

```toml
# libs.versions.toml
koin              = "4.2.x"
koin-annotations  = "4.2.x"   # aligned with koin-core since 4.x
ksp               = "2.3.7"   # standalone semver since KSP 2.3.x (no longer tied to Kotlin version)
```

---

### Feature module pattern

Each feature declares a single `@Module` class that uses `@ComponentScan` to auto-discover its annotated classes:

```kotlin
// features/auth/AuthModule.kt
@Module
@ComponentScan("com.company.app.features.auth")
class AuthModule
```

Koin Annotations scans the package and generates bindings for all annotated classes found within it.

---

### Class-level annotations

```kotlin
// Single — one instance shared across the app
@Single
@Binds(binds = [AuthRepository::class])
class AuthRepositoryImpl(
    private val networkClient: NetworkClient,
    private val authDao: AuthDao,
    private val tokenProvider: TokenProvider,
) : AuthRepository

// Factory — new instance per injection point
@Factory
class LoginUseCase(private val repository: AuthRepository)

@Factory
class AuthValidator

// ViewModel — lifecycle-aware, one per ViewModelStoreOwner
@KoinViewModel
class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val validator: AuthValidator,
) : BaseViewModel<AuthState, AuthAction, AuthEffect>(AuthState())
```

---

### BaseViewModel — KoinComponent for shared services

`BaseViewModel` implements `KoinComponent` to inject shared services (e.g., `StringProvider`) without requiring them as constructor parameters on every ViewModel. This keeps `@KoinViewModel` declarations lean.

```kotlin
abstract class BaseViewModel<St : UiState, A : UiAction, E : UiEffect>(
    initialState: St,
) : ViewModel(), KoinComponent {

    // Injected once by Koin — not a constructor parameter
    private val stringProvider: StringProvider by inject()
    protected val S: StringProvider get() = stringProvider

    // …state, effects, navigateTo(), etc.
}
```

Feature ViewModels only declare their own use cases:

```kotlin
@KoinViewModel
class AuthViewModel(
    private val loginUseCase: LoginUseCase,
) : BaseViewModel<AuthState, AuthAction, AuthEffect>(AuthState())
// No StringProvider in constructor — BaseViewModel gets it from Koin
```

---

### Core module (manually declared for framework-level singletons)

Koin Annotations handles per-feature classes. Framework-level singletons that require platform-specific construction (logger, network client, database) use explicit `@Module` methods:

```kotlin
// core/di/CoreModule.kt
@Module
class CoreModule {
    @Single
    fun provideStringProvider(): StringProvider = // platform-specific factory
        getKoin().get<StringProviderFactory>().create()

    @Single
    fun provideAppLogger(factory: LoggerFactory): AppLogger =
        AppLoggerImpl(factory.build())

    @Single
    fun provideNetworkClient(
        tokenProvider: TokenProvider,
        logger: AppLogger,
    ): NetworkClient = NetworkClientImpl(tokenProvider, logger)
}
```

---

### App-level initialization

```kotlin
// shared/core/di/KoinInit.kt
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        CoreModule().module,        // generated by KSP
        NetworkModule().module,
        DatabaseModule().module,
        StorageModule().module,
        AuthModule().module,
        CourseModule().module,
        // add new feature modules here
    )
}
```

```kotlin
// androidMain — Application.onCreate()
initKoin {
    androidContext(this@App)
    androidLogger()
}

// iosMain — called from Swift @main.init()
// KoinInitKt.initKoin()
```

---

### DI graph verification

```kotlin
// commonTest — core/di/KoinModulesTest.kt
class KoinModulesTest : KoinTest {
    @Test
    fun `koin graph is valid`() = checkModules {
        modules(testModule + appModules)
        // Provide mock ViewModelStoreOwner for ViewModel validation
        withInstance<Context>(mockContext)
    }
}
```

`testModule` overrides platform-specific singletons with `PrintLogger` and `FakeStringProvider`.

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Koin DSL only | Manual boilerplate per class; no compile-time verification |
| Hilt | Android-only; incompatible with `commonMain` |
| Kodein | Lower adoption; less tooling support |
| Manual DI (factories) | Does not scale to feature-driven architecture |
