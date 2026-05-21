package com.yourcompany.kmptemplate.settings

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourcompany.kmptemplate.core.ui.theme.AppTheme
import com.yourcompany.kmptemplate.settings.domain.model.ThemeMode
import com.yourcompany.kmptemplate.settings.presentation.SettingsAction
import com.yourcompany.kmptemplate.settings.presentation.SettingsState

@Composable
fun SettingsScreen(state: SettingsState, onAction: (SettingsAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAction(SettingsAction.SetThemeMode(mode)) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = state.themeMode == mode,
                    onClick = { onAction(SettingsAction.SetThemeMode(mode)) },
                )
                Spacer(Modifier.width(8.dp))
                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dynamic Colors")
                    Text(
                        "Use your wallpaper colors (Android 12+)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.useDynamicColor,
                    onCheckedChange = { onAction(SettingsAction.SetUseDynamicColor(it)) },
                )
            }
        }

        // TODO: add more settings sections here
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    AppTheme(ThemeMode.LIGHT) {
        SettingsScreen(state = SettingsState(), onAction = {})
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenDarkPreview() {
    AppTheme(ThemeMode.DARK) {
        SettingsScreen(state = SettingsState(themeMode = ThemeMode.DARK), onAction = {})
    }
}
