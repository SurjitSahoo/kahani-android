package org.grakovne.lissen.ui.screens.player.composable

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.common.withHaptic
import org.grakovne.lissen.lib.domain.SeekTime
import org.grakovne.lissen.ui.components.GlowIcon
import org.grakovne.lissen.ui.icons.AppIcons
import org.grakovne.lissen.ui.screens.player.composable.common.SeekButton
import org.grakovne.lissen.viewmodel.PlayerViewModel
import org.grakovne.lissen.viewmodel.SettingsViewModel

@Composable
fun PlaybackButtonsComposable(
  viewModel: PlayerViewModel,
  settingsViewModel: SettingsViewModel,
  modifier: Modifier = Modifier,
) {
  val isPlaying by viewModel.isPlaying.observeAsState(false)
  val currentTrackIndex by viewModel.currentChapterIndex.observeAsState(0)
  val book by viewModel.book.observeAsState()
  val chapters = book?.chapters ?: emptyList()

  val seekTime by settingsViewModel.seekTime.observeAsState(SeekTime.Default)
  val showNavButtons by settingsViewModel.showPlayerNavButtons.observeAsState(true)

  val view: View = LocalView.current

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
    horizontalArrangement =
      Arrangement.spacedBy(
        space = if (showNavButtons) 16.dp else 32.dp,
        alignment = Alignment.CenterHorizontally,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (showNavButtons) {
      IconButton(
        onClick = {
          withHaptic(view) { viewModel.previousTrack() }
        },
        enabled = true,
        modifier = Modifier.size(44.dp),
      ) {
        GlowIcon(
          imageVector = AppIcons.SkipPrevious,
          contentDescription = null,
          tint = colorScheme.onSurface,
          glowColor = colorScheme.onSurface.copy(alpha = 0.3f),
          glowRadius = 4.dp,
          modifier = Modifier.size(28.dp),
        )
      }
    }

    SeekButton(
      duration = seekTime.rewind.seconds,
      isForward = false,
      onClick = { withHaptic(view) { viewModel.rewind() } },
    )

    IconButton(
      onClick = { withHaptic(view) { viewModel.togglePlayPause() } },
      modifier = Modifier.size(72.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = colorScheme.primary,
        modifier = Modifier.fillMaxSize(),
        shadowElevation = 4.dp,
      ) {
        Box(contentAlignment = Alignment.Center) {
          GlowIcon(
            imageVector = if (isPlaying) Icons.Filled.Pause else AppIcons.PlayFilled,
            contentDescription = null,
            tint = colorScheme.onPrimary,
            glowColor = colorScheme.onPrimary.copy(alpha = 0.5f),
            glowRadius = 8.dp,
            modifier = Modifier.size(42.dp),
          )
        }
      }
    }

    SeekButton(
      duration = seekTime.forward.seconds,
      isForward = true,
      onClick = { withHaptic(view) { viewModel.forward() } },
    )

    if (showNavButtons) {
      IconButton(
        onClick = {
          if (currentTrackIndex < chapters.size - 1) {
            withHaptic(view) { viewModel.nextTrack() }
          }
        },
        enabled = currentTrackIndex < chapters.size - 1,
        modifier = Modifier.size(44.dp),
      ) {
        GlowIcon(
          imageVector = AppIcons.SkipNext,
          contentDescription = null,
          tint = if (currentTrackIndex < chapters.size - 1) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.3f),
          glowColor = if (currentTrackIndex < chapters.size - 1) colorScheme.onSurface.copy(alpha = 0.3f) else Color.Transparent,
          glowRadius = 4.dp,
          modifier = Modifier.size(28.dp),
        )
      }
    }
  }
}
