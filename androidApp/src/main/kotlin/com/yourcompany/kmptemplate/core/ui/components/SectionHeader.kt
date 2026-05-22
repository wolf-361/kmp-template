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
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
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
