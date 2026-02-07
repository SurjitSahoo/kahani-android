package org.grakovne.lissen.playback.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.grakovne.lissen.channel.audiobookshelf.common.api.RequestHeadersProvider
import org.grakovne.lissen.content.LissenMediaProvider
import org.grakovne.lissen.lib.domain.BookFile
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.MediaProgress
import org.grakovne.lissen.lib.domain.TimerOption
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import org.grakovne.lissen.playback.MediaSessionProvider
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
  @Inject
  lateinit var exoPlayer: ExoPlayer

  @Inject
  lateinit var mediaSessionProvider: MediaSessionProvider

  @Inject
  lateinit var mediaProvider: LissenMediaProvider

  @Inject
  lateinit var playbackSynchronizationService: PlaybackSynchronizationService

  @Inject
  lateinit var sharedPreferences: LissenSharedPreferences

  @Inject
  lateinit var channelProvider: LissenMediaProvider

  @Inject
  lateinit var requestHeadersProvider: RequestHeadersProvider

  @Inject
  lateinit var playbackTimer: PlaybackTimer

  @Inject
  lateinit var mediaCache: dagger.Lazy<Cache>

  private var session: MediaSession? = null

  private var artworkJob: kotlinx.coroutines.Job? = null

  private var playbackJob: kotlinx.coroutines.Job? = null

  private var smartRewindApplied = AtomicBoolean(false)

  private val playerServiceScope = MainScope()

  override fun onCreate() {
    super.onCreate()

    session = getSession()

    playerServiceScope.launch {
      combine(
        sharedPreferences.showPlayerNavButtonsFlow,
        sharedPreferences.seekTimeFlow,
      ) { _, _ -> }
        .collect {
          session?.let { currentSession ->
            mediaSessionProvider.updateSessionCommands(currentSession)
          }
        }
    }
  }

  @Suppress("DEPRECATION")
  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    super.onStartCommand(intent, flags, startId)

    when (intent?.action) {
      ACTION_SET_TIMER -> {
        val delay = intent.getDoubleExtra(TIMER_VALUE_EXTRA, 0.0)
        val option = intent.getSerializableExtra(TIMER_OPTION_EXTRA) as? TimerOption

        if (delay > 0 && option != null) {
          setTimer(delay, option)
        }

        return START_NOT_STICKY
      }

      ACTION_CANCEL_TIMER -> {
        cancelTimer()
        return START_NOT_STICKY
      }

      ACTION_PLAY -> {
        playbackJob?.cancel()
        playbackJob =
          playerServiceScope
            .launch {
              checkAndApplySmartRewind()
              if (exoPlayer.playbackState == ExoPlayer.STATE_IDLE) {
                exoPlayer.prepare()
              }
              exoPlayer.setPlaybackSpeed(sharedPreferences.getPlaybackSpeed())
              exoPlayer.playWhenReady = true
            }
        return START_STICKY
      }

      ACTION_PAUSE -> {
        playbackJob?.cancel()
        playbackJob =
          playerServiceScope
            .launch {
              smartRewindApplied.set(false)
              exoPlayer.playWhenReady = false
            }
        return START_NOT_STICKY
      }

      ACTION_SET_PLAYBACK -> {
        val book = intent.getSerializableExtra(BOOK_EXTRA) as? DetailedItem
        book?.let {
          playbackJob?.cancel()
          playbackJob =
            playerServiceScope
              .launch { preparePlayback(it) }
        }
        return START_NOT_STICKY
      }

      ACTION_SEEK_TO -> {
        val book = intent.getSerializableExtra(BOOK_EXTRA) as? DetailedItem
        val position = intent.getDoubleExtra(POSITION, 0.0)
        book?.let { seek(it.files, position) }
        return START_NOT_STICKY
      }

      else -> {
        return START_NOT_STICKY
      }
    }
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = getSession()

  private fun getSession(): MediaSession =
    when (val currentSession = session) {
      null -> mediaSessionProvider.provideMediaSession().also { session = it }
      else -> currentSession
    }

  override fun onDestroy() {
    playbackSynchronizationService.cancelSynchronization()
    playerServiceScope.cancel()

    exoPlayer.clearMediaItems()
    exoPlayer.release()

    session?.release()
    session = null

    super.onDestroy()
  }

  @OptIn(UnstableApi::class)
  private suspend fun preparePlayback(book: DetailedItem) {
    exoPlayer.playWhenReady = false
    smartRewindApplied.set(false)
    artworkJob?.cancel()

    withContext(Dispatchers.IO) {
      val prepareQueue =
        async {
          val sourceFactory =
            LissenDataSourceFactory(
              baseContext = baseContext,
              mediaCache = mediaCache.get(),
              requestHeadersProvider = requestHeadersProvider,
              sharedPreferences = sharedPreferences,
              mediaProvider = mediaProvider,
            )

          val playingQueue =
            book
              .files
              .map { file ->
                val mediaData =
                  MediaMetadata
                    .Builder()
                    .setTitle(file.name)
                    .setArtist(book.title)
                    .build()

                val mediaItem =
                  MediaItem
                    .Builder()
                    .setMediaId(file.id)
                    .setUri(apply(book.id, file.id))
                    .setTag(book)
                    .setMediaMetadata(mediaData)
                    .build()

                ProgressiveMediaSource
                  .Factory(sourceFactory)
                  .createMediaSource(mediaItem)
              }

          withContext(Dispatchers.Main) {
            exoPlayer.setMediaSources(playingQueue)
            exoPlayer.prepare()

            val startPosition = calculateSmartRewindPosition(book)
            val currentPosition = book.progress?.currentTime ?: 0.0

            seek(book.files, startPosition)
            smartRewindApplied.set(true)
          }
        }

      val prepareSession =
        async {
          playbackSynchronizationService.startPlaybackSynchronization(book)
        }

      // Fire-and-forget: Update cover in background without blocking playback readiness.
      artworkJob =
        launch {
          val artworkUri = fetchCover(book) ?: return@launch

          withContext(Dispatchers.Main) {
            for (i in 0 until exoPlayer.mediaItemCount) {
              val currentItem = exoPlayer.getMediaItemAt(i)
              val updatedMetadata =
                currentItem
                  .mediaMetadata
                  .buildUpon()
                  .setArtworkUri(artworkUri)
                  .build()

              val updatedItem =
                currentItem
                  .buildUpon()
                  .setMediaMetadata(updatedMetadata)
                  .build()

              exoPlayer.replaceMediaItem(i, updatedItem)
            }
          }
        }

      awaitAll(prepareSession, prepareQueue)

      val intent =
        Intent(PLAYBACK_READY).apply {
          putExtra(BOOK_EXTRA, book)
        }

      LocalBroadcastManager
        .getInstance(baseContext)
        .sendBroadcast(intent)
    }
  }

  private suspend fun checkAndApplySmartRewind() {
    if (smartRewindApplied.get()) {
      return
    }

    val item = exoPlayer.currentMediaItem?.localConfiguration?.tag as? DetailedItem ?: return

    val startPosition = calculateSmartRewindPosition(item)
    val currentPosition = item.progress?.currentTime ?: 0.0

    if (startPosition < currentPosition) {
      withContext(Dispatchers.Main) {
        Timber.d("Smart rewind applied (on resume). Seeking to $startPosition from $currentPosition")
        seek(item.files, startPosition)
      }
    }

    smartRewindApplied.set(true)
  }

  private fun calculateSmartRewindPosition(book: DetailedItem): Double =
    when (sharedPreferences.getSmartRewindEnabled()) {
      true -> {
        val lastUpdate = book.progress?.lastUpdate ?: 0L
        val currentTime = System.currentTimeMillis()
        val threshold = sharedPreferences.getSmartRewindThreshold().durationMillis
        val rewindDuration = sharedPreferences.getSmartRewindDuration().durationSeconds.toDouble()

        val currentPosition = book.progress?.currentTime ?: 0.0

        if (currentTime - lastUpdate > threshold) {
          (currentPosition - rewindDuration).coerceAtLeast(0.0)
        } else {
          currentPosition
        }
      }

      false -> book.progress?.currentTime ?: 0.0
    }

  private suspend fun fetchCover(book: DetailedItem) =
    mediaProvider
      .fetchBookCover(
        bookId = book.id,
      ).fold(
        onSuccess = { it.toUri() },
        onFailure = { null },
      )

  private fun setTimer(
    delay: Double,
    option: TimerOption,
  ) {
    playbackTimer.startTimer(delay, option)
    Timber.d("Timer started for ${delay * 1000} ms.")
  }

  private fun cancelTimer() {
    playbackTimer.stopTimer()
    Timber.d("Timer canceled.")
  }

  private fun seek(
    items: List<BookFile>,
    position: Double?,
  ) {
    if (items.isEmpty()) {
      Timber.w("Tried to seek position $position in the empty book. Skipping")
      return
    }

    when (position) {
      null -> exoPlayer.seekTo(0, 0)
      else -> {
        val positionMs = (position * 1000).toLong()

        val durationsMs = items.map { (it.duration * 1000).toLong() }
        val cumulativeDurationsMs = durationsMs.runningFold(0L) { acc, duration -> acc + duration }

        val targetChapterIndex = cumulativeDurationsMs.indexOfFirst { it > positionMs }

        when (targetChapterIndex - 1 >= 0) {
          true -> {
            val chapterStartTimeMs = cumulativeDurationsMs[targetChapterIndex - 1]
            val chapterProgressMs = positionMs - chapterStartTimeMs
            exoPlayer.seekTo(targetChapterIndex - 1, chapterProgressMs)
          }

          false -> {
            val lastChapterIndex = items.size - 1
            val lastChapterDurationMs = durationsMs.last()
            exoPlayer.seekTo(lastChapterIndex, lastChapterDurationMs)
          }
        }
      }
    }
  }

  private fun setPlaybackProgress(
    chapters: List<BookFile>,
    progress: MediaProgress?,
  ) = seek(chapters, progress?.currentTime)

  companion object {
    const val ACTION_PLAY = "org.grakovne.lissen.player.service.PLAY"
    const val ACTION_PAUSE = "org.grakovne.lissen.player.service.PAUSE"
    const val ACTION_SET_PLAYBACK = "org.grakovne.lissen.player.service.SET_PLAYBACK"
    const val ACTION_SEEK_TO = "org.grakovne.lissen.player.service.ACTION_SEEK_TO"
    const val ACTION_SET_TIMER = "org.grakovne.lissen.player.service.ACTION_SET_TIMER"
    const val ACTION_CANCEL_TIMER = "org.grakovne.lissen.player.service.CANCEL_TIMER"

    const val BOOK_EXTRA = "org.grakovne.lissen.player.service.BOOK"
    const val TIMER_VALUE_EXTRA = "org.grakovne.lissen.player.service.TIMER_VALUE"
    const val TIMER_OPTION_EXTRA = "org.grakovne.lissen.player.service.TIMER_OPTION"
    const val TIMER_EXPIRED = "org.grakovne.lissen.player.service.TIMER_EXPIRED"
    const val TIMER_TICK = "org.grakovne.lissen.player.service.TIMER_TICK"

    const val TIMER_REMAINING = "org.grakovne.lissen.player.service.TIMER_REMAINING"
    const val PLAYBACK_READY = "org.grakovne.lissen.player.service.PLAYBACK_READY"
    const val POSITION = "org.grakovne.lissen.player.service.POSITION"
  }
}
