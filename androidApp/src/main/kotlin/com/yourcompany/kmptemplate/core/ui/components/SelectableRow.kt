package com.yourcompany.kmptemplate.core.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
fun SelectableRow(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        Column {
            SelectableRow(label = "System", isSelected = true, onClick = {})
            SelectableRow(label = "Light", isSelected = false, onClick = {})
            SelectableRow(label = "Dark", isSelected = false, onClick = {})
        }
    }
}
