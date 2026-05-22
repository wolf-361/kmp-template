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
