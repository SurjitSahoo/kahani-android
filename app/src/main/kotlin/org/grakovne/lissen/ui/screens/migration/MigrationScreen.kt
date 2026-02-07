package org.grakovne.lissen.ui.screens.migration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.grakovne.lissen.R
import org.grakovne.lissen.ui.theme.Spacing
import org.grakovne.lissen.viewmodel.MigrationState
import org.grakovne.lissen.viewmodel.MigrationViewModel

@Composable
fun MigrationScreen(
  onMigrationComplete: () -> Unit,
  viewModel: MigrationViewModel = hiltViewModel(),
) {
  val state by viewModel.migrationState.observeAsState(MigrationState.Idle)

  LaunchedEffect(state) {
    if (state is MigrationState.Completed) {
      onMigrationComplete()
    }
  }

  LaunchedEffect(Unit) {
    viewModel.startMigration()
  }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
        Modifier
          .padding(Spacing.lg)
          .padding(bottom = Spacing.xl),
    ) {
      when (state) {
        is MigrationState.Error -> {
          Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
          )

          Spacer(modifier = Modifier.height(Spacing.xl))

          Text(
            text = stringResource(R.string.migration_screen_error_message),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
          )

          Spacer(modifier = Modifier.height(Spacing.xl))

          Button(
            onClick = { viewModel.retryMigration() },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
          ) {
            Text(text = stringResource(R.string.migration_screen_retry_button))
          }

          Spacer(modifier = Modifier.height(Spacing.md))

          // Dismiss button is intentionally hidden on error to prevent bypassing failed migration.
          // Users must retry or clear app data (via system settings) if issues persist.
        }

        else -> {
          CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
          )

          Spacer(modifier = Modifier.height(Spacing.xl))

          Text(
            text = stringResource(R.string.migration_screen_message),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
      }
    }
  }
}
