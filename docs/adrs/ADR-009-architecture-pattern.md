# ADR-009: Architecture Pattern — Feature-First Clean Architecture + MVI

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The project needs a consistent architecture that:
- Scales to many features without becoming a "big ball of mud".
- Keeps platform-specific code (Compose, SwiftUI) entirely in the native layer.
- Makes business logic independently testable (no Android/iOS framework in domain).
- Provides unidirectional data flow to simplify UI state reasoning.
- Works naturally with Kotlin Coroutines and Flow.

---

## Decision Drivers

1. Clear separation of concerns — each layer has one responsibility.
2. Feature isolation — adding or removing a feature should not affect other features.
3. Testability — domain and presentation logic must be testable in `commonTest`.
4. Unidirectional data flow — eliminates ambiguous UI state.
5. Consistency — one pattern applied everywhere, no feature-specific exceptions.

---

## Considered Options

### Option A: MVVM without layering

ViewModels talk directly to APIs and databases. Simple for small projects.

**Cons:** Features bleed into each other; impossible to test without platform setup; no offline-first support.

---

### Option B: Clean Architecture with MVVM (layered per feature)

Data / Domain / Presentation layers per feature, with MVVM (LiveData or StateFlow).

**Pros:** Well-understood; separates concerns.

**Cons:** Multiple ViewModels updating State independently creates race conditions; no formal Action model; UI can trigger state mutations from multiple paths.

---

### Option C: Feature-First Clean Architecture + MVI — **CHOSEN**

Clean Architecture (Data / Domain / Presentation) applied per feature, with an MVI (Model-View-Intent) contract enforcing unidirectional data flow at the ViewModel boundary.

---

## Decision Outcome

**Use Feature-First Clean Architecture + MVI.**

### Layer responsibilities

| Layer | Responsibility | May import |
|:---|:---|:---|
| **Data** | Network, DB, mappers | Ktor, Room, DataStore |
| **Domain** | Business logic, use cases, repository interfaces | Only `kotlinx.*`, project models |
| **Presentation** | ViewModel, State, Action, Effect | Domain, `Res.string.*` (via `StringProvider`) |
| **Native UI** | Rendering, user events | Presentation layer only |

### MVI Contract

Every screen has three contracts in `presentation/contract/`:

```kotlin
// State — immutable snapshot; errors are plain resolved Strings
data class AuthState(
    val email: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
) : UiState

// Action — user intent
sealed interface AuthAction : UiAction {
    data class OnEmailChanged(val value: String) : AuthAction
    data object OnSubmit : AuthAction
}

// Effect — UI-level one-shot events ONLY (never navigation)
sealed interface AuthEffect : UiEffect {
    data class ShowResetDialog(val email: String) : AuthEffect
    data object ShakePasswordField : AuthEffect
}
```

### BaseViewModel contract

```kotlin
abstract class BaseViewModel<St : UiState, A : UiAction, E : UiEffect>(
    initialState: St,
) : ViewModel(), KoinComponent {

    // Injected by Koin — not a constructor parameter on every subclass
    private val stringProvider: StringProvider by inject()
    protected val S: StringProvider get() = stringProvider

    val state: StateFlow<St>

    // Feature-specific UI effects — buffered to prevent silent drops
    val effects: SharedFlow<E>         // extraBufferCapacity = 64

    // App-level effects: navigation, toast, snackbar, unauthorized
    val globalEffects: SharedFlow<GlobalUiEffect>   // extraBufferCapacity = 64

    abstract fun onAction(action: A)

    protected fun updateState(block: (St) -> St)
    protected fun emitEffect(effect: E)

    // Navigation — called directly, never via XEffect
    protected fun navigateTo(destination: Destination)
    protected fun navigateBack()
    protected fun navigateBackTo(destination: Destination, inclusive: Boolean = false)

    // Global feedback
    protected fun showToast(message: String)
    protected fun showSnackbar(message: String)
}
```

### Feature folder structure

```
features/{name}/
├── {Name}Module.kt            ← Koin
├── data/
│   ├── local/                 ← Room entities + DAOs
│   ├── remote/                ← DTOs + requests/responses
│   ├── mappers/               ← SyncMapper + EntityMapper
│   └── {Name}RepositoryImpl.kt
├── domain/
│   ├── models/
│   │   ├── {DomainModel}.kt
│   │   └── errors/{Name}Error.kt
│   ├── usecases/
│   ├── validation/
│   └── {Name}Repository.kt    ← Interface
└── presentation/
    ├── contract/
    │   ├── {Name}State.kt
    │   ├── {Name}Action.kt
    │   ├── {Name}Effect.kt
    │   └── {Name}SheetState.kt
    ├── mappers/               ← Error → String ONLY (via S)
    ├── fixtures/              ← Test data
    └── {Name}ViewModel.kt
```

### Mapper chain

```
Network DTO
    ↓  SyncMapper (data/mappers/)       Dto ↔ Entity
Room Entity
    ↓  EntityMapper (data/mappers/)     Entity ↔ Domain
Domain Model
    ↓  directly into State (no presentation model)
UiState
    ↓  errors only — UiMapper (presentation/mappers/)
    ↓  DomainError.asString(S) → String?
```

### No `utils/` or `helpers/` packages

Extension functions that would otherwise live in a utility class are placed in domain-specific files:
- `core/domain/extensions/DateTimeExtensions.kt`
- `core/domain/extensions/StringExtensions.kt`
- `features/auth/domain/extensions/AuthExtensions.kt`

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| MVVM without layers | Bleeds between features; untestable |
| Clean Architecture with plain MVVM | No formal Action model; bidirectional state mutations |
| Redux / TEA | Heavy boilerplate for KMP; iOS bridging complexity |
| MVI with a single app-wide store | Global state contention; feature isolation lost |
