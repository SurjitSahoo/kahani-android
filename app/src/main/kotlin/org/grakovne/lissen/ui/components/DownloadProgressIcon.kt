package org.grakovne.lissen.ui.components

import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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
  showShine: Boolean = false,
) {
  val isDark = isSystemInDarkTheme()
  val successColor =
    if (isDark) {
      org.grakovne.lissen.ui.theme.DownloadSuccessDark
    } else {
      org.grakovne.lissen.ui.theme.DownloadSuccessLight
    }

  var triggerShine by remember { mutableStateOf(false) }
  var wasCaching by remember { mutableStateOf(false) }

  LaunchedEffect(cacheState.status) {
    if (cacheState.status is CacheStatus.Caching) {
      wasCaching = true
    }

    if (wasCaching && (cacheState.status is CacheStatus.Completed || cacheState.status is CacheStatus.Idle) && isFullyDownloaded) {
      triggerShine = true
      wasCaching = false
    }
  }

  val shineAlpha by animateFloatAsState(
    targetValue = if (showShine && triggerShine) 1f else 0f,
    animationSpec = tween(durationMillis = 1500, easing = EaseOutExpo),
    label = "shineAlpha",
    finishedListener = {
      if (it == 0f) triggerShine = false
    },
  )

  val shineScale by animateFloatAsState(
    targetValue = if (showShine && triggerShine) 3.5f else 1f,
    animationSpec = tween(durationMillis = 1500, easing = EaseOutExpo),
    label = "shineScale",
  )

  Box(contentAlignment = Alignment.Center) {
    if (showShine && triggerShine && shineAlpha > 0f) {
      Box(
        modifier =
          Modifier
            .size(size)
            .graphicsLayer(
              scaleX = shineScale,
              scaleY = shineScale,
              alpha = shineAlpha * (1f - (shineScale - 1f) / 2.5f).coerceIn(0f, 1f),
              clip = false,
            ).background(
              color = successColor.copy(alpha = 0.6f),
              shape = CircleShape,
            ),
      )
    }

    when (cacheState.status) {
      is CacheStatus.Queued -> {
        val queuedDescription = stringResource(R.string.accessibility_id_download_queued)

        CircularProgressIndicator(
          modifier =
            Modifier
              .semantics { contentDescription = queuedDescription }
              .size(size - 4.dp),
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
            contentDescription = stringResource(R.string.accessibility_id_download_complete),
            modifier = Modifier.size(size),
            tint = successColor,
          )
        } else {
          Icon(
            imageVector = Icons.Outlined.CloudDownload,
            contentDescription = stringResource(R.string.accessibility_id_download_available),
            modifier = Modifier.size(size).alpha(0.8f),
            tint = color,
          )
        }
      }

      else -> {
        if (isFullyDownloaded) {
          Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = stringResource(R.string.accessibility_id_download_complete),
            modifier = Modifier.size(size),
            tint = successColor,
          )
        } else {
          Icon(
            imageVector = Icons.Outlined.CloudDownload,
            contentDescription = stringResource(R.string.accessibility_id_download_available),
            modifier = Modifier.size(size).alpha(0.8f),
            tint = color,
          )
        }
      }
    }
  }
}
