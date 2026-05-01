# ADR-005: Navigation Architecture

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

KMP navigation must satisfy two requirements:

1. **Shared route definitions** — the same `Destination` type is used from `commonMain` ViewModels without importing any platform framework.
2. **Platform execution** — the actual navigation call happens in platform-specific code (`NavController` on Android, `NavigationPath` on iOS).

A key design constraint: **feature-level Effect sealed interfaces must not contain navigation cases.** Navigation is a cross-cutting concern that all ViewModels share; it belongs in `BaseViewModel`, not scattered across feature contracts.

---

## Decision Drivers

1. Type-safe route arguments — no stringly-typed route strings.
2. Deep-link support — routes must be serialisable.
3. Zero platform-framework imports in `commonMain`.
4. Navigation is called directly from the ViewModel — not via a feature-specific `XEffect`.
5. Feature `XEffect` sealed interfaces contain only UI-level one-shot events (dialogs, animations, scroll-to-top).
6. Single source of truth for all routes.

---

## Navigation vs Effect — the rule

| Concern | Mechanism | Lives in |
|:---|:---|:---|
| **Navigation** | `navigateTo(destination)` on `BaseViewModel` | `BaseViewModel` |
| **Feature UI events** | `emitEffect(XEffect.ShowDialog)` | Feature `XEffect` |

Feature `XEffect` sealed interfaces must **never** contain:
- `NavigateToX` cases
- `NavigateBack`

These are always handled by `BaseViewModel.navigateTo()` / `navigateBack()` directly. `GlobalUiEffect.NavigateTo` exists as an implementation detail of `BaseViewModel` — it is not exposed to feature code.

---

## Considered Options

### Option A: Navigation via feature Effects

Each feature declares navigation cases in its own `XEffect`:
```kotlin
sealed interface AuthEffect {
    data object NavigateToDashboard : AuthEffect   // ← REJECTED
}
viewModel.emitEffect(AuthEffect.NavigateToDashboard)
```

**Rejected because:** Every native UI screen must observe two channels (global effects + feature effects) for navigation. Navigation logic is scattered across features. Testing navigation requires asserting on feature-specific effect types.

---

### Option B: Navigator interface injected into ViewModel

```kotlin
interface Navigator {
    fun navigateTo(destination: Destination)
    fun navigateBack()
}

class AuthViewModel(private val navigator: Navigator) : BaseViewModel()
```

**Rejected because:** `Navigator` must be injected into every ViewModel; the implementation must be provided per Activity/ViewController lifecycle which complicates Koin scoping with Koin Annotations.

---

### Option C: Navigation methods on BaseViewModel backed by GlobalUiEffect — **CHOSEN**

`BaseViewModel` exposes `navigateTo()` and `navigateBack()` as protected functions. Internally they emit on the `globalEffects` SharedFlow, which is observed once at the app root — not per feature.

---

## Decision Outcome

### BaseViewModel navigation API

```kotlin
// core/presentation/BaseViewModel.kt
abstract class BaseViewModel<St : UiState, A : UiAction, E : UiEffect>(
    initialState: St,
) : ViewModel(), KoinComponent {

    private val _globalEffects = MutableSharedFlow<GlobalUiEffect>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val globalEffects: SharedFlow<GlobalUiEffect> = _globalEffects.asSharedFlow()

    protected fun navigateTo(destination: Destination) {
        viewModelScope.launch { _globalEffects.emit(GlobalUiEffect.NavigateTo(destination)) }
    }

    protected fun navigateBack() {
        viewModelScope.launch { _globalEffects.emit(GlobalUiEffect.NavigateBack) }
    }

    protected fun navigateBackTo(destination: Destination, inclusive: Boolean = false) {
        viewModelScope.launch {
            _globalEffects.emit(GlobalUiEffect.NavigateBackTo(destination, inclusive))
        }
    }
}
```

### GlobalUiEffect (shared)

```kotlin
sealed interface GlobalUiEffect {
    data class NavigateTo(val destination: Destination) : GlobalUiEffect
    data object NavigateBack : GlobalUiEffect
    data class NavigateBackTo(val destination: Destination, val inclusive: Boolean) : GlobalUiEffect
    data class ShowToast(val message: String) : GlobalUiEffect
    data class ShowSnackbar(val message: String) : GlobalUiEffect
    data object Unauthorized : GlobalUiEffect
}
```

### ViewModel usage

```kotlin
@KoinViewModel
class AuthViewModel(
    private val loginUseCase: LoginUseCase,
) : BaseViewModel<AuthState, AuthAction, AuthEffect>(AuthState()) {

    override fun onAction(action: AuthAction) = when (action) {
        is AuthAction.OnSubmit -> login()
    }

    private fun login() = viewModelScope.launch {
        loginUseCase(state.value.email, state.value.password).handle {
            success { navigateTo(TopLevelGraph.Dashboard) }          // ← direct call
            failure<AuthError.InvalidCredentials> {
                updateState { it.copy(emailError = S.err_invalid_credentials) }
            }
            catch { error ->
                updateState { it.copy(globalError = error.asString()) }
            }
        }
    }
}
```

### Feature Effect — UI-only events

```kotlin
// AuthEffect contains ONLY feature-specific UI events
sealed interface AuthEffect : UiEffect {
    data class ShowPasswordResetDialog(val email: String) : AuthEffect
    data object ShakePasswordField : AuthEffect
}
```

### Destinations (shared — single source of truth)

```kotlin
// core/navigation/Destinations.kt
sealed interface Destination

sealed interface AuthDestination : Destination {
    @Serializable data object Login : AuthDestination
    @Serializable data object Register : AuthDestination
    @Serializable data class PasswordReset(val email: String) : AuthDestination
}

sealed interface TopLevelGraph : Destination {
    @Serializable data object Dashboard : TopLevelGraph
    @Serializable data object Calendar : TopLevelGraph
    @Serializable data object Settings : TopLevelGraph
}

sealed interface DetailDestination : Destination {
    @Serializable data class CourseDetail(val courseId: String) : DetailDestination
}
```

### Android — single global observer

```kotlin
// composeApp — AppNavHost.kt
@Composable
fun AppNavHost(navController: NavHostController, globalEffectsHandler: GlobalUiEffectsHandler) {
    LaunchedEffect(Unit) {
        globalEffectsHandler.globalEffects.collect { effect ->
            when (effect) {
                is GlobalUiEffect.NavigateTo     -> navController.navigate(effect.destination)
                is GlobalUiEffect.NavigateBack   -> navController.navigateUp()
                is GlobalUiEffect.NavigateBackTo -> navController.popBackStack(
                    effect.destination, effect.inclusive
                )
                is GlobalUiEffect.Unauthorized   -> navController.navigate(AuthDestination.Login) {
                    popUpTo(0) { inclusive = true }
                }
                else -> { /* handled by other observers */ }
            }
        }
    }

    NavHost(navController, startDestination = AuthDestination.Login) {
        composable<AuthDestination.Login>     { LoginScreen() }
        composable<TopLevelGraph.Dashboard>   { DashboardScreen() }
        // …
    }
}
```

### iOS — single global observer (SKIE)

```swift
// iosApp — RootView.swift
.task {
    for await effect in globalEffectsHandler.globalEffects {
        switch effect {
        case let nav as GlobalUiEffect.NavigateTo:
            path.append(nav.destination)
        case is GlobalUiEffect.NavigateBack:
            if !path.isEmpty { path.removeLast() }
        case is GlobalUiEffect.Unauthorized:
            path = NavigationPath()
            currentRoot = .auth
        default: break
        }
    }
}
```

---

## Deep Links

`@Serializable` on `Destination` members enables deep-link URL generation on Android:

```kotlin
composable<DetailDestination.CourseDetail>(
    deepLinks = listOf(navDeepLink<DetailDestination.CourseDetail>(basePath = "app://course"))
)
```

On iOS, deep links are handled via `onOpenURL` and mapped to the same `Destination` types manually.

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Navigation in feature Effects | Scatters navigation logic; two observers per screen |
| Injected Navigator interface | Koin scoping complexity per lifecycle |
| Decompose | Couples navigation to shared VM lifecycle |
| Voyager | Android-centric; fragile iOS SKIE binding |
