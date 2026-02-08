package org.grakovne.lissen.ui.screens.settings.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.R
import org.grakovne.lissen.channel.audiobookshelf.HostType
import org.grakovne.lissen.common.maskForAnalytics
import org.grakovne.lissen.ui.navigation.AppNavigationService
import org.grakovne.lissen.ui.theme.Spacing
import org.grakovne.lissen.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsComposable(
  navController: AppNavigationService,
  viewModel: SettingsViewModel,
) {
  var connectionInfoExpanded by remember { mutableStateOf(false) }

  val localUrls by viewModel.localUrls.observeAsState(emptyList())
  val host by viewModel.host.observeAsState()

  LaunchedEffect(Unit) {
    viewModel.refreshConnectionInfo()
  }

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable { connectionInfoExpanded = true }
        .padding(start = Spacing.md, end = 12.dp, top = 12.dp, bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Outlined.Cloud,
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(Spacing.md))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.settings_screen_server_connection),
        style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(bottom = 4.dp),
      )

      host?.let {
        Text(
          text = it.url,
          style = typography.bodyMedium,
          color = colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.maskForAnalytics(),
        )
      }
    }
    IconButton(
      onClick = {
        navController.showLogin()
        viewModel.logout()
      },
      modifier =
        Modifier
          .background(
            color = colorScheme.errorContainer,
            shape = RoundedCornerShape(8.dp),
          ).size(36.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.DeleteOutline,
        contentDescription = "Logout",
        tint = colorScheme.onErrorContainer,
        modifier = Modifier.size(20.dp),
      )
    }
  }

  if (connectionInfoExpanded) {
    ModalBottomSheet(
      containerColor = MaterialTheme.colorScheme.background,
      onDismissRequest = { connectionInfoExpanded = false },
      content = {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
              .padding(horizontal = 16.dp),
        ) {
          viewModel.username.value?.let {
            InfoRow(
              label = stringResource(R.string.settings_screen_connected_as_title),
              value = it,
              modifier = Modifier.maskForAnalytics(),
            )

            HorizontalDivider()
          }

          viewModel.serverVersion.value?.let {
            InfoRow(
              label = stringResource(R.string.settings_screen_server_version),
              value = it,
            )
          }

          if (localUrls.isNotEmpty() && host != null) {
            val connectionType =
              when (host?.type) {
                HostType.INTERNAL -> stringResource(R.string.settings_screen_connection_local)
                HostType.EXTERNAL -> stringResource(R.string.settings_screen_connection_external)
                else -> ""
              }

            HorizontalDivider()

            InfoRow(
              label = stringResource(R.string.settings_screen_connection_type),
              value = connectionType,
            )
          }
        }
      },
    )
  }
}

@Composable
fun InfoRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  ListItem(
    modifier = modifier,
    headlineContent = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(text = label)
        Text(text = value)
      }
    },
  )
}
