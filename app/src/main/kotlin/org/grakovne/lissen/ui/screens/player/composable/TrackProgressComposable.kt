package org.grakovne.lissen.ui.screens.player.composable

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.lib.domain.LibraryType
import org.grakovne.lissen.ui.extensions.formatTime
import org.grakovne.lissen.ui.screens.player.composable.common.provideChapterIndexTitle
import org.grakovne.lissen.ui.screens.player.composable.common.sanitizeChapterTitle
import org.grakovne.lissen.viewmodel.PlayerViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TrackProgressComposable(
  viewModel: PlayerViewModel,
  libraryType: LibraryType,
  onChaptersClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentTrackIndex by viewModel.currentChapterIndex.observeAsState(0)
  val currentTrackPosition by viewModel.currentChapterPosition.observeAsState(0.0)
  val currentTrackDuration by viewModel.currentChapterDuration.observeAsState(0.0)

  val book by viewModel.book.observeAsState()
  val context = LocalContext.current

  var sliderPosition by remember { mutableDoubleStateOf(0.0) }
  var isDragging by remember { mutableStateOf(false) }
  var showRemainingTime by remember { mutableStateOf(false) }

  LaunchedEffect(currentTrackPosition, currentTrackIndex, currentTrackDuration) {
    if (!isDragging) {
      sliderPosition = currentTrackPosition
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp),
  ) {
    val rawChapterTitle = book?.chapters?.getOrNull(currentTrackIndex)?.title
    val chapterTitle =
      remember(rawChapterTitle, book?.title) {
        sanitizeChapterTitle(rawChapterTitle, book?.title)
      }

    if (!chapterTitle.isNullOrBlank()) {
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          color = colorScheme.onSurface.copy(alpha = 0.04f),
          border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
          shape = RoundedCornerShape(12.dp),
          modifier =
            Modifier
              .padding(bottom = 8.dp)
              .widthIn(max = 280.dp)
              .clickable { onChaptersClick() },
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.List,
              contentDescription = null,
              tint = colorScheme.onSurface.copy(alpha = 0.8f),
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = chapterTitle,
              style = typography.labelMedium,
              fontWeight = FontWeight.Light,
              color = colorScheme.onSurface,
              maxLines = 1,
              modifier =
                Modifier
                  .weight(1f, fill = false)
                  .basicMarquee(),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = colorScheme.onSurface.copy(alpha = 0.3f),
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier.fillMaxWidth(),
    ) {
      Slider(
        value = sliderPosition.toFloat(),
        onValueChange = { newPosition ->
          isDragging = true
          sliderPosition = newPosition.toDouble()
        },
        onValueChangeFinished = {
          isDragging = false
          viewModel.seekTo(sliderPosition)
        },
        valueRange = 0f..currentTrackDuration.toFloat(),
        colors =
          SliderDefaults.colors(
            activeTrackColor = colorScheme.primary,
            inactiveTrackColor = colorScheme.onSurface.copy(alpha = 0.08f),
          ),
        track = { sliderPositions ->
          SliderDefaults.Track(
            sliderState = sliderPositions,
            modifier = Modifier.height(4.dp),
            thumbTrackGapSize = 0.dp,
            trackInsideCornerSize = 0.dp,
            drawStopIndicator = null,
          )
        },
        thumb = {
          Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            // Sharp Thumb Core (User adjusted size)
            Box(
              modifier =
                Modifier
                  .size(24.dp)
                  .background(colorScheme.primary, CircleShape),
            ) {
              // Internal focal glow ("Bloom" effect)
              Box(
                modifier =
                  Modifier
                    .size(18.dp)
                    .align(Alignment.Center)
                    .blur(4.dp) // Soft focus
                    .background(
                      Brush.radialGradient(
                        colors =
                          listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent,
                          ),
                      ),
                      CircleShape,
                    ),
              )

              // Sharp center dot
              Box(
                modifier =
                  Modifier
                    .size(4.dp)
                    .align(Alignment.Center)
                    .background(Color.White, CircleShape),
              )
            }
          }
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }

    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = sliderPosition.toInt().formatTime(true),
        style = typography.bodySmall.copy(fontWeight = FontWeight.W300),
        color = colorScheme.onSurface,
      )

      val chapterIndexString =
        provideChapterIndexTitle(
          currentTrackIndex = currentTrackIndex,
          book = book,
          libraryType = libraryType,
          context = context,
        )

      Text(
        text = chapterIndexString,
        style = typography.labelSmall,
        fontWeight = FontWeight.W300,
        color = colorScheme.onSurface,
      )

      Text(
        text =
          if (showRemainingTime) {
            "-" +
              maxOf(0.0, currentTrackDuration - sliderPosition)
                .toInt()
                .formatTime(true)
          } else {
            currentTrackDuration.toInt().formatTime(true)
          },
        style = typography.bodySmall.copy(fontWeight = FontWeight.W300),
        color = colorScheme.onSurface,
        modifier = Modifier.clickable { showRemainingTime = !showRemainingTime },
      )
    }
  }
}
