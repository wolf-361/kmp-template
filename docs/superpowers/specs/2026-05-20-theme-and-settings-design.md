# Theme System & Settings Feature Design

**Date:** 2026-05-20
**Branch:** feat/gpd9

---

## Goal

Add a proper theme system (light / dark / system + opt-in Dynamic Color on Android) to the template, persisted via DataStore and surfaced through a `SettingsViewModel` that both Android and iOS consume. Fix the Android `LoginScreen` preview gap at the same time.

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
    val useDynamicColor: Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setUseDynamicColor(enabled: Boolean)
}
```

**`settings/data/repository/SettingsRepositoryImpl.kt`**
DataStore-backed implementation. Keys: `stringPreferencesKey("theme_mode")` and `booleanPreferencesKey("use_dynamic_color")`. `ThemeMode` defaults to `SYSTEM`; `useDynamicColor` defaults to `false` (opt-in).

```kotlin
@Single
class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("use_dynamic_color")

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs -> ThemeMode.fromKey(prefs[themeModeKey]) }

    override val useDynamicColor: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[dynamicColorKey] ?: false }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[themeModeKey] = mode.name }
    }

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[dynamicColorKey] = enabled }
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

**`settings/domain/usecase/GetUseDynamicColorUseCase.kt`**
```kotlin
class GetUseDynamicColorUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<Boolean> = repository.useDynamicColor
}
```

**`settings/domain/usecase/SetUseDynamicColorUseCase.kt`**
```kotlin
open class SetUseDynamicColorUseCase(private val repository: SettingsRepository) {
    open suspend operator fun invoke(enabled: Boolean) = repository.setUseDynamicColor(enabled)
}
```

All use cases marked `open` for Mokkery mocking in tests (consistent with auth use cases).

**`settings/presentation/SettingsState.kt`**
```kotlin
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
)
```

**`settings/presentation/SettingsAction.kt`**
```kotlin
sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
    data class SetUseDynamicColor(val enabled: Boolean) : SettingsAction
}
```

**`settings/presentation/SettingsViewModel.kt`**
```kotlin
@Factory
class SettingsViewModel(
    private val getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val getUseDynamicColor: GetUseDynamicColorUseCase,
    private val setUseDynamicColor: SetUseDynamicColorUseCase,
) : BaseViewModel<SettingsState, SettingsAction, Nothing>(SettingsState()) {

    init {
        viewModelScope.launch {
            getThemeMode().collect { setState { copy(themeMode = it) } }
        }
        viewModelScope.launch {
            getUseDynamicColor().collect { setState { copy(useDynamicColor = it) } }
        }
    }

    override fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode ->
                viewModelScope.launch { setThemeMode(action.mode) }
            is SettingsAction.SetUseDynamicColor ->
                viewModelScope.launch { setUseDynamicColor(action.enabled) }
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
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (useDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        useDark -> darkScheme
        else    -> lightScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
```
Dynamic Color (Material You) is opt-in: only applied when `useDynamicColor = true` **and** the device is API 31+. Static `lightScheme`/`darkScheme` otherwise. The toggle is hidden in `SettingsScreen` on devices below API 31.

**`settings/SettingsRoute.kt`**
Koin-aware entry point: `koinViewModel<SettingsViewModel>()`, collects state, delegates to `SettingsScreen`.

**`settings/SettingsScreen.kt`**
Pure composable — takes `SettingsState` and `(SettingsAction) -> Unit`. Shows:
1. A `ThemeMode` segmented/radio picker (System / Light / Dark)
2. A "Use Dynamic Colors" `Switch` toggle — only rendered when `Build.VERSION.SDK_INT >= S`, hidden otherwise

Includes `@Preview` for each mode. Clearly marked with a comment that this is the stub to extend.

### Modified files

**`MainActivity.kt`**
- Add `enableEdgeToEdge()`
- Get `SettingsViewModel` via `koinViewModel()`
- Collect `state` from it with `collectAsState()`
- Pass into `AppTheme(themeMode = state.themeMode, useDynamicColor = state.useDynamicColor) { AppNavHost(...) }`

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
  ├─ SettingsRepositoryImpl.themeMode: Flow<ThemeMode>
  │    └─ GetThemeModeUseCase()  ─┐
  └─ SettingsRepositoryImpl.useDynamicColor: Flow<Boolean>
       └─ GetUseDynamicColorUseCase()  ─┤
                                        └─ SettingsViewModel.state: StateFlow<SettingsState>
                                             ├─ Android: MainActivity → AppTheme(themeMode, useDynamicColor)
                                             └─ iOS: SettingsViewModelHolder → RootView.preferredColorScheme
                                                      (useDynamicColor ignored on iOS)
```

On write:
```
User taps theme option   → SettingsAction.SetThemeMode(mode)      → SetThemeModeUseCase
User toggles dyn. color  → SettingsAction.SetUseDynamicColor(bool) → SetUseDynamicColorUseCase
  → SettingsRepositoryImpl → DataStore.edit → Flow emits → all collectors update
```

---

## What Is NOT Included

- Accent color / predefined color palettes — product decision, not template concern
- Contrast variants (medium/high) — add per-project if needed
- `materialkolor` dependency — not worth the weight for a template default
- iOS custom color assets — relying on system semantics + SwiftUI accent color is correct for a starting point

---

## Testing

- `GetThemeModeUseCaseTest` — delegates to repository
- `SetThemeModeUseCaseTest` — delegates to repository
- `GetUseDynamicColorUseCaseTest` — delegates to repository
- `SetUseDynamicColorUseCaseTest` — delegates to repository
- `SettingsViewModelTest` — uses `BaseViewModelTest`, mocks all four use cases with Mokkery
- `SettingsScreen` `@Preview` — light / dark / system variants visible in Android Studio

No iOS UI tests — out of scope for the template.
