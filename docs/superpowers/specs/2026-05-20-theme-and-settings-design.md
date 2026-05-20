# Theme System & Settings Feature Design

**Date:** 2026-05-20
**Branch:** feat/gpd9

---

## Goal

Add a proper theme system (light / dark / system) to the template, persisted via DataStore and surfaced through a `SettingsViewModel` that both Android and iOS consume. Fix the Android `LoginScreen` preview gap at the same time.

## Architecture

Full repository + use case stack, consistent with the auth feature. `SettingsViewModel` is the single source of truth for theme state on both platforms. No global singletons.

## Tech Stack

- Kotlin Multiplatform — shared domain, data, presentation
- DataStore Preferences — persistence layer (already in stack)
- Koin KSP — DI (already in stack)
- Jetpack Compose + Material3 — Android UI, Dynamic Color on API 31+
- SwiftUI — iOS UI, `.preferredColorScheme()`

---

## Section 1 — Shared (KMP)

### New files

**`settings/domain/model/ThemeMode.kt`**
```kotlin
enum class ThemeMode { SYSTEM, LIGHT, DARK }
```
Lives under `settings/domain/model/`, not `core/` — it is a settings concern, not a system-wide primitive.

**`settings/domain/repository/SettingsRepository.kt`**
```kotlin
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

**`settings/data/repository/SettingsRepositoryImpl.kt`**
DataStore-backed implementation. Key: `stringPreferencesKey("theme_mode")`, value: enum name string. Defaults to `ThemeMode.SYSTEM` when no value is stored.

```kotlin
@Single
class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    private val key = stringPreferencesKey("theme_mode")

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs -> ThemeMode.fromKey(prefs[key]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[key] = mode.name }
    }
}
```

Add `companion object { fun fromKey(key: String?): ThemeMode = entries.find { it.name == key } ?: SYSTEM }` to `ThemeMode`.

**`settings/domain/usecase/GetThemeModeUseCase.kt`**
```kotlin
class GetThemeModeUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}
```

**`settings/domain/usecase/SetThemeModeUseCase.kt`**
```kotlin
open class SetThemeModeUseCase(private val repository: SettingsRepository) {
    open suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
```
Marked `open` for Mokkery mocking in tests (consistent with auth use cases).

**`settings/presentation/SettingsState.kt`**
```kotlin
data class SettingsState(val themeMode: ThemeMode = ThemeMode.SYSTEM)
```

**`settings/presentation/SettingsAction.kt`**
```kotlin
sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
}
```

**`settings/presentation/SettingsViewModel.kt`**
```kotlin
@Factory
class SettingsViewModel(
    private val getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : BaseViewModel<SettingsState, SettingsAction, Nothing>(SettingsState()) {

    init {
        viewModelScope.launch {
            getThemeMode().collect { setState { copy(themeMode = it) } }
        }
    }

    override fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> viewModelScope.launch { setThemeMode(action.mode) }
        }
    }
}
```

**`di/SettingsModule.kt`**
```kotlin
@Module
@ComponentScan("com.yourcompany.kmptemplate.settings")
class SettingsModule {
    @Single
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        SettingsRepositoryImpl(dataStore)
}
```
`SettingsRepositoryImpl` is wired manually because it has a constructor arg (`DataStore`) that Koin needs to resolve. The use cases and `SettingsViewModel` are annotated `@Factory` and picked up by `@ComponentScan`.

### Modified files

**`KoinHelper.kt`** — add `getSettingsViewModel()` accessor alongside existing helpers.

---

## Section 2 — Android

### New files

**`core/ui/theme/Color.kt`**
Full Material3 light and dark `ColorScheme` token definitions generated from a neutral blue seed (`#4A6FA5`). Devs replace this with their brand color — the seed is intentionally generic. Tokens cover all 30 M3 slots (primary, secondary, tertiary, surface family, error family).

**`core/ui/theme/Type.kt`**
`AppTypography` using Material3 baseline type scale. No custom fonts — devs add their own. All entries explicitly defined so the file is self-documenting.

**`core/ui/theme/Theme.kt`**
```kotlin
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (useDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        useDark -> darkScheme
        else    -> lightScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
```
Dynamic Color (Material You) on API 31+; static `lightScheme`/`darkScheme` fallback on older devices.

**`settings/SettingsRoute.kt`**
Koin-aware entry point: `koinViewModel<SettingsViewModel>()`, collects state, delegates to `SettingsScreen`.

**`settings/SettingsScreen.kt`**
Pure composable — takes `SettingsState` and `(SettingsAction) -> Unit`. Shows a `ThemeMode` segmented/radio picker (System / Light / Dark). Includes `@Preview` for each mode. Clearly marked with a comment that this is the stub to extend.

### Modified files

**`MainActivity.kt`**
- Add `enableEdgeToEdge()`
- Get `SettingsViewModel` via `koinViewModel()`
- Collect `themeMode` from its state with `collectAsState()`
- Pass into `AppTheme(themeMode = themeMode) { AppNavHost(...) }`

**`AppNavHost.kt`**
- Replace `PlaceholderScreen("Settings")` with `SettingsRoute()`

**`auth/LoginScreen.kt`** → split into two files:
- `auth/LoginRoute.kt` — `koinViewModel<AuthViewModel>()`, collects state, calls `LoginScreen`
- `auth/LoginScreen.kt` — pure composable `LoginScreen(state, onAction)` + `@Preview`

**`AppNavHost.kt`**
- Replace `LoginScreen()` with `LoginRoute()`

---

## Section 3 — iOS

### New files

**`Core/UI/Theme/ThemeMode+ColorScheme.swift`**
```swift
import Shared
import SwiftUI

extension Shared.ThemeMode {
    var preferredColorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light:  .light
        case .dark:   .dark
        default:      nil
        }
    }
}
```

**`Features/Settings/SettingsViewModelHolder.swift`**
`@MainActor final class` conforming to `ObservableObject`. Holds `SettingsViewModel` from Koin, publishes `SettingsState`. Follows the same `AuthViewModelHolder` pattern already in the app.

**`Features/Settings/SettingsView.swift`**
SwiftUI `View` with a `Picker` for SYSTEM/LIGHT/DARK bound to `settingsHolder.state.themeMode`. Dispatches `SettingsAction.SetThemeMode` on change. Stub `Section`s for future settings rows are present but empty, with a `// TODO: add settings here` comment.

### Modified files

**`RootView.swift`**
- Add `@StateObject private var settingsHolder = SettingsViewModelHolder()`
- Apply `.preferredColorScheme(settingsHolder.state.themeMode.preferredColorScheme)` on the root `ZStack`/`Group`
- `DashboardNavStack` gets a `.toolbar { Button("Settings") { showSettings = true } }` that presents `SettingsView` as a `.sheet`. Settings is not a `TopLevelGraph` destination on iOS — it is always a modal sheet, consistent with iOS convention.

---

## Data Flow

```
DataStore
  └─ SettingsRepositoryImpl.themeMode: Flow<ThemeMode>
       └─ GetThemeModeUseCase()
            └─ SettingsViewModel.state: StateFlow<SettingsState>
                 ├─ Android: MainActivity collects themeMode → AppTheme(themeMode)
                 └─ iOS: SettingsViewModelHolder publishes state → RootView.preferredColorScheme
```

On write:
```
User taps theme option
  → SettingsAction.SetThemeMode(mode)
  → SettingsViewModel.onAction
  → SetThemeModeUseCase(mode)
  → SettingsRepositoryImpl.setThemeMode
  → DataStore.edit
  → Flow emits new value → all collectors update
```

---

## What Is NOT Included

- Accent color / predefined color palettes — product decision, not template concern
- Contrast variants (medium/high) — add per-project if needed
- `materialkolor` dependency — not worth the weight for a template default
- iOS custom color assets — relying on system semantics + SwiftUI accent color is correct for a starting point

---

## Testing

- `SetThemeModeUseCaseTest` — delegates to repository (follows existing use case test pattern)
- `GetThemeModeUseCaseTest` — delegates to repository
- `SettingsViewModelTest` — uses `BaseViewModelTest`, fakes repository with Mokkery mock
- `SettingsScreen` `@Preview` — light / dark / system variants visible in Android Studio

No iOS UI tests — out of scope for the template.
