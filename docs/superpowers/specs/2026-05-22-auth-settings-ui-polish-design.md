# Auth & Settings UI Polish — Design Spec

**Date:** 2026-05-22
**Branch:** feat/gpd9
**Status:** Approved

## Goal

Rework the Auth and Settings screens on both Android (Compose) and iOS (SwiftUI) to match the design language established in Planific. Extract reusable components into `core/ui/components/` (Android) and `Core/UI/Components/` (iOS), designed library-ready so they can later be extracted into a standalone KMP design system package shared across DevDash, Waystone, and future projects.

## Context

The current screens are functional but bare:
- **LoginScreen**: plain `OutlinedButton` stack, no branding, no layout structure
- **SettingsScreen Android**: flat `Column` with radio buttons, no icons, no sections
- **SettingsView iOS**: single segmented `Picker`, essentially empty

Planific's design patterns serve as the reference: `AppHeader`, `SettingsRow`, `OrDivider`, section headers, bottom sheets for selection, and full-width buttons with loading states.

## Components

### Android — `androidApp/src/main/kotlin/…/core/ui/components/`

#### `AppHeader.kt`
Branded header row used at the top of the login screen.
- `text: String` — app name, displayed uppercase with `FontWeight.Black` and `letterSpacing = 4.sp` in `MaterialTheme.colorScheme.primary`
- `modifier: Modifier = Modifier`
- Previews: light, dark

#### `PrimaryButton.kt`
Full-width filled button with inline loading state.
- `text: String`
- `onClick: () -> Unit`
- `isLoading: Boolean = false` — replaces label with `CircularProgressIndicator` when true
- `enabled: Boolean = true`
- `modifier: Modifier = Modifier`
- Height fixed at 52dp for touch comfort
- Previews: idle, loading, disabled, dark

#### `OrDivider.kt`
Horizontal divider with centred "or" label. Used between auth sections.
- `modifier: Modifier = Modifier`
- Implemented as a `Row` with two `HorizontalDivider`s flanking a `Text("or")`
- Previews: light, dark

#### `SettingsRow.kt`
Standard list row for settings screens. Supports icons, subtitles, chevrons, and arbitrary trailing content.
- `title: String`
- `icon: ImageVector? = null`
- `iconTint: Color = MaterialTheme.colorScheme.primary`
- `subtitle: String? = null`
- `showChevron: Boolean = false`
- `onClick: (() -> Unit)? = null`
- `trailing: @Composable (() -> Unit)? = null`
- Previews: icon + chevron, icon + subtitle, icon + switch trailing, no icon, dark

#### `SectionHeader.kt`
Small uppercase label used above settings sections.
- `title: String`
- `modifier: Modifier = Modifier`
- Style: `MaterialTheme.typography.labelSmall`, color `onSurfaceVariant`, padding `horizontal = 16.dp, vertical = 8.dp`
- Previews: light, dark

#### `SelectableRow.kt`
Radio-style row used inside bottom sheets for single-select lists (e.g., theme picker).
- `label: String`
- `isSelected: Boolean`
- `onClick: () -> Unit`
- Implemented as a clickable `Row` with a `RadioButton` leading
- Previews: selected, unselected, dark

---

### iOS — `iosApp/iosApp/Core/UI/Components/`

`SectionHeader` is omitted — SwiftUI `List` + `Section(header:)` handles this natively.

#### `AppHeader.swift`
- `text: String` — displayed with `.system(.title2, weight: .black)`, `.tracking(4)`, `accentColor`
- Previews: light, dark (via `#Preview` with `colorScheme` environment)

#### `PrimaryButton.swift`
- `title: String`
- `isLoading: Bool = false`
- `action: () -> Void`
- `disabled: Bool = false`
- Full-width `.frame(maxWidth: .infinity)`, height 52, `.buttonStyle(.borderedProminent)`
- Shows `ProgressView` in place of title when loading
- Previews: idle, loading, disabled, dark

#### `OrDivider.swift`
- `HStack` with `Divider`s flanking `Text("or")`
- Previews: light, dark

#### `SettingsRow.swift`
Port of Planific's component (already well-designed there).
- `icon: String? = nil` — SF Symbol name
- `iconTint: Color = .accentColor`
- `title: String`
- `subtitle: String? = nil`
- `showChevron: Bool = false`
- `action: (() -> Void)? = nil`
- `trailing: some View` via generic + convenience `EmptyView` initialiser
- Previews: icon + chevron, subtitle, toggle trailing, no icon

#### `SelectableRow.swift`
Checkmark-style row for single-select sheets (iOS convention).
- `label: String`
- `isSelected: Bool`
- `action: () -> Void`
- Trailing `Image(systemName: "checkmark")` shown when selected, tinted `accentColor`
- Previews: selected, unselected, dark

---

## Screen Changes

### LoginScreen — Android

**File:** `androidApp/…/auth/LoginScreen.kt`

- Wrap body in `Scaffold` → inner `Column` with `verticalScroll`
- Layout: `AppHeader(text = "YourApp")` → `Spacer(48dp)` → title `Text("Sign in", titleLarge)` → `Spacer(32dp)` → OAuth buttons → error
- OAuth buttons: keep as `OutlinedButton` (all 4 are equal-weight — no primary CTA exists) but height 52dp, `fillMaxWidth`, with `Spacer(12dp)` between
- Error: wrap in `AnimatedVisibility` instead of bare `let`
- Loading: centred `CircularProgressIndicator(48dp)` (unchanged)

### SettingsScreen — Android

**File:** `androidApp/…/settings/SettingsScreen.kt`

- Replace `Column` with `LazyColumn`
- Section structure:
  ```
  SectionHeader("Appearance")
  SettingsRow("Theme", Icons.Default.DarkMode, subtitle = themeMode.label, showChevron = true, onClick = { showThemeSheet = true })
  SettingsRow("Dynamic Colors", Icons.Default.Palette, subtitle = "Use wallpaper colors (Android 12+)", trailing = { Switch(…) })   // Android 12+ guard unchanged
  HorizontalDivider(padding vertical = 8dp)
  ```
- Theme sheet: `ModalBottomSheet` driven by local `var showThemeSheet by remember { mutableStateOf(false) }` — sheet state is UI-only, does not touch shared VM
- Sheet content: `SelectableRow` per `ThemeMode` entry

### LoginView — iOS

**File:** `iosApp/…/auth/LoginView.swift`

- Wrap in `ScrollView` → inner `VStack(spacing: 24)`
- Layout: `AppHeader(text: "YourApp")` → `Spacer(48)` → title `Text("Sign in").font(.title).bold()` → OAuth buttons → error
- OAuth buttons: `Button` with `.bordered` style, `frame(maxWidth: .infinity)`, `controlSize(.large)`, `Spacer(8)` between
- Error: `if let error` with `.foregroundStyle(.red).font(.caption)` (unchanged, no animation needed on iOS — system handles it)

### SettingsView — iOS

**File:** `iosApp/…/Features/Settings/SettingsView.swift`

- Keep `Form` + `Section` structure (native iOS, already correct)
- Replace `Picker(.segmented)` with:
  ```swift
  SettingsRow(icon: "moon.fill", title: "Theme", subtitle: currentMode, showChevron: true, action: { showThemeSheet = true })
  ```
- Sheet driven by local `@State var showThemeSheet = false`, `.sheet(isPresented:)` containing `SelectableRow` per `ThemeMode`
- Dynamic Colors row: Android-only concept, nothing added on iOS

---

## File Structure After Changes

```
androidApp/src/main/kotlin/…/core/ui/components/
  AppHeader.kt          (new)
  PrimaryButton.kt      (new)
  OrDivider.kt          (new)
  SettingsRow.kt        (new)
  SectionHeader.kt      (new)
  SelectableRow.kt      (new)

androidApp/…/auth/LoginScreen.kt       (modified)
androidApp/…/settings/SettingsScreen.kt (modified)

iosApp/iosApp/Core/UI/Components/
  AppHeader.swift       (new)
  PrimaryButton.swift   (new)
  OrDivider.swift       (new)
  SettingsRow.swift     (new)
  SelectableRow.swift   (new)

iosApp/…/auth/LoginView.swift          (modified)
iosApp/…/Features/Settings/SettingsView.swift (modified)
```

## Library-Readiness Notes

These components are designed for future extraction into a standalone `sd-components` KMP library:
- No app-specific imports (no ViewModel references, no navigation, no DI)
- All parameters are primitives or Material/SwiftUI types
- No hardcoded strings — callers pass all text
- Previews cover all meaningful states so the library can be validated in isolation

## Out of Scope

- Provider icons on OAuth buttons (requires bundling SVG assets per provider)
- `SettingsProfileHeader` (avatar + name row) — no user profile concept in template yet
- `BottomSheet` wrapper component — `ModalBottomSheet` / `.sheet` used directly
- Library publishing infrastructure — separate session
