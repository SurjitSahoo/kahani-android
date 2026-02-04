package org.grakovne.lissen.ui.components

import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.R
import org.grakovne.lissen.content.cache.persistent.CacheState
import org.grakovne.lissen.lib.domain.CacheStatus

@Composable
fun DownloadProgressIcon(
  cacheState: CacheState,
  isFullyDownloaded: Boolean,
  size: Dp = 24.dp,
  color: Color = LocalContentColor.current,
) {
  Box(contentAlignment = Alignment.Center) {
    when (cacheState.status) {
      is CacheStatus.Queued -> {
        CircularProgressIndicator(
          modifier = Modifier.size(size - 4.dp),
          strokeWidth = 2.dp,
          color = colorScheme.primary,
          trackColor = color.copy(alpha = 0.1f),
          strokeCap = StrokeCap.Round,
        )
      }

      is CacheStatus.Caching -> {
        val targetProgress = cacheState.progress.coerceIn(0.0, 1.0).toFloat()
        val animatedProgress by animateFloatAsState(
          targetValue = targetProgress,
          animationSpec =
            tween(
              durationMillis = 800,
              easing = EaseOutQuart,
            ),
          label = "progress",
        )

        val progressDescription =
          stringResource(
            R.string.download_progress_description,
            (animatedProgress * 100).toInt(),
          )

        CircularProgressIndicator(
          progress = { animatedProgress },
          modifier =
            Modifier
              .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
                contentDescription = progressDescription
              }.size(size - 2.dp),
          strokeWidth = (size - 2.dp) * 0.12f,
          color = colorScheme.primary,
          trackColor = color.copy(alpha = 0.1f),
          strokeCap = StrokeCap.Round,
          gapSize = 0.dp,
        )
      }

      is CacheStatus.Completed -> {
        if (isFullyDownloaded) {
          Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(size),
            tint = colorScheme.primary,
          )
        } else {
          Icon(
            imageVector = Icons.Outlined.CloudDownload,
            contentDescription = stringResource(R.string.player_screen_downloads_navigation),
            modifier = Modifier.size(size).alpha(0.8f),
            tint = color,
          )
        }
      }

      else -> {
        if (isFullyDownloaded) {
          Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(size),
            tint = colorScheme.primary,
          )
        } else {
          Icon(
            imageVector = Icons.Outlined.CloudDownload,
            contentDescription = stringResource(R.string.player_screen_downloads_navigation),
            modifier = Modifier.size(size).alpha(0.8f),
            tint = color,
          )
        }
      }
    }
  }
}
