package org.grakovne.lissen.ui.screens.settings.advanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.ui.theme.Spacing

@Composable
fun AdvancedSettingsNavigationItemComposable(
  title: String,
  description: String,
  onclick: () -> Unit,
  icon: ImageVector? = null,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable { onclick() }
        .padding(start = Spacing.md, end = 12.dp, top = 12.dp, bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.width(Spacing.md))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(bottom = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = description,
        style = typography.bodyMedium,
        color = colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    IconButton(
      onClick = { onclick() },
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
        contentDescription = "Forward",
      )
    }
  }
}
