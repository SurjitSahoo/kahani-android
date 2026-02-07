package org.grakovne.lissen.playback.service

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.grakovne.lissen.channel.common.OperationError
import org.grakovne.lissen.content.LissenMediaProvider
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.PlaybackProgress
import org.grakovne.lissen.lib.domain.PlaybackSession
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSynchronizationService
  @Inject
  constructor(
    private val exoPlayer: ExoPlayer,
    private val mediaChannel: LissenMediaProvider,
    private val sharedPreferences: LissenSharedPreferences,
  ) {
    private var currentItem: DetailedItem? = null
    private var currentChapterIndex: Int? = null
    private var playbackSession: PlaybackSession? = null
    private val serviceScope = MainScope()
    private var syncJob: Job? = null
    private val syncMutex = Mutex()

    init {
      exoPlayer.addListener(
        object : Player.Listener {
          override fun onEvents(
            player: Player,
            events: Player.Events,
          ) {
            if (syncEvents.any(events::contains)) {
              handleSyncEvent()
            }
          }
        },
      )
    }

    fun startPlaybackSynchronization(item: DetailedItem) {
      serviceScope.coroutineContext.cancelChildren()
      currentItem = item
      runSync()
    }

    fun cancelSynchronization() {
      syncJob?.cancel()
      runSync()
    }

    private fun handleSyncEvent() {
      runSync()

      if (syncJob?.isActive == true) return

      syncJob =
        serviceScope
          .launch {
            while (
              syncJob?.isActive == true &&
              exoPlayer.playWhenReady &&
              exoPlayer.playbackState != Player.STATE_ENDED
            ) {
              val nearStart = exoPlayer.duration - exoPlayer.currentPosition < SHORT_SYNC_WINDOW
              val nearEnd = exoPlayer.currentPosition < SHORT_SYNC_WINDOW

              when (nearEnd || nearStart) {
                true -> delay(SYNC_INTERVAL_SHORT)
                false -> delay(SYNC_INTERVAL_LONG)
              }

              runSync()
            }
          }.also { job ->
            job.invokeOnCompletion {
              syncJob = null
            }
          }
    }

    private fun runSync() {
      serviceScope.launch {
        val elapsedMs = exoPlayer.currentPosition
        val overallProgress = getProgress(elapsedMs) ?: return@launch
        val item = currentItem ?: return@launch
        val session = playbackSession
        val lastChapterIndex = currentChapterIndex

        Timber.d("Trying to sync $overallProgress for ${item.id}")

        launch(Dispatchers.IO) {
          if (syncMutex.tryLock().not()) {
            Timber.d("Sync is already running")
            return@launch
          }

          try {
            val currentIndex = calculateChapterIndex(item, overallProgress.currentTotalTime)

            var activeSession = session
            if (activeSession == null || activeSession.itemId != item.id || currentIndex != lastChapterIndex) {
              activeSession = openPlaybackSession(item, overallProgress)
              withContext(Dispatchers.Main) {
                playbackSession = activeSession
                currentChapterIndex = currentIndex
              }
            }

            mediaChannel.syncLocalProgress(item.id, overallProgress)
            activeSession?.let { requestSync(it, item, overallProgress) }
          } catch (e: Exception) {
            Timber.e(e, "Error during sync")
          } finally {
            syncMutex.unlock()
          }
        }
      }
    }

    private suspend fun requestSync(
      session: PlaybackSession,
      item: DetailedItem,
      overallProgress: PlaybackProgress,
    ) = mediaChannel
      .syncProgress(
        sessionId = session.sessionId,
        itemId = session.itemId,
        progress = overallProgress,
      ).foldAsync(
        onSuccess = {},
        onFailure = {
          when (it.code) {
            OperationError.NotFoundError -> {
              val newSession = openPlaybackSession(item, overallProgress)
              withContext(Dispatchers.Main) {
                playbackSession = newSession
              }
            }
            else -> Unit
          }
        },
      )

    private suspend fun openPlaybackSession(
      item: DetailedItem,
      overallProgress: PlaybackProgress,
    ): PlaybackSession? {
      val chapterIndex = calculateChapterIndex(item, overallProgress.currentTotalTime)
      return mediaChannel
        .startPlayback(
          itemId = item.id,
          deviceId = sharedPreferences.getDeviceId(),
          supportedMimeTypes = MimeTypeProvider.getSupportedMimeTypes(),
          chapterId = item.chapters[chapterIndex].id,
        ).fold(
          onSuccess = { it },
          onFailure = { null },
        )
    }

    private fun getProgress(currentElapsedMs: Long): PlaybackProgress? {
      val currentItem =
        exoPlayer
          .currentMediaItem
          ?.localConfiguration
          ?.tag as? DetailedItem
          ?: return null

      val currentIndex = exoPlayer.currentMediaItemIndex

      val previousDuration =
        currentItem.files
          .take(currentIndex)
          .sumOf { it.duration * 1000 }

      val currentTotalTime = (previousDuration + currentElapsedMs) / 1000.0
      val currentChapterTime = calculateChapterPosition(currentItem, currentTotalTime)

      return PlaybackProgress(
        currentTotalTime = currentTotalTime,
        currentChapterTime = currentChapterTime,
      )
    }

    companion object {
      private const val SYNC_INTERVAL_LONG = 10_000L
      private const val SHORT_SYNC_WINDOW = SYNC_INTERVAL_LONG * 2 - 1

      private const val SYNC_INTERVAL_SHORT = 5_000L

      private val syncEvents =
        listOf(
          Player.EVENT_MEDIA_ITEM_TRANSITION,
          Player.EVENT_PLAYBACK_STATE_CHANGED,
          Player.EVENT_IS_PLAYING_CHANGED,
          Player.EVENT_POSITION_DISCONTINUITY,
        )
    }
  }
