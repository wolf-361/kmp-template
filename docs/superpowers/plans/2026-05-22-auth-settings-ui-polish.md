# Auth & Settings UI Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add six library-ready UI components (AppHeader, PrimaryButton, OrDivider, SettingsRow, SectionHeader, SelectableRow) on both platforms, then apply them to upgrade LoginScreen and SettingsScreen.

**Architecture:** Components live in `androidApp/.../core/ui/components/` and `iosApp/.../Core/UI/Components/`, with no app-specific imports. Sheet/modal state is managed locally in each screen — the shared ViewModels are not touched. Route files are not changed (LoginScreen/SettingsScreen signatures stay the same).

**Tech Stack:** Jetpack Compose + Material3 via CMP (Android), SwiftUI (iOS). Kotlin shared module for ThemeMode enum and SettingsAction/AuthAction types.

---

## File Map

**New — Android**
- `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/AppHeader.kt`
- `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/PrimaryButton.kt`
- `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/OrDivider.kt`
- `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/SettingsRow.kt`
- `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/SectionHeader.kt`
- `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/SelectableRow.kt`

**Modified — Android**
- `gradle/libs.versions.toml` — add material-icons-extended entry
- `androidApp/build.gradle.kts` — add material-icons-extended implementation
- `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/auth/LoginScreen.kt`
- `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/settings/SettingsScreen.kt`

**New — iOS**
- `iosApp/iosApp/Core/UI/Components/AppHeader.swift`
- `iosApp/iosApp/Core/UI/Components/PrimaryButton.swift`
- `iosApp/iosApp/Core/UI/Components/OrDivider.swift`
- `iosApp/iosApp/Core/UI/Components/SettingsRow.swift`
- `iosApp/iosApp/Core/UI/Components/SelectableRow.swift`
- `iosApp/iosApp/Core/UI/Theme/ThemeMode+UI.swift`

**Modified — iOS**
- `iosApp/iosApp/auth/LoginView.swift`
- `iosApp/iosApp/Features/Settings/SettingsView.swift`

---

## Task 1: Add material-icons-extended (Android)

The component icon set (DarkMode, Palette, etc.) lives in the extended icons artifact, which is not yet a dependency.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `androidApp/build.gradle.kts`

- [ ] **Step 1: Add library entry to version catalog**

In `gradle/libs.versions.toml`, under `[libraries]`, add:
```toml
material-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version.ref = "compose-multiplatform" }
```

- [ ] **Step 2: Add implementation to androidApp**

In `androidApp/build.gradle.kts`, inside the `dependencies { }` block, add after the other compose lines:
```kotlin
implementation(libs.material.icons.extended)
```

- [ ] **Step 3: Sync and verify**

Run:
```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**
```bash
git add gradle/libs.versions.toml androidApp/build.gradle.kts
git commit -m "build(android): add material-icons-extended dependency"
```

---

## Task 2: AppHeader.kt (Android)

Branded header row. Displays the app name in uppercase with heavy weight and primary color. No business logic — caller provides the string.

**Files:**
- Create: `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/AppHeader.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.yourcompany.kmptemplate.core.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

@Composable
fun AppHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppHeaderLightPreview() {
    AppTheme(ThemeMode.LIGHT) { AppHeader(text = "YourApp") }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun AppHeaderDarkPreview() {
    AppTheme(ThemeMode.DARK) { AppHeader(text = "YourApp") }
}
```

- [ ] **Step 2: Compile**
```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/AppHeader.kt
git commit -m "feat(android): add AppHeader component"
```

---

## Task 3: PrimaryButton.kt (Android)

Full-width filled button with an inline loading spinner. Used for primary submit actions (not the OAuth buttons, which are all equal-weight).

**Files:**
- Create: `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/PrimaryButton.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.yourcompany.kmptemplate.core.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonIdlePreview() {
    AppTheme(ThemeMode.LIGHT) { PrimaryButton(text = "Continue", onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonLoadingPreview() {
    AppTheme(ThemeMode.LIGHT) { PrimaryButton(text = "Continue", onClick = {}, isLoading = true) }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonDisabledPreview() {
    AppTheme(ThemeMode.LIGHT) { PrimaryButton(text = "Continue", onClick = {}, enabled = false) }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PrimaryButtonDarkPreview() {
    AppTheme(ThemeMode.DARK) { PrimaryButton(text = "Continue", onClick = {}) }
}
```

- [ ] **Step 2: Compile**
```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/PrimaryButton.kt
git commit -m "feat(android): add PrimaryButton component"
```

---

## Task 4: OrDivider.kt (Android)

Horizontal "or" separator used between auth sections (e.g., between OAuth and email/password). Built as a future utility — not wired to the auth screen yet since auth is currently OAuth-only.

**Files:**
- Create: `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/OrDivider.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.yourcompany.kmptemplate.core.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "or",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
private fun OrDividerLightPreview() {
    AppTheme(ThemeMode.LIGHT) { OrDivider(modifier = Modifier.padding(16.dp)) }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun OrDividerDarkPreview() {
    AppTheme(ThemeMode.DARK) { OrDivider(modifier = Modifier.padding(16.dp)) }
}
```

- [ ] **Step 2: Compile**
```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/OrDivider.kt
git commit -m "feat(android): add OrDivider component"
```

---

## Task 5: SettingsRow.kt (Android)

Standard list row for settings screens. Supports an optional leading icon, title+subtitle, chevron, and an arbitrary trailing slot (switch, text, etc.).

**Files:**
- Create: `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/SettingsRow.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.yourcompany.kmptemplate.core.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val rowModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            trailing != null -> trailing()
            showChevron -> Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 72)
@Composable
private fun SettingsRowIconChevronPreview() {
    AppTheme(ThemeMode.LIGHT) {
        SettingsRow(title = "Theme", icon = Icons.Default.DarkMode, showChevron = true, onClick = {})
    }
}

@Preview(showBackground = true, heightDp = 88)
@Composable
private fun SettingsRowWithSubtitlePreview() {
    AppTheme(ThemeMode.LIGHT) {
        SettingsRow(
            title = "Theme",
            subtitle = "System",
            icon = Icons.Default.DarkMode,
            showChevron = true,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 72)
@Composable
private fun SettingsRowWithSwitchPreview() {
    AppTheme(ThemeMode.LIGHT) {
        SettingsRow(
            title = "Notifications",
            icon = Icons.Default.Notifications,
            trailing = { Switch(checked = true, onCheckedChange = {}) },
        )
    }
}

@Preview(showBackground = true, heightDp = 56)
@Composable
private fun SettingsRowNoIconPreview() {
    AppTheme(ThemeMode.LIGHT) {
        SettingsRow(title = "Sign out", onClick = {})
    }
}

@Preview(showBackground = true, heightDp = 88, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SettingsRowDarkPreview() {
    AppTheme(ThemeMode.DARK) {
        SettingsRow(
            title = "Theme",
            subtitle = "Dark",
            icon = Icons.Default.DarkMode,
            showChevron = true,
            onClick = {},
        )
    }
}
```

- [ ] **Step 2: Compile**
```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/SettingsRow.kt
git commit -m "feat(android): add SettingsRow component"
```

---

## Task 6: SectionHeader.kt (Android)

Small label used above settings sections. Matches Material3 convention for section labels in lists.

**Files:**
- Create: `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/SectionHeader.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.yourcompany.kmptemplate.core.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderLightPreview() {
    AppTheme(ThemeMode.LIGHT) { SectionHeader(title = "Appearance") }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SectionHeaderDarkPreview() {
    AppTheme(ThemeMode.DARK) { SectionHeader(title = "Appearance") }
}
```

- [ ] **Step 2: Compile**
```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/SectionHeader.kt
git commit -m "feat(android): add SectionHeader component"
```

---

## Task 7: SelectableRow.kt (Android)

Radio-style row for single-select lists inside bottom sheets. Each option in a theme/language picker maps to one `SelectableRow`.

**Files:**
- Create: `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/SelectableRow.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.yourcompany.kmptemplate.core.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

@Composable
fun SelectableRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true, heightDp = 56)
@Composable
private fun SelectableRowSelectedPreview() {
    AppTheme(ThemeMode.LIGHT) { SelectableRow(label = "System", isSelected = true, onClick = {}) }
}

@Preview(showBackground = true, heightDp = 56)
@Composable
private fun SelectableRowUnselectedPreview() {
    AppTheme(ThemeMode.LIGHT) { SelectableRow(label = "Dark", isSelected = false, onClick = {}) }
}

@Preview(showBackground = true, heightDp = 168, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SelectableRowGroupDarkPreview() {
    AppTheme(ThemeMode.DARK) {
        androidx.compose.foundation.layout.Column {
            SelectableRow(label = "System", isSelected = true, onClick = {})
            SelectableRow(label = "Light", isSelected = false, onClick = {})
            SelectableRow(label = "Dark", isSelected = false, onClick = {})
        }
    }
}
```

- [ ] **Step 2: Compile**
```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add androidApp/src/main/kotlin/com/yourcompany/kmptemplate/core/ui/components/SelectableRow.kt
git commit -m "feat(android): add SelectableRow component"
```

---

## Task 8: Update LoginScreen.kt (Android)

Apply `AppHeader`, wrap in `Scaffold` + `verticalScroll`, upgrade to `titleLarge` title, size the OAuth buttons to 52dp, and use `AnimatedVisibility` for the error.

**Files:**
- Modify: `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/auth/LoginScreen.kt`

- [ ] **Step 1: Replace the file content**

```kotlin
package com.yourcompany.kmptemplate.auth

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.auth.domain.model.OAuthProvider
import com.yourcompany.kmptemplate.auth.presentation.AuthAction
import com.yourcompany.kmptemplate.auth.presentation.AuthState
import com.yourcompany.kmptemplate.core.ui.components.AppHeader
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode

@Composable
fun LoginScreen(
    state: AuthState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppHeader(text = "YourApp")
            Spacer(Modifier.height(48.dp))
            Text(text = "Sign in", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(32.dp))

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else {
                OAuthButton("Continue with Google", OAuthProvider.GOOGLE, onAction)
                Spacer(Modifier.height(12.dp))
                OAuthButton("Continue with Apple", OAuthProvider.APPLE, onAction)
                Spacer(Modifier.height(12.dp))
                OAuthButton("Continue with Microsoft", OAuthProvider.MICROSOFT, onAction)
                Spacer(Modifier.height(12.dp))
                OAuthButton("Continue with GitHub", OAuthProvider.GITHUB, onAction)
            }

            AnimatedVisibility(visible = state.error != null) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun OAuthButton(label: String, provider: OAuthProvider, onAction: (AuthAction) -> Unit) {
    OutlinedButton(
        onClick = { onAction(AuthAction.LoginWith(provider)) },
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(label)
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenLightPreview() {
    AppTheme(ThemeMode.LIGHT) { LoginScreen(state = AuthState(), onAction = {}) }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenDarkPreview() {
    AppTheme(ThemeMode.DARK) { LoginScreen(state = AuthState(), onAction = {}) }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenLoadingPreview() {
    AppTheme(ThemeMode.LIGHT) { LoginScreen(state = AuthState(isLoading = true), onAction = {}) }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenErrorPreview() {
    AppTheme(ThemeMode.LIGHT) {
        LoginScreen(state = AuthState(error = "Authentication failed. Please try again."), onAction = {})
    }
}
```

- [ ] **Step 2: Compile**
```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add androidApp/src/main/kotlin/com/yourcompany/kmptemplate/auth/LoginScreen.kt
git commit -m "feat(android): restyle LoginScreen with AppHeader and Scaffold"
```

---

## Task 9: Update SettingsScreen.kt (Android)

Replace the flat `Column` with a `LazyColumn`, section headers, `SettingsRow` components, and a `ModalBottomSheet` for theme selection. Sheet state is local — the shared ViewModel is not touched.

**Files:**
- Modify: `androidApp/src/main/kotlin/com/yourcompany/kmptemplate/settings/SettingsScreen.kt`

- [ ] **Step 1: Replace the file content**

```kotlin
package com.yourcompany.kmptemplate.settings

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.core.ui.components.SectionHeader
import com.yourcompany.kmptemplate.core.ui.components.SelectableRow
import com.yourcompany.kmptemplate.core.ui.components.SettingsRow
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode
import com.yourcompany.kmptemplate.settings.presentation.SettingsAction
import com.yourcompany.kmptemplate.settings.presentation.SettingsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showThemeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { SectionHeader(title = "Appearance") }

        item {
            SettingsRow(
                title = "Theme",
                icon = Icons.Default.DarkMode,
                subtitle = state.themeMode.label,
                showChevron = true,
                onClick = { showThemeSheet = true },
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                SettingsRow(
                    title = "Dynamic Colors",
                    icon = Icons.Default.Palette,
                    subtitle = "Use wallpaper colors (Android 12+)",
                    trailing = {
                        Switch(
                            checked = state.useDynamicColor,
                            onCheckedChange = { onAction(SettingsAction.SetUseDynamicColor(it)) },
                        )
                    },
                )
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
    }

    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            sheetState = sheetState,
        ) {
            ThemeMode.entries.forEach { mode ->
                SelectableRow(
                    label = mode.label,
                    isSelected = state.themeMode == mode,
                    onClick = {
                        onAction(SettingsAction.SetThemeMode(mode))
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showThemeSheet = false }
                    },
                )
            }
        }
    }
}

private val ThemeMode.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

@Preview(showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    AppTheme(ThemeMode.LIGHT) { SettingsScreen(state = SettingsState(), onAction = {}) }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenDarkPreview() {
    AppTheme(ThemeMode.DARK) {
        SettingsScreen(state = SettingsState(themeMode = ThemeMode.DARK), onAction = {})
    }
}
```

- [ ] **Step 2: Compile**
```bash
./gradlew :androidApp:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**
```bash
git add androidApp/src/main/kotlin/com/yourcompany/kmptemplate/settings/SettingsScreen.kt
git commit -m "feat(android): restyle SettingsScreen with SettingsRow, SectionHeader, and bottom sheet"
```

---

## Task 10: AppHeader.swift (iOS)

**Files:**
- Create: `iosApp/iosApp/Core/UI/Components/AppHeader.swift`
- Add the new file to the Xcode project: in Xcode, right-click `Core/UI/Components` group → Add Files, or drag the file in.

- [ ] **Step 1: Create `Core/UI/Components/` directory if it doesn't exist**
```bash
mkdir -p /Users/wolf361/Git/personal/kmp-template/iosApp/iosApp/Core/UI/Components
```

- [ ] **Step 2: Create the file**

```swift
import SwiftUI

struct AppHeader: View {
    let text: String

    var body: some View {
        Text(text.uppercased())
            .font(.system(.title2, design: .default, weight: .black))
            .tracking(4)
            .foregroundStyle(Color.accentColor)
            .frame(maxWidth: .infinity, alignment: .center)
    }
}

#Preview("Light") {
    AppHeader(text: "YourApp")
        .padding()
}

#Preview("Dark") {
    AppHeader(text: "YourApp")
        .padding()
        .preferredColorScheme(.dark)
}
```

- [ ] **Step 3: Add to Xcode project and build (⌘B)**

Expected: 0 errors, 0 warnings related to this file.

- [ ] **Step 4: Commit**
```bash
git add iosApp/iosApp/Core/UI/Components/AppHeader.swift
git commit -m "feat(ios): add AppHeader component"
```

---

## Task 11: PrimaryButton.swift (iOS)

**Files:**
- Create: `iosApp/iosApp/Core/UI/Components/PrimaryButton.swift`

- [ ] **Step 1: Create the file**

```swift
import SwiftUI

struct PrimaryButton: View {
    let title: String
    var isLoading: Bool = false
    var disabled: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(.white)
                } else {
                    Text(title)
                        .fontWeight(.semibold)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
        }
        .buttonStyle(.borderedProminent)
        .disabled(disabled || isLoading)
    }
}

#Preview("Idle") {
    PrimaryButton(title: "Continue", action: {})
        .padding()
}

#Preview("Loading") {
    PrimaryButton(title: "Continue", isLoading: true, action: {})
        .padding()
}

#Preview("Disabled") {
    PrimaryButton(title: "Continue", disabled: true, action: {})
        .padding()
}

#Preview("Dark") {
    PrimaryButton(title: "Continue", action: {})
        .padding()
        .preferredColorScheme(.dark)
}
```

- [ ] **Step 2: Add to Xcode project and build (⌘B)**

Expected: 0 errors.

- [ ] **Step 3: Commit**
```bash
git add iosApp/iosApp/Core/UI/Components/PrimaryButton.swift
git commit -m "feat(ios): add PrimaryButton component"
```

---

## Task 12: OrDivider.swift (iOS)

**Files:**
- Create: `iosApp/iosApp/Core/UI/Components/OrDivider.swift`

- [ ] **Step 1: Create the file**

```swift
import SwiftUI

struct OrDivider: View {
    var body: some View {
        HStack(spacing: 12) {
            VStack { Divider() }
            Text("or")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            VStack { Divider() }
        }
    }
}

#Preview("Light") {
    OrDivider()
        .padding()
}

#Preview("Dark") {
    OrDivider()
        .padding()
        .preferredColorScheme(.dark)
}
```

- [ ] **Step 2: Add to Xcode project and build (⌘B)**

Expected: 0 errors.

- [ ] **Step 3: Commit**
```bash
git add iosApp/iosApp/Core/UI/Components/OrDivider.swift
git commit -m "feat(ios): add OrDivider component"
```

---

## Task 13: SettingsRow.swift (iOS)

Generic row for settings lists. Uses a generic `Trailing` view so callers can pass anything (Toggle, Text, EmptyView). A convenience initialiser covers the no-trailing case.

**Files:**
- Create: `iosApp/iosApp/Core/UI/Components/SettingsRow.swift`

- [ ] **Step 1: Create the file**

```swift
import SwiftUI

struct SettingsRow<Trailing: View>: View {
    var icon: String?
    var iconTint: Color = .accentColor
    var title: String
    var subtitle: String?
    var showChevron: Bool = false
    var action: (() -> Void)?
    @ViewBuilder var trailing: Trailing

    var body: some View {
        let content = HStack(spacing: 14) {
            if let icon {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundStyle(iconTint)
                    .frame(width: 24, alignment: .center)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body)
                    .foregroundStyle(.primary)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()
            trailing

            if showChevron {
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(.tertiaryLabel))
            }
        }
        .contentShape(Rectangle())

        if let action {
            Button(action: action) { content }
                .buttonStyle(.plain)
        } else {
            content
        }
    }
}

extension SettingsRow where Trailing == EmptyView {
    init(
        icon: String? = nil,
        iconTint: Color = .accentColor,
        title: String,
        subtitle: String? = nil,
        showChevron: Bool = false,
        action: (() -> Void)? = nil
    ) {
        self.icon = icon
        self.iconTint = iconTint
        self.title = title
        self.subtitle = subtitle
        self.showChevron = showChevron
        self.action = action
        self.trailing = EmptyView()
    }
}

#Preview("Icon + Chevron") {
    List {
        SettingsRow(icon: "moon.fill", title: "Theme", showChevron: true, action: {})
    }
}

#Preview("With Subtitle") {
    List {
        SettingsRow(icon: "moon.fill", title: "Theme", subtitle: "System", showChevron: true, action: {})
    }
}

#Preview("With Toggle") {
    List {
        SettingsRow(icon: "bell.fill", title: "Notifications") {
            Toggle("", isOn: .constant(true)).labelsHidden()
        }
    }
}

#Preview("No Icon") {
    List {
        SettingsRow(title: "Sign out", action: {})
    }
}

#Preview("Dark") {
    List {
        SettingsRow(icon: "moon.fill", title: "Theme", subtitle: "Dark", showChevron: true, action: {})
    }
    .preferredColorScheme(.dark)
}
```

- [ ] **Step 2: Add to Xcode project and build (⌘B)**

Expected: 0 errors.

- [ ] **Step 3: Commit**
```bash
git add iosApp/iosApp/Core/UI/Components/SettingsRow.swift
git commit -m "feat(ios): add SettingsRow component"
```

---

## Task 14: SelectableRow.swift (iOS)

Checkmark-style single-select row for iOS sheets. Follows iOS HIG convention (checkmark trailing, not radio button).

**Files:**
- Create: `iosApp/iosApp/Core/UI/Components/SelectableRow.swift`

- [ ] **Step 1: Create the file**

```swift
import SwiftUI

struct SelectableRow: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(label)
                    .foregroundStyle(.primary)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Color.accentColor)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.vertical, 4)
    }
}

#Preview("All States") {
    List {
        SelectableRow(label: "System", isSelected: true, action: {})
        SelectableRow(label: "Light", isSelected: false, action: {})
        SelectableRow(label: "Dark", isSelected: false, action: {})
    }
}

#Preview("Dark") {
    List {
        SelectableRow(label: "System", isSelected: true, action: {})
        SelectableRow(label: "Light", isSelected: false, action: {})
        SelectableRow(label: "Dark", isSelected: false, action: {})
    }
    .preferredColorScheme(.dark)
}
```

- [ ] **Step 2: Add to Xcode project and build (⌘B)**

Expected: 0 errors.

- [ ] **Step 3: Commit**
```bash
git add iosApp/iosApp/Core/UI/Components/SelectableRow.swift
git commit -m "feat(ios): add SelectableRow component"
```

---

## Task 15: ThemeMode+UI.swift (iOS)

Adds `displayName` to `ThemeMode` for use in the settings row subtitle. Lives alongside the existing `ThemeMode+ColorScheme.swift`.

**Files:**
- Create: `iosApp/iosApp/Core/UI/Theme/ThemeMode+UI.swift`

- [ ] **Step 1: Create the file**

```swift
import shared

extension ThemeMode {
    var displayName: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        default: return "System"
        }
    }
}
```

- [ ] **Step 2: Add to Xcode project and build (⌘B)**

Expected: 0 errors.

- [ ] **Step 3: Commit**
```bash
git add iosApp/iosApp/Core/UI/Theme/ThemeMode+UI.swift
git commit -m "feat(ios): add ThemeMode.displayName extension"
```

---

## Task 16: Update LoginView.swift (iOS)

Apply `AppHeader`, wrap in a `GeometryReader`+`ScrollView` so content centres on small screens and scrolls on large, style OAuth buttons with `.bordered`+`.large`.

**Files:**
- Modify: `iosApp/iosApp/auth/LoginView.swift`

- [ ] **Step 1: Replace the file content**

```swift
import SwiftUI
import shared

struct LoginView: View {
    @StateObject private var holder = AuthViewModelHolder()

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 0) {
                    Spacer(minLength: 48)

                    AppHeader(text: "YourApp")

                    Spacer(minLength: 48)

                    Text("Sign in")
                        .font(.title)
                        .fontWeight(.bold)
                        .padding(.bottom, 32)

                    if holder.state.isLoading {
                        ProgressView()
                            .frame(height: 120)
                    } else {
                        VStack(spacing: 12) {
                            OAuthButton(
                                label: "Continue with Google",
                                provider: OAuthProvider.google,
                                vm: holder.viewModel
                            )
                            OAuthButton(
                                label: "Continue with Apple",
                                provider: OAuthProvider.apple,
                                vm: holder.viewModel
                            )
                            OAuthButton(
                                label: "Continue with Microsoft",
                                provider: OAuthProvider.microsoft,
                                vm: holder.viewModel
                            )
                            OAuthButton(
                                label: "Continue with GitHub",
                                provider: OAuthProvider.github,
                                vm: holder.viewModel
                            )
                        }
                    }

                    if let error = holder.state.error {
                        Text(error)
                            .foregroundStyle(.red)
                            .font(.caption)
                            .padding(.top, 16)
                    }

                    Spacer(minLength: 48)
                }
                .padding(.horizontal, 32)
                .frame(minHeight: geometry.size.height)
            }
        }
    }
}

private struct OAuthButton: View {
    let label: String
    let provider: OAuthProvider
    let vm: AuthViewModel

    var body: some View {
        Button(label) {
            vm.onAction(action: AuthActionLoginWith(provider: provider))
        }
        .buttonStyle(.bordered)
        .controlSize(.large)
        .frame(maxWidth: .infinity)
    }
}

@MainActor
private final class AuthViewModelHolder: ObservableObject {
    let viewModel: AuthViewModel = KoinHelperKt.getAuthViewModel()
    @Published var state: AuthState = AuthState(isLoading: false, error: nil)

    init() {
        Task { await observeState() }
    }

    private func observeState() async {
        for await s in viewModel.state {
            if let s { state = s }
        }
    }
}
```

- [ ] **Step 2: Build in Xcode (⌘B)**

Expected: 0 errors.

- [ ] **Step 3: Commit**
```bash
git add iosApp/iosApp/auth/LoginView.swift
git commit -m "feat(ios): restyle LoginView with AppHeader and GeometryReader scroll"
```

---

## Task 17: Update SettingsView.swift (iOS)

Replace the segmented picker with a `SettingsRow` that opens a `.sheet` containing `SelectableRow`s for theme selection. Sheet state is local `@State`.

**Files:**
- Modify: `iosApp/iosApp/Features/Settings/SettingsView.swift`

- [ ] **Step 1: Replace the file content**

```swift
import shared
import SwiftUI

struct SettingsView: View {
    @StateObject private var holder = SettingsViewModelHolder()
    @State private var showThemeSheet = false

    var body: some View {
        Form {
            Section("Appearance") {
                SettingsRow(
                    icon: "moon.fill",
                    title: "Theme",
                    subtitle: holder.state.themeMode.displayName,
                    showChevron: true,
                    action: { showThemeSheet = true }
                )
            }
        }
        .navigationTitle("Settings")
        .sheet(isPresented: $showThemeSheet) {
            NavigationStack {
                List {
                    SelectableRow(
                        label: "System",
                        isSelected: holder.state.themeMode == .system,
                        action: {
                            holder.viewModel.onAction(
                                action: SettingsActionSetThemeMode(mode: .system)
                            )
                            showThemeSheet = false
                        }
                    )
                    SelectableRow(
                        label: "Light",
                        isSelected: holder.state.themeMode == .light,
                        action: {
                            holder.viewModel.onAction(
                                action: SettingsActionSetThemeMode(mode: .light)
                            )
                            showThemeSheet = false
                        }
                    )
                    SelectableRow(
                        label: "Dark",
                        isSelected: holder.state.themeMode == .dark,
                        action: {
                            holder.viewModel.onAction(
                                action: SettingsActionSetThemeMode(mode: .dark)
                            )
                            showThemeSheet = false
                        }
                    )
                }
                .navigationTitle("Theme")
                .navigationBarTitleDisplayMode(.inline)
            }
            .presentationDetents([.medium])
            .preferredColorScheme(holder.state.themeMode.preferredColorScheme)
        }
    }
}
```

- [ ] **Step 2: Build in Xcode (⌘B)**

Expected: 0 errors.

- [ ] **Step 3: Commit**
```bash
git add iosApp/iosApp/Features/Settings/SettingsView.swift
git commit -m "feat(ios): restyle SettingsView with SettingsRow and theme sheet"
```
