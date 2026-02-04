package org.grakovne.lissen.ui.screens.player.composable

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.map
import kotlinx.coroutines.launch
import org.grakovne.lissen.R
import org.grakovne.lissen.common.PlaybackVolumeBoost
import org.grakovne.lissen.content.cache.persistent.CacheState
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.LibraryType
import org.grakovne.lissen.ui.components.DownloadProgressIcon
import org.grakovne.lissen.ui.extensions.formatTime
import org.grakovne.lissen.ui.navigation.AppNavigationService
import org.grakovne.lissen.ui.screens.settings.composable.CommonSettingsItem
import org.grakovne.lissen.ui.screens.settings.composable.CommonSettingsItemComposable
import org.grakovne.lissen.viewmodel.CachingModelView
import org.grakovne.lissen.viewmodel.PlayerViewModel
import org.grakovne.lissen.viewmodel.SettingsViewModel

@Composable
fun NavigationBarComposable(
  book: DetailedItem,
  playerViewModel: PlayerViewModel,
  contentCachingModelView: CachingModelView,
  settingsViewModel: SettingsViewModel,
  navController: AppNavigationService,
  modifier: Modifier = Modifier,
  onDownloadsClick: () -> Unit = {},
  libraryType: LibraryType,
) {
  val timerOption by playerViewModel.timerOption.observeAsState(null)
  val timerRemaining by playerViewModel.timerRemaining.observeAsState(0)
  val playbackSpeed by playerViewModel.playbackSpeed.observeAsState(1f)
  val playingQueueExpanded by playerViewModel.playingQueueExpanded.observeAsState(false)
  val hasEpisodes by playerViewModel.book.map { book.chapters.isNotEmpty() }.observeAsState(true)
  val preferredPlaybackVolumeBoost by settingsViewModel.preferredPlaybackVolumeBoost.observeAsState()
  val isOnline by playerViewModel.isOnline.collectAsState(initial = false)

  var playbackSpeedExpanded by remember { mutableStateOf(false) }
  var timerExpanded by remember { mutableStateOf(false) }

  var volumeBoostExpanded by remember { mutableStateOf(false) }

  val scope = rememberCoroutineScope()
  val context = androidx.compose.ui.platform.LocalContext.current

  val cacheProgress by contentCachingModelView
    .getProgress(
      book.id,
    ).collectAsState(
      initial =
        org.grakovne.lissen.content.cache.persistent
          .CacheState(org.grakovne.lissen.lib.domain.CacheStatus.Idle),
    )

  val cacheVersion by contentCachingModelView.cacheVersion.collectAsState(initial = 0L)
  val volumes = remember(book, cacheProgress.status, cacheVersion) { contentCachingModelView.getVolumes(book) }
  val isFullyDownloaded = volumes.isNotEmpty() && volumes.all { it.isDownloaded }

  // Get volume boost label
  val volumeBoostIsActive = preferredPlaybackVolumeBoost != null && preferredPlaybackVolumeBoost != PlaybackVolumeBoost.DISABLED

  val volumeBoostDetail =
    when (preferredPlaybackVolumeBoost) {
      PlaybackVolumeBoost.LOW -> stringResource(R.string.volume_boost_low)
      PlaybackVolumeBoost.MEDIUM -> stringResource(R.string.volume_boost_medium)
      PlaybackVolumeBoost.HIGH -> stringResource(R.string.volume_boost_high)
      PlaybackVolumeBoost.MAX -> stringResource(R.string.volume_boost_max)
      else -> null
    }

  val volumeBoostLabel = volumeBoostDetail ?: stringResource(R.string.player_nav_boost)

  Surface(
    color = Color.Transparent,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val iconSize = 24.dp

        // Download button
        PlayerActionItem(
          icon = {
            DownloadProgressIcon(
              cacheState = cacheProgress,
              isFullyDownloaded = isFullyDownloaded,
              size = iconSize,
              color = colorScheme.onSurface,
            )
          },
          label = stringResource(R.string.player_nav_download),
          isActive = false,
          enabled = true,
          onClick = onDownloadsClick,
        )

        // Volume/Boost button
        PlayerActionItem(
          icon = {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = null,
              tint = if (volumeBoostIsActive) colorScheme.tertiary else colorScheme.onSurface,
              modifier = Modifier.size(iconSize),
            )
          },
          label = volumeBoostLabel,
          isActive = volumeBoostIsActive,
          enabled = true,
          onClick = { volumeBoostExpanded = true },
        )

        // Speed button
        PlayerActionItem(
          icon = {
            Icon(
              imageVector = Icons.Filled.Speed,
              contentDescription = null,
              tint = if (playbackSpeed != 1f) colorScheme.tertiary else colorScheme.onSurface,
              modifier = Modifier.size(iconSize),
            )
          },
          label = if (playbackSpeed != 1f) "${playbackSpeed}x" else stringResource(R.string.player_nav_speed),
          isActive = playbackSpeed != 1f,
          enabled = hasEpisodes,
          onClick = { playbackSpeedExpanded = true },
        )

        // Timer button
        PlayerActionItem(
          icon = {
            Icon(
              imageVector = Icons.Filled.Timer,
              contentDescription = null,
              tint = if (timerOption != null) colorScheme.tertiary else colorScheme.onSurface,
              modifier = Modifier.size(iconSize),
            )
          },
          label = if (timerOption != null) (timerRemaining ?: 0).toInt().formatTime() else stringResource(R.string.player_nav_timer),
          isActive = timerOption != null,
          enabled = hasEpisodes,
          onClick = { timerExpanded = true },
        )
      }
    }
  }

  if (playbackSpeedExpanded) {
    PlaybackSpeedComposable(
      currentSpeed = playbackSpeed,
      onSpeedChange = { playerViewModel.setPlaybackSpeed(it) },
      onDismissRequest = { playbackSpeedExpanded = false },
    )
  }

  if (timerExpanded) {
    TimerComposable(
      libraryType = libraryType,
      currentOption = timerOption,
      onOptionSelected = { playerViewModel.setTimer(it) },
      onDismissRequest = { timerExpanded = false },
    )
  }

  if (volumeBoostExpanded) {
    CommonSettingsItemComposable(
      title = stringResource(R.string.volume_boost_title),
      items =
        listOf(
          PlaybackVolumeBoost.DISABLED.toItem(context),
          PlaybackVolumeBoost.LOW.toItem(context),
          PlaybackVolumeBoost.MEDIUM.toItem(context),
          PlaybackVolumeBoost.HIGH.toItem(context),
          PlaybackVolumeBoost.MAX.toItem(context),
        ),
      selectedItem = preferredPlaybackVolumeBoost?.toItem(context),
      onDismissRequest = { volumeBoostExpanded = false },
      onItemSelected = { item ->
        PlaybackVolumeBoost
          .entries
          .find { it.name == item.id }
          ?.let { settingsViewModel.preferPlaybackVolumeBoost(it) }
      },
    )
  }
}

@Composable
private fun PlayerActionItem(
  icon: @Composable () -> Unit,
  label: String,
  isActive: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      modifier
        .clickable(
          enabled = enabled,
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onClick,
        ).padding(horizontal = 12.dp, vertical = 6.dp),
  ) {
    icon()
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      style = typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = if (isActive) colorScheme.tertiary else colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private fun PlaybackVolumeBoost.toItem(context: Context): CommonSettingsItem {
  val id = this.name
  val name =
    when (this) {
      PlaybackVolumeBoost.DISABLED -> context.getString(R.string.volume_boost_disabled)
      PlaybackVolumeBoost.LOW -> context.getString(R.string.volume_boost_low)
      PlaybackVolumeBoost.MEDIUM -> context.getString(R.string.volume_boost_medium)
      PlaybackVolumeBoost.HIGH -> context.getString(R.string.volume_boost_high)
      PlaybackVolumeBoost.MAX -> context.getString(R.string.volume_boost_max)
    }

  return CommonSettingsItem(id, name, null)
}
