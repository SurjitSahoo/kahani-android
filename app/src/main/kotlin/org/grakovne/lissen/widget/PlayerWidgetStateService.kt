package org.grakovne.lissen.widget

import android.content.Context
import androidx.annotation.OptIn
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.asFlow
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.grakovne.lissen.common.RunningComponent
import org.grakovne.lissen.content.LissenMediaProvider
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import org.grakovne.lissen.playback.MediaRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(UnstableApi::class)
class PlayerWidgetStateService
  @Inject
  constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val mediaProvider: LissenMediaProvider,
    private val preferences: LissenSharedPreferences,
  ) : RunningComponent {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
      scope.launch {
        combine(
          mediaRepository.playingBook.asFlow().distinctUntilChanged(),
          mediaRepository.isPlaying
            .asFlow()
            .filterNotNull()
            .distinctUntilChanged(),
          mediaRepository.currentChapterIndex.asFlow().distinctUntilChanged(),
          preferences.seekTimeFlow.distinctUntilChanged(),
        ) { playingItem, isPlaying, chapterIndex, seekTime ->
          val chapterTitle = provideChapterTitle(playingItem, chapterIndex)

          val maybeCover =
            playingItem
              ?.id
              ?.let { mediaProvider.fetchBookCover(it) }
              ?.fold(
                onSuccess = { it },
                onFailure = { null },
              )

          PlayingItemState(
            id = playingItem?.id ?: "",
            title = playingItem?.title ?: "",
            chapterTitle = chapterTitle,
            isPlaying = isPlaying,
            coverFile = maybeCover,
            rewindIcon = provideRewindIcon(seekTime.rewind.seconds),
            forwardIcon = provideForwardIcon(seekTime.forward.seconds),
          )
        }.collect { playingItemState ->
          updatePlayingItem(playingItemState)
        }
      }
    }

    private fun provideChapterTitle(
      item: DetailedItem?,
      chapterIndex: Int?,
    ): String? {
      if (null == item || null == chapterIndex) {
        return null
      }

      return when (chapterIndex in item.chapters.indices) {
        true -> item.chapters[chapterIndex].title
        false -> item.title
      }
    }

    private suspend fun updatePlayingItem(state: PlayingItemState) {
      val manager = GlanceAppWidgetManager(context)
      val glanceIds = manager.getGlanceIds(PlayerWidget::class.java)

      glanceIds
        .forEach { glanceId ->
          updateAppWidgetState(context, glanceId) { prefs ->
            prefs[PlayerWidget.bookId] = state.id
            prefs[PlayerWidget.coverPath] = state.coverFile?.absolutePath ?: ""
            prefs[PlayerWidget.title] = state.title
            prefs[PlayerWidget.chapterTitle] = state.chapterTitle ?: ""
            prefs[PlayerWidget.isPlaying] = state.isPlaying
            prefs[PlayerWidget.rewindIconRes] = state.rewindIcon
            prefs[PlayerWidget.forwardIconRes] = state.forwardIcon
          }
          PlayerWidget().update(context, glanceId)
        }
    }

    private fun provideRewindIcon(duration: Int) =
      when (duration) {
        5 -> org.grakovne.lissen.R.drawable.ic_notification_rewind_5
        10 -> org.grakovne.lissen.R.drawable.ic_notification_rewind_10
        15 -> org.grakovne.lissen.R.drawable.ic_notification_rewind_15
        30 -> org.grakovne.lissen.R.drawable.ic_notification_rewind_30
        60 -> org.grakovne.lissen.R.drawable.ic_notification_rewind_60
        else -> org.grakovne.lissen.R.drawable.ic_notification_rewind_10
      }

    private fun provideForwardIcon(duration: Int) =
      when (duration) {
        5 -> org.grakovne.lissen.R.drawable.ic_notification_forward_5
        10 -> org.grakovne.lissen.R.drawable.ic_notification_forward_10
        15 -> org.grakovne.lissen.R.drawable.ic_notification_forward_15
        30 -> org.grakovne.lissen.R.drawable.ic_notification_forward_30
        60 -> org.grakovne.lissen.R.drawable.ic_notification_forward_60
        else -> org.grakovne.lissen.R.drawable.ic_notification_forward_30
      }
  }

data class PlayingItemState(
  val id: String,
  val title: String,
  val chapterTitle: String?,
  val isPlaying: Boolean = false,
  val coverFile: File?,
  val rewindIcon: Int,
  val forwardIcon: Int,
)
