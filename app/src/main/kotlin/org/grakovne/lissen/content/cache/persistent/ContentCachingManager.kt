package org.grakovne.lissen.content.cache.persistent

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.grakovne.lissen.analytics.ClarityTracker
import org.grakovne.lissen.channel.audiobookshelf.common.api.RequestHeadersProvider
import org.grakovne.lissen.channel.common.MediaChannel
import org.grakovne.lissen.channel.common.createOkHttpClient
import org.grakovne.lissen.content.cache.common.findRelatedFiles
import org.grakovne.lissen.content.cache.common.findRelatedFilesByStartTimes
import org.grakovne.lissen.content.cache.common.withBlur
import org.grakovne.lissen.content.cache.common.writeToFile
import org.grakovne.lissen.content.cache.persistent.api.CachedBookRepository
import org.grakovne.lissen.content.cache.persistent.api.CachedLibraryRepository
import org.grakovne.lissen.lib.domain.BookFile
import org.grakovne.lissen.lib.domain.CacheStatus
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.DownloadOption
import org.grakovne.lissen.lib.domain.PlayingChapter
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class ContentCachingManager
  @Inject
  constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: CachedBookRepository,
    private val libraryRepository: CachedLibraryRepository,
    private val properties: OfflineBookStorageProperties,
    private val requestHeadersProvider: RequestHeadersProvider,
    private val preferences: LissenSharedPreferences,
    private val clarityTracker: ClarityTracker,
    private val localCacheRepository: LocalCacheRepository,
  ) {
    fun cacheMediaItem(
      mediaItem: DetailedItem,
      option: DownloadOption,
      channel: MediaChannel,
      currentTotalPosition: Double,
    ) = channelFlow {
      try {
        send(CacheState(CacheStatus.Queued))
        clarityTracker.trackEvent("download_started")

        val fileStartTimes =
          withContext(Dispatchers.Default) {
            val startTimes =
              mediaItem.files
                .runningFold(0.0) { acc, file -> acc + file.duration }
                .dropLast(1)
            mediaItem.files.zip(startTimes)
          }

        val requestedChapters =
          calculateRequestedChapters(
            book = mediaItem,
            option = option,
            currentTotalPosition = currentTotalPosition,
            fileStartTimes = fileStartTimes,
          )

        val existingChapters =
          withContext(Dispatchers.IO) {
            bookRepository
              .fetchBook(bookId = mediaItem.id)
              ?.chapters
              ?.filter { it.available }
              ?: emptyList()
          }

        val cachingChapters = requestedChapters - existingChapters.toSet()

        val requestedFiles =
          withContext(Dispatchers.Default) {
            cachingChapters
              .flatMap { findRelatedFilesByStartTimes(it, fileStartTimes) }
              .distinctBy { it.id }
          }

        if (requestedFiles.isEmpty()) {
          send(CacheState(CacheStatus.Completed))
          return@channelFlow
        }

        send(CacheState(CacheStatus.Caching, 0.0))

        val mediaCachingDeferred =
          async {
            cacheBookMedia(
              mediaItem.id,
              requestedFiles,
              channel,
            ) { send(CacheState(CacheStatus.Caching, it)) }
          }

        val coverCachingDeferred = async { cacheBookCover(mediaItem, channel) }
        val librariesCachingDeferred = async { cacheLibraries(channel) }

        val mediaCachingResult = mediaCachingDeferred.await()
        val coverCachingResult = coverCachingDeferred.await()
        val librariesCachingResult = librariesCachingDeferred.await()

        when {
          listOf(
            mediaCachingResult,
            coverCachingResult,
            librariesCachingResult,
          ).all { it.status == CacheStatus.Completed } -> {
            localCacheRepository.cacheBookMetadata(mediaItem)
            send(CacheState(CacheStatus.Completed))
            clarityTracker.trackEvent("download_finished")
          }

          else -> {
            cachingChapters.forEach { dropCache(mediaItem, it) }
            send(CacheState(CacheStatus.Error))
          }
        }
      } catch (e: Exception) {
        if (e !is kotlinx.coroutines.CancellationException) {
          Timber.e(e, "Failed to cache media item")
          send(CacheState(CacheStatus.Error))
        }
        throw e
      } finally {
        // No additional terminal state needed if completed/error already sent
      }
    }

    suspend fun dropCache(
      item: DetailedItem,
      chapter: PlayingChapter,
    ) {
      bookRepository
        .cacheBook(
          book = item,
          fetchedChapters = emptyList(),
          droppedChapters = listOf(chapter),
        )

      val stillCachedChapters =
        bookRepository
          .fetchBook(item.id)
          ?.chapters
          ?.filter { it.available }
          ?: emptyList()

      if (stillCachedChapters.isEmpty()) {
        dropCache(item.id)
        return
      }

      val stillNeededFiles =
        stillCachedChapters
          .flatMap { findRelatedFiles(it, item.files) }
          .map { it.id }
          .toSet()

      findRequestedFiles(item, listOf(chapter))
        .filter { it.id !in stillNeededFiles }
        .forEach { file ->
          val binaryContent = properties.provideMediaCachePath(item.id, file.id)

          if (binaryContent.exists()) {
            binaryContent.delete()
          }
        }
    }

    suspend fun dropCompletedChapters(item: DetailedItem) {
      val currentTime = item.progress?.currentTime ?: 0.0
      val completedChapters = item.chapters.filter { it.available && it.end <= currentTime }

      if (completedChapters.isEmpty()) return

      completedChapters.forEach { chapter ->
        dropCache(item, chapter)
      }
    }

    suspend fun dropCache(itemId: String) {
      val book = bookRepository.fetchBook(itemId) ?: return

      bookRepository.cacheBook(
        book = book,
        fetchedChapters = emptyList(),
        droppedChapters = book.chapters,
      )

      val cachedContent: File = properties.provideBookCache(itemId)

      if (cachedContent.exists()) {
        cachedContent.deleteRecursively()
      }
    }

    fun hasMetadataCached(mediaItemId: String) = bookRepository.provideCacheState(mediaItemId)

    fun hasMetadataCached(
      mediaItemId: String,
      chapterId: String,
    ) = bookRepository.provideCacheState(mediaItemId, chapterId)

    private suspend fun cacheBookMedia(
      bookId: String,
      files: List<BookFile>,
      channel: MediaChannel,
      onProgress: suspend (Double) -> Unit,
    ): CacheState =
      withContext(Dispatchers.IO) {
        val headers = requestHeadersProvider.fetchRequestHeaders()
        val client =
          createOkHttpClient(
            requestHeaders = headers,
            preferences = preferences,
          )

        files.forEachIndexed { index, file ->
          val uri = channel.provideFileUri(bookId, file.id)
          val requestBuilder = Request.Builder().url(uri.toString())
          headers.forEach { requestBuilder.addHeader(it.name, it.value) }

          val request = requestBuilder.build()
          val response = client.newCall(request).execute()

          if (!response.isSuccessful) {
            Timber.e("Unable to cache media content: $response")
            return@withContext CacheState(CacheStatus.Error)
          }

          val body = response.body
          val dest = properties.provideMediaCachePath(bookId, file.id)
          dest.parentFile?.mkdirs()

          try {
            dest.outputStream().use { output ->
              body.byteStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int
                var totalBytesRead = 0L
                val contentLength = body.contentLength().takeIf { it > 0 } ?: file.size

                var lastReportedProgress = -1.0
                val reportThreshold = 0.01 // 1%

                while (input.read(buffer).also { bytesRead = it } != -1) {
                  output.write(buffer, 0, bytesRead)
                  totalBytesRead += bytesRead

                  val fileProgress = if (contentLength > 0) totalBytesRead.toDouble() / contentLength.toDouble() else 0.0
                  val overallProgress = (index.toDouble() + fileProgress) / files.size.toDouble()

                  if (overallProgress - lastReportedProgress >= reportThreshold || overallProgress >= 1.0) {
                    onProgress(overallProgress)
                    lastReportedProgress = overallProgress
                  }
                }
              }
            }
          } catch (ex: Exception) {
            return@withContext CacheState(CacheStatus.Error)
          }
        }

        CacheState(CacheStatus.Completed)
      }

    private suspend fun cacheBookCover(
      book: DetailedItem,
      channel: MediaChannel,
    ): CacheState {
      val thumbFile = properties.provideBookCoverThumbPath(book.id)
      val rawFile = properties.provideBookCoverRawPath(book.id)

      return withContext(Dispatchers.IO) {
        channel
          .fetchBookCover(book.id, width = null)
          .fold(
            onSuccess = { cover ->
              try {
                cover
                  .peek()
                  .withBlur(context)
                  .writeToFile(rawFile)

                cover
                  .withBlur(context, width = 300) // Trigger thumbnail transformation
                  .writeToFile(thumbFile)
              } catch (ex: Exception) {
                return@fold CacheState(CacheStatus.Error)
              }
            },
            onFailure = {
            },
          )

        CacheState(CacheStatus.Completed)
      }
    }

    private suspend fun cacheLibraries(channel: MediaChannel): CacheState =
      channel
        .fetchLibraries()
        .foldAsync(
          onSuccess = {
            libraryRepository.cacheLibraries(it)
            CacheState(CacheStatus.Completed)
          },
          onFailure = {
            CacheState(CacheStatus.Error)
          },
        )

    private fun findRequestedFiles(
      book: DetailedItem,
      requestedChapters: List<PlayingChapter>,
    ): List<BookFile> =
      requestedChapters
        .flatMap { findRelatedFiles(it, book.files) }
        .distinctBy { it.id }
  }
