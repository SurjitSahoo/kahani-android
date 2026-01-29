package org.grakovne.lissen.ui.screens.settings.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.ui.theme.Spacing

@Composable
fun SettingsSection(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
  ) {
    Text(
      text = title.uppercase(),
      style = typography.labelSmall,
      color = colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = Spacing.sm, bottom = Spacing.xs),
    )
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors =
        CardDefaults.cardColors(
          containerColor = colorScheme.surfaceContainer,
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        content = content,
      )
    }
  }
}
