package org.grakovne.lissen.ui.screens.player.composable

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.grakovne.lissen.R
import org.grakovne.lissen.content.cache.common.findRelatedFilesByStartTimes
import org.grakovne.lissen.content.cache.persistent.calculateRequestedChapters
import org.grakovne.lissen.lib.domain.AllItemsDownloadOption
import org.grakovne.lissen.lib.domain.BookStorageType
import org.grakovne.lissen.lib.domain.BookVolume
import org.grakovne.lissen.lib.domain.CurrentItemDownloadOption
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.DownloadOption
import org.grakovne.lissen.lib.domain.LibraryType
import org.grakovne.lissen.lib.domain.NumberItemDownloadOption
import org.grakovne.lissen.lib.domain.RemainingItemsDownloadOption
import org.grakovne.lissen.lib.domain.SpecificFilesDownloadOption
import org.grakovne.lissen.playback.service.calculateChapterIndex
import org.grakovne.lissen.ui.effects.WindowBlurEffect
import org.grakovne.lissen.ui.screens.common.makeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsComposable(
  book: DetailedItem,
  storageType: BookStorageType,
  volumes: List<BookVolume>,
  isOnline: Boolean,
  cachingInProgress: Boolean,
  onRequestedDownload: (DownloadOption) -> Unit,
  onRequestedStop: () -> Unit,
  onRequestedDrop: () -> Unit,
  onRequestedDropCompleted: () -> Unit,
  onDismissRequest: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val libraryType = book.libraryType ?: LibraryType.UNKNOWN
  val hasCachedContent = volumes.any { it.isDownloaded }
  val isFullBookDownloaded = volumes.all { it.isDownloaded }

  val currentChapterIndex =
    calculateChapterIndex(book, book.progress?.currentTime ?: 0.0)
  val currentVolume =
    volumes.find { volume ->
      volume.chapters.any { it.id == book.chapters.getOrNull(currentChapterIndex)?.id }
    }

  val remainingVolumes =
    if (currentVolume != null) {
      val currentIndex = volumes.indexOf(currentVolume)
      volumes.drop(currentIndex + 1).filter { !it.isDownloaded }
    } else {
      emptyList()
    }

  val completedVolumes = volumes.filter { it.isDownloaded && it.chapters.all { ch -> (book.progress?.currentTime ?: 0.0) >= ch.end } }

  WindowBlurEffect()

  ModalBottomSheet(
    sheetState = sheetState,
    containerColor = colorScheme.background,
    scrimColor = colorScheme.scrim.copy(alpha = 0.65f),
    onDismissRequest = onDismissRequest,
    dragHandle = { DragHandle() },
    content = {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        val title =
          when (libraryType) {
            LibraryType.LIBRARY -> stringResource(R.string.downloads_menu_download_book)
            LibraryType.PODCAST -> stringResource(R.string.downloads_menu_download_podcast)
            LibraryType.UNKNOWN -> stringResource(R.string.downloads_menu_download_unknown)
          }

        Text(
          text = title,
          style =
            typography.titleSmall.copy(
              fontWeight = FontWeight.SemiBold,
              color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
              letterSpacing = 0.8.sp,
              fontSize = 14.sp,
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        val fileStartTimes =
          androidx.compose.runtime.remember(book.id) {
            book.files
              .runningFold(0.0) { acc, file -> acc + file.duration }
              .dropLast(1)
              .let { book.files.zip(it) }
          }

        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          item {
            Surface(
              color = colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
              shape = RoundedCornerShape(16.dp),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .border(
                    width = 0.5.dp,
                    color = colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                  ),
            ) {
              Column {
                // Scenario A: Monolith
                if (storageType == BookStorageType.MONOLITH) {
                  val monolithVolume = volumes.firstOrNull()
                  if (monolithVolume != null && !monolithVolume.isDownloaded) {
                    ActionRow(
                      title =
                        stringResource(
                          R.string.download_modal_monolith_title,
                          Formatter.formatFileSize(context, monolithVolume.size),
                        ),
                      subtitle = stringResource(R.string.download_modal_monolith_description),
                      icon = Icons.Default.LibraryMusic,
                      trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      enabled = isOnline,
                      isSuggested = true,
                      onClick = {
                        onRequestedDownload(AllItemsDownloadOption)
                        onDismissRequest()
                      },
                    )
                  }
                }

                // Scenario B: Segmented
                if (storageType == BookStorageType.SEGMENTED) {
                  if (currentVolume != null && !currentVolume.isDownloaded) {
                    val startIdx = book.chapters.indexOfFirst { it.id == currentVolume.chapters.firstOrNull()?.id }
                    val startChapter = if (startIdx >= 0) startIdx + 1 else null

                    val endIdx = book.chapters.indexOfFirst { it.id == currentVolume.chapters.lastOrNull()?.id }
                    val endChapter = if (endIdx >= 0) endIdx + 1 else null

                    val range =
                      when {
                        startChapter != null && endChapter != null ->
                          if (startChapter ==
                            endChapter
                          ) {
                            "$startChapter"
                          } else {
                            "$startChapter-$endChapter"
                          }
                        else -> "?"
                      }

                    ActionRow(
                      title =
                        stringResource(
                          R.string.download_modal_segmented_part_title,
                          volumes.indexOf(currentVolume) + 1,
                          Formatter.formatFileSize(context, currentVolume.size),
                        ),
                      subtitle = stringResource(R.string.download_modal_segmented_part_subtext, range),
                      icon = Icons.Default.MusicNote,
                      trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      enabled = isOnline,
                      isSuggested = true,
                      onClick = {
                        onRequestedDownload(
                          SpecificFilesDownloadOption(listOf(currentVolume.id)),
                        )
                        onDismissRequest()
                      },
                    )

                    HorizontalDivider(
                      modifier = Modifier.padding(horizontal = 24.dp),
                      thickness = 0.5.dp,
                      color = colorScheme.onSurface.copy(alpha = 0.05f),
                    )
                  }

                  if (remainingVolumes.isNotEmpty()) {
                    val totalSize = remainingVolumes.sumOf { it.size }
                    val startPart = volumes.indexOf(remainingVolumes.first()) + 1
                    val endPart = volumes.indexOf(remainingVolumes.last()) + 1
                    val partRange = if (startPart == endPart) "$startPart" else "$startPart-$endPart"

                    ActionRow(
                      title =
                        stringResource(
                          R.string.download_modal_segmented_remaining,
                          Formatter.formatFileSize(context, totalSize),
                        ),
                      subtitle = stringResource(R.string.download_modal_segmented_remaining_subtext, partRange),
                      icon = Icons.Default.AutoAwesomeMotion,
                      trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      enabled = isOnline,
                      isSuggested = currentVolume?.isDownloaded == true,
                      onClick = {
                        onRequestedDownload(
                          SpecificFilesDownloadOption(remainingVolumes.map { it.id }),
                        )
                        onDismissRequest()
                      },
                    )

                    HorizontalDivider(
                      modifier = Modifier.padding(horizontal = 24.dp),
                      thickness = 0.5.dp,
                      color = colorScheme.onSurface.copy(alpha = 0.05f),
                    )
                  }

                  if (!isFullBookDownloaded) {
                    val totalSize =
                      volumes
                        .filter { !it.isDownloaded }
                        .sumOf { it.size }
                    ActionRow(
                      title = stringResource(R.string.downloads_menu_download_option_entire_book),
                      subtitle = Formatter.formatFileSize(context, totalSize),
                      icon = Icons.Default.Folder,
                      trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      enabled = isOnline,
                      onClick = {
                        onRequestedDownload(AllItemsDownloadOption)
                        onDismissRequest()
                      },
                    )
                  }
                }

                // Scenario C: Atomic
                if (storageType == BookStorageType.ATOMIC) {
                  AtomicOptions.forEachIndexed { index, option ->
                    val requestedChapters =
                      calculateRequestedChapters(
                        book = book,
                        option = option,
                        currentTotalPosition = book.progress?.currentTime ?: 0.0,
                        fileStartTimes = fileStartTimes,
                      )

                    val totalSize =
                      requestedChapters
                        .flatMap { chapter -> findRelatedFilesByStartTimes(chapter, fileStartTimes) }
                        .distinctBy { it.id }
                        .sumOf { it.size }

                    val icon =
                      when (option) {
                        is CurrentItemDownloadOption -> Icons.Default.MusicNote
                        is NumberItemDownloadOption -> Icons.Default.Layers
                        is RemainingItemsDownloadOption -> Icons.AutoMirrored.Filled.QueueMusic
                        is AllItemsDownloadOption -> Icons.Default.Folder
                        else -> Icons.Default.Folder
                      }

                    ActionRow(
                      title = option.makeText(context, libraryType),
                      subtitle = Formatter.formatFileSize(context, totalSize),
                      icon = icon,
                      trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      enabled = isOnline,
                      isSuggested = index == 0,
                      onClick = {
                        onRequestedDownload(option)
                        onDismissRequest()
                      },
                    )

                    if (index < AtomicOptions.size - 1) {
                      HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = 0.5.dp,
                        color = colorScheme.onSurface.copy(alpha = 0.05f),
                      )
                    }
                  }
                }
              }
            }
          }

          if (cachingInProgress || hasCachedContent) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
              Surface(
                color = colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .border(
                      width = 0.5.dp,
                      color = colorScheme.onSurface.copy(alpha = 0.1f),
                      shape = RoundedCornerShape(16.dp),
                    ),
              ) {
                Column {
                  if (cachingInProgress) {
                    ActionRow(
                      title = stringResource(R.string.downloads_menu_download_option_stop_downloads),
                      icon = Icons.Default.StopCircle,
                      trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      isDanger = true,
                      onClick = {
                        onRequestedStop()
                        onDismissRequest()
                      },
                    )

                    if (hasCachedContent) {
                      HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = 0.5.dp,
                        color = colorScheme.onSurface.copy(alpha = 0.05f),
                      )
                    }
                  }

                  if (hasCachedContent) {
                    val showClearCompleted =
                      completedVolumes.isNotEmpty() ||
                        (
                          storageType == BookStorageType.ATOMIC &&
                            book.chapters.any { it.available && (book.progress?.currentTime ?: 0.0) >= it.end }
                        )

                    if (showClearCompleted) {
                      ActionRow(
                        title =
                          when (libraryType) {
                            LibraryType.LIBRARY ->
                              stringResource(
                                R.string.downloads_menu_download_option_clear_completed_chapters,
                              )
                            LibraryType.PODCAST ->
                              stringResource(
                                R.string.downloads_menu_download_option_clear_completed_episodes,
                              )
                            else -> stringResource(R.string.downloads_menu_download_option_clear_completed_items)
                          },
                        icon = Icons.Default.DownloadDone,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        isDanger = true,
                        onClick = {
                          onRequestedDropCompleted()
                          onDismissRequest()
                        },
                      )

                      HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = 0.5.dp,
                        color = colorScheme.onSurface.copy(alpha = 0.05f),
                      )
                    }

                    ActionRow(
                      title =
                        if (storageType == BookStorageType.MONOLITH) {
                          stringResource(R.string.download_modal_remove_book)
                        } else {
                          when (libraryType) {
                            LibraryType.LIBRARY ->
                              stringResource(
                                R.string.downloads_menu_download_option_clear_chapters,
                              )
                            LibraryType.PODCAST ->
                              stringResource(
                                R.string.downloads_menu_download_option_clear_episodes,
                              )
                            else -> stringResource(R.string.downloads_menu_download_option_clear_items)
                          }
                        },
                      icon = Icons.Default.DeleteSweep,
                      trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      isDanger = true,
                      onClick = {
                        onRequestedDrop()
                        onDismissRequest()
                      },
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
  )
}

@Composable
private fun DragHandle() {
  Box(
    modifier =
      Modifier
        .padding(vertical = 8.dp)
        .size(width = 32.dp, height = 4.dp)
        .background(color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(2.dp)),
  )
}

private val AtomicOptions =
  listOf(
    CurrentItemDownloadOption,
    NumberItemDownloadOption(5),
    NumberItemDownloadOption(10),
    RemainingItemsDownloadOption,
    AllItemsDownloadOption,
  )
