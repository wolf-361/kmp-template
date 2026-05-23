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
fun SettingsScreen(state: SettingsState, onAction: (SettingsAction) -> Unit, modifier: Modifier = Modifier) {
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
                        scope.launch {
                            sheetState.hide()
                            showThemeSheet = false
                        }
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
