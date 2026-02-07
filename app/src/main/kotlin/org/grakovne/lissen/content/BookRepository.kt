package org.grakovne.lissen.content

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.grakovne.lissen.analytics.ClarityTracker
import org.grakovne.lissen.channel.audiobookshelf.AudiobookshelfChannelProvider
import org.grakovne.lissen.channel.common.MediaChannel
import org.grakovne.lissen.channel.common.OperationError
import org.grakovne.lissen.channel.common.OperationResult
import org.grakovne.lissen.common.NetworkService
import org.grakovne.lissen.content.cache.persistent.LocalCacheRepository
import org.grakovne.lissen.content.cache.temporary.CachedCoverProvider
import org.grakovne.lissen.lib.domain.Book
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.Library
import org.grakovne.lissen.lib.domain.PagedItems
import org.grakovne.lissen.lib.domain.PlaybackProgress
import org.grakovne.lissen.lib.domain.PlaybackSession
import org.grakovne.lissen.lib.domain.RecentBook
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository
  @Inject
  constructor(
    private val preferences: LissenSharedPreferences,
    private val audiobookshelfChannelProvider: AudiobookshelfChannelProvider,
    private val localCacheRepository: LocalCacheRepository,
    private val cachedCoverProvider: CachedCoverProvider,
    private val networkService: NetworkService,
    private val clarityTracker: ClarityTracker,
  ) {
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun provideFileUri(
      libraryItemId: String,
      chapterId: String,
    ): OperationResult<Uri> {
      Timber.d("Fetching File $libraryItemId and $chapterId URI")

      localCacheRepository
        .provideFileUri(libraryItemId, chapterId)
        ?.let {
          Timber.d("Providing LOCAL URI for $libraryItemId / $chapterId: $it")
          return OperationResult.Success(it)
        }

      Timber.d("Local URI miss for $libraryItemId / $chapterId. Falling back to REMOTE.")

      return try {
        val uri = providePreferredChannel().provideFileUri(libraryItemId, chapterId)

        if (uri == null) {
          OperationResult.Error(OperationError.InternalError, "Remote URI is null")
        } else {
          Timber.d("Providing REMOTE URI for $libraryItemId / $chapterId: $uri")
          OperationResult.Success(uri)
        }
      } catch (e: Exception) {
        Timber.e(e, "Failed to provide file URI for $libraryItemId and $chapterId")
        OperationResult.Error(OperationError.InternalError, e.message ?: "Unknown error occurred")
      }
    }

    suspend fun syncProgress(
      sessionId: String,
      itemId: String,
      progress: PlaybackProgress,
    ): OperationResult<Unit> {
      Timber.d("Syncing Progress for $itemId. $progress")

      localCacheRepository.syncProgress(itemId, progress)
      clarityTracker.trackEvent("sync_progress")

      val channelSyncResult =
        providePreferredChannel()
          .syncProgress(sessionId, progress)

      return when (preferences.isForceCache()) {
        true -> OperationResult.Success(Unit)
        false -> channelSyncResult
      }
    }

    suspend fun syncLocalProgress(
      itemId: String,
      progress: PlaybackProgress,
    ): OperationResult<Unit> {
      Timber.d("Syncing LOCAL Progress only for $itemId. $progress")
      localCacheRepository.syncProgress(itemId, progress)
      return OperationResult.Success(Unit)
    }

    suspend fun fetchBookCover(
      bookId: String,
      width: Int? = null,
    ): OperationResult<File> {
      val localResult = localCacheRepository.fetchBookCover(bookId, width)
      if (localResult is OperationResult.Success) {
        return localResult
      }

      return cachedCoverProvider.provideCover(
        channel = providePreferredChannel(),
        itemId = bookId,
        width = width,
      )
    }

    suspend fun searchBooks(
      libraryId: String,
      query: String,
      limit: Int,
    ): OperationResult<List<Book>> {
      Timber.d("Searching books with query $query of library: $libraryId")

      val localResult = localCacheRepository.searchBooks(libraryId = libraryId, query = query)
      if (localResult is OperationResult.Success && localResult.data.isNotEmpty()) {
        return localResult
      }

      return providePreferredChannel()
        .searchBooks(
          libraryId = libraryId,
          query = query,
          limit = limit,
        )
    }

    suspend fun fetchLatestUpdate(libraryId: String) = localCacheRepository.fetchLatestUpdate(libraryId)

    suspend fun clearMetadataCache() = localCacheRepository.clearMetadataCache()

    suspend fun fetchBooks(
      libraryId: String,
      pageSize: Int,
      pageNumber: Int,
      downloadedOnly: Boolean = false,
    ): OperationResult<PagedItems<Book>> {
      Timber.d("Fetching page $pageNumber of library: $libraryId. Downloaded only: $downloadedOnly")

      val localResult =
        localCacheRepository.fetchBooks(
          libraryId = libraryId,
          pageSize = pageSize,
          pageNumber = pageNumber,
          downloadedOnly = downloadedOnly,
        )

      if (downloadedOnly) {
        return localResult
      }

      return localResult.fold(
        onSuccess = { OperationResult.Success(it) },
        onFailure = { OperationResult.Success(PagedItems(emptyList(), pageNumber, 0)) },
      )
    }

    suspend fun syncLibraryPage(
      libraryId: String,
      pageSize: Int,
      pageNumber: Int,
    ): OperationResult<Unit> =
      OperationResult
        .Success(Unit)
        .also { backgroundScope.launch { syncAllLibraries() } }

    private suspend fun syncAllLibraries() {
      val librariesResult = fetchLibraries()
      val libraries =
        when (librariesResult) {
          is OperationResult.Success -> librariesResult.data
          is OperationResult.Error -> return
        }

      libraries.forEach { library ->
        syncFullLibrary(library.id)
      }
    }

    private suspend fun syncFullLibrary(libraryId: String) {
      val remoteItemsResult = providePreferredChannel().fetchLibraryMinified(libraryId)

      val remoteItems =
        when (remoteItemsResult) {
          is OperationResult.Success -> remoteItemsResult.data
          is OperationResult.Error -> return
        }

      // Fast Sync: Immediately cache basic metadata (Title, Author, ID)
      // This makes new books visible and searchable instantly.
      // Existing details (Chapters, Duration) are preserved by the repository logic.
      localCacheRepository.cacheBooks(remoteItems)

      val knownBooks =
        localCacheRepository
          .fetchBooks(libraryId, Int.MAX_VALUE, 0, false)
          .fold(
            onSuccess = { it.items },
            onFailure = { emptyList() },
          ).associateBy { it.id }

      val newOrUpdatedBooks =
        remoteItems.filter { remote ->
          val local = knownBooks[remote.id]

          // If local is null (shouldn't be, since we just cached), or remote is newer
          // Note: addedAt/updatedAt are now available in Book
          local == null || (remote.updatedAt > local.updatedAt)
        }

      if (newOrUpdatedBooks.isEmpty()) {
        Timber.d("Local library is up to date. Sync finished.")
        return
      }

      Timber.d("Found ${newOrUpdatedBooks.size} new or updated books. Syncing details...")

      newOrUpdatedBooks
        .chunked(20)
        .forEach { chunk ->
          withContext(Dispatchers.IO) {
            chunk
              .map { book ->
                async {
                  providePreferredChannel()
                    .fetchBook(book.id)
                    .foldAsync(
                      onSuccess = {
                        localCacheRepository.cacheBookMetadata(it)
                        it
                      },
                      onFailure = { null },
                    )
                }
              }.awaitAll()
              .filterNotNull()
              .let { items ->
                backgroundScope.launch {
                  prefetchCovers(items.map { item -> item.toBook() })
                }
              }
          }
        }
    }

    suspend fun fetchLibraries(): OperationResult<List<Library>> {
      Timber.d("Fetching List of libraries")

      val localResult = localCacheRepository.fetchLibraries()
      if (localResult is OperationResult.Success && localResult.data.isNotEmpty()) {
        return localResult
      }

      return providePreferredChannel()
        .fetchLibraries()
        .also {
          it.foldAsync(
            onSuccess = { libraries -> localCacheRepository.updateLibraries(libraries) },
            onFailure = {},
          )
        }
    }

    suspend fun startPlayback(
      itemId: String,
      chapterId: String,
      supportedMimeTypes: List<String>,
      deviceId: String,
    ): OperationResult<PlaybackSession> {
      Timber.d("Starting Playback for $itemId. $supportedMimeTypes are supported")

      return providePreferredChannel().startPlayback(
        bookId = itemId,
        episodeId = chapterId,
        supportedMimeTypes = supportedMimeTypes,
        deviceId = deviceId,
      )
    }

    suspend fun fetchRecentListenedBooks(libraryId: String): OperationResult<List<RecentBook>> {
      Timber.d("Fetching Recent books of library $libraryId")

      val isOffline = !networkService.isServerAvailable.value || preferences.isForceCache()

      val localResult =
        localCacheRepository.fetchRecentListenedBooks(
          libraryId = libraryId,
          downloadedOnly = isOffline,
        )

      if (isOffline) {
        return localResult
      }

      if (localResult is OperationResult.Success && localResult.data.isNotEmpty()) {
        return localResult
      }

      return providePreferredChannel()
        .fetchRecentListenedBooks(libraryId)
        .map { items -> syncFromLocalProgress(libraryId = libraryId, detailedItems = items) }
    }

    fun fetchRecentListenedBooksFlow(libraryId: String): Flow<List<RecentBook>> {
      val isOffline = !networkService.isServerAvailable.value || preferences.isForceCache()

      return localCacheRepository.fetchRecentListenedBooksFlow(
        libraryId = libraryId,
        downloadedOnly = isOffline,
      )
    }

    suspend fun fetchBook(bookId: String): OperationResult<DetailedItem> {
      Timber.d("Fetching Detailed book info for $bookId")

      val localResult = localCacheRepository.fetchBook(bookId)
      val isDetailed =
        localResult
          ?.let { it.chapters.isNotEmpty() || it.files.isNotEmpty() }
          ?: false

      if (localResult != null && isDetailed) {
        return OperationResult.Success(makeAvailableIfOnline(localResult))
      }

      return providePreferredChannel()
        .fetchBook(bookId)
        .map { syncFromLocalProgress(it) }
        .also {
          it.foldAsync(
            onSuccess = { book -> localCacheRepository.cacheBookMetadata(book) },
            onFailure = {},
          )
        }.map { makeAvailableIfOnline(it) }
    }

    private fun makeAvailableIfOnline(book: DetailedItem): DetailedItem {
      if (!networkService.isNetworkAvailable()) {
        return book
      }

      val isAllCached = book.chapters.all { it.available }
      if (isAllCached) {
        return book
      }

      return book.copy(
        chapters = book.chapters.map { it.copy(available = true) },
      )
    }

    fun fetchBookFlow(bookId: String): Flow<DetailedItem?> =
      localCacheRepository
        .fetchBookFlow(bookId)
        .combine(networkService.networkStatus) { book: DetailedItem?, isOnline: Boolean ->
          if (book == null) return@combine null

          val isAllCached = book.chapters.all { it.available }
          if (!isOnline || isAllCached) return@combine book

          book.copy(
            chapters = book.chapters.map { it.copy(available = true) },
          )
        }

    suspend fun syncRepositories(overrideLibraryId: String? = null) {
      val libraryId = overrideLibraryId ?: preferences.getPreferredLibrary()?.id ?: return

      syncFullLibrary(libraryId)

      val remoteRecents = providePreferredChannel().fetchRecentListenedBooks(libraryId).getOrNull() ?: emptyList()
      val localRecents =
        localCacheRepository
          .fetchRecentListenedBooks(
            libraryId = libraryId,
            downloadedOnly = false,
          ).getOrNull() ?: emptyList()

      val remoteMap = remoteRecents.associateBy { it.id }
      val localMap = localRecents.associateBy { it.id }
      val allIds = (remoteMap.keys + localMap.keys).toSet()

      for (id in allIds) {
        val remote = remoteMap[id]
        val local = localMap[id]

        val remoteTime = remote?.listenedLastUpdate ?: 0L
        val localTime = local?.listenedLastUpdate ?: 0L

        when {
          remoteTime > localTime -> {
            providePreferredChannel().fetchBook(id).foldAsync(
              onSuccess = {
                localCacheRepository.cacheBookMetadata(it)
                backgroundScope.launch { prefetchCovers(listOf(it.toBook())) }
              },
              onFailure = {},
            )
          }

          localTime > remoteTime -> {
            val book = localCacheRepository.fetchBook(id) ?: continue
            val progress = book.progress ?: continue

            val session =
              providePreferredChannel()
                .startPlayback(
                  bookId = id,
                  episodeId = book.chapters.firstOrNull()?.id ?: continue,
                  supportedMimeTypes = emptyList(),
                  deviceId = preferences.getDeviceId(),
                ).getOrNull() ?: continue

            providePreferredChannel().syncProgress(
              sessionId = session.sessionId,
              progress =
                PlaybackProgress(
                  currentTotalTime = progress.currentTime,
                  currentChapterTime = 0.0, // Server will recalculate based on total time
                ),
            )
          }
        }
      }
    }

    private suspend fun prefetchCovers(books: List<Book>) {
      if (!networkService.isNetworkAvailable() || preferences.isForceCache()) {
        return
      }

      yield()
      delay(2000) // Initial delay to prioritize core metadata and thumbnails

      withContext(Dispatchers.IO) {
        books.forEach { book ->
          fetchBookCover(book.id, null)
          delay(100)
          yield()
        }
      }
    }

    private fun DetailedItem.toBook() =
      Book(
        id = this.id,
        subtitle = this.subtitle,
        series = this.series.joinToString { it.name },
        title = this.title,
        author = this.author,
        duration = this.chapters.sumOf { it.duration },
        libraryId = this.libraryId ?: "",
      )

    private fun <T> OperationResult<T>.getOrNull(): T? =
      when (this) {
        is OperationResult.Success -> data
        is OperationResult.Error -> null
      }

    private suspend fun syncFromLocalProgress(
      libraryId: String,
      detailedItems: List<RecentBook>,
    ): List<RecentBook> {
      val localRecentlyBooks =
        localCacheRepository
          .fetchRecentListenedBooks(
            libraryId = libraryId,
            downloadedOnly = false,
          ).fold(
            onSuccess = { it },
            onFailure = { return@fold detailedItems },
          )

      val localMap = localRecentlyBooks.associateBy { it.id }

      val syncedRecentlyBooks =
        detailedItems
          .mapNotNull { item -> localMap[item.id]?.let { item to it } }
          .map { (remote, local) ->
            val localTimestamp = local.listenedLastUpdate ?: return@map remote
            val remoteTimestamp = remote.listenedLastUpdate ?: return@map remote

            when (remoteTimestamp > localTimestamp) {
              true -> remote
              false -> local
            }
          }

      val syncedMap = syncedRecentlyBooks.associateBy { it.id }

      return detailedItems
        .map { item ->
          syncedMap
            .get(item.id)
            ?.let { local -> item.copy(listenedPercentage = local.listenedPercentage) }
            ?: item
        }
    }

    private suspend fun syncFromLocalProgress(detailedItem: DetailedItem): DetailedItem {
      val cachedBook = localCacheRepository.fetchBook(detailedItem.id) ?: return detailedItem

      val cachedProgress = cachedBook.progress ?: return detailedItem
      val channelProgress = detailedItem.progress

      if (channelProgress == null) return detailedItem.copy(progress = cachedProgress)

      val updatedProgress =
        if (cachedProgress.lastUpdate > channelProgress.lastUpdate) {
          cachedProgress
        } else {
          channelProgress
        }

      return detailedItem.copy(progress = updatedProgress)
    }

    fun fetchConnectionHost() = providePreferredChannel().fetchConnectionHost()

    suspend fun fetchConnectionInfo() = providePreferredChannel().fetchConnectionInfo()

    fun providePreferredChannel(): MediaChannel = audiobookshelfChannelProvider.provideMediaChannel()
  }
