package org.grakovne.lissen.ui.screens.settings.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.grakovne.lissen.BuildConfig
import org.grakovne.lissen.R
import org.grakovne.lissen.ui.theme.Spacing

@Composable
fun LicenseFooterComposable() {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = Spacing.md)
        .padding(bottom = Spacing.md),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    HorizontalDivider(
      modifier = Modifier.padding(horizontal = Spacing.sm),
      color = colorScheme.outlineVariant,
    )

    Spacer(modifier = Modifier.height(Spacing.md))

    // App name and version
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_notification_silhouette),
        contentDescription = null,
        modifier = Modifier.size(12.dp),
        tint = colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.width(Spacing.xs))

      Text(
        text =
          stringResource(
            R.string.settings_screen_footer_app_name_pattern,
            stringResource(R.string.branding_name),
            BuildConfig.VERSION_NAME,
          ),
        style =
          TextStyle(
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant,
          ),
      )
    }

    Spacer(modifier = Modifier.height(Spacing.sm))

    // Original author copyright
    Text(
      text = stringResource(R.string.settings_screen_footer_copyright_original),
      style =
        TextStyle(
          textAlign = TextAlign.Center,
          fontSize = 10.sp,
          color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        ),
    )

    // Fork contributor copyright
    Text(
      text = stringResource(R.string.settings_screen_footer_copyright_fork),
      style =
        TextStyle(
          textAlign = TextAlign.Center,
          fontSize = 10.sp,
          color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        ),
    )

    Spacer(modifier = Modifier.height(Spacing.xs))

    // License
    Text(
      text = stringResource(R.string.settings_screen_footer_license),
      style =
        TextStyle(
          textAlign = TextAlign.Center,
          fontSize = 10.sp,
          color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        ),
    )
  }
}
