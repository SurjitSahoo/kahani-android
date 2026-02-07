package org.grakovne.lissen.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.grakovne.lissen.content.cache.persistent.CacheState
import org.grakovne.lissen.content.cache.persistent.ContentCachingManager
import org.grakovne.lissen.content.cache.persistent.ContentCachingProgress
import org.grakovne.lissen.content.cache.persistent.ContentCachingService
import org.grakovne.lissen.content.cache.persistent.LocalCacheRepository
import org.grakovne.lissen.content.cache.temporary.CachedCoverProvider
import org.grakovne.lissen.lib.domain.CacheStatus
import org.grakovne.lissen.lib.domain.ContentCachingTask
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.DownloadOption
import org.grakovne.lissen.lib.domain.PlayingChapter
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import org.grakovne.lissen.ui.screens.settings.advanced.cache.CachedItemsPageSource
import java.io.Serializable
import javax.inject.Inject

@HiltViewModel
class CachingModelView
  @Inject
  constructor(
    @ApplicationContext private val context: Context,
    private val localCacheRepository: LocalCacheRepository,
    private val contentCachingProgress: ContentCachingProgress,
    private val contentCachingManager: ContentCachingManager,
    private val preferences: LissenSharedPreferences,
    private val cachedCoverProvider: CachedCoverProvider,
  ) : ViewModel() {
    private val _totalCount = MutableLiveData<Int>()
    val totalCount: LiveData<Int> = _totalCount
    private val _bookCachingProgress = mutableMapOf<String, MutableStateFlow<CacheState>>()

    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: Flow<StorageStats?> = _storageStats

    private val _cacheVersion = MutableStateFlow(0L)
    val cacheVersion: Flow<Long> = _cacheVersion

    data class StorageStats(
      val usedBytes: Long,
      val freeBytes: Long,
      val totalBytes: Long,
    )

    private val pageConfig =
      PagingConfig(
        pageSize = PAGE_SIZE,
        initialLoadSize = PAGE_SIZE,
        prefetchDistance = PAGE_SIZE,
      )

    private var pageSource: PagingSource<Int, DetailedItem>? = null
    val libraryPager: Flow<PagingData<DetailedItem>> by lazy {
      Pager(
        config = pageConfig,
        pagingSourceFactory = {
          val source = CachedItemsPageSource(localCacheRepository) { _totalCount.postValue(it) }

          pageSource = source
          source
        },
      ).flow.cachedIn(viewModelScope)
    }

    init {
      viewModelScope.launch {
        contentCachingProgress.statusFlow.collect { (item, progress) ->
          val flow =
            _bookCachingProgress.getOrPut(item.id) {
              MutableStateFlow(progress)
            }
          flow.value = progress

          if (progress.status is CacheStatus.Completed || progress.status is CacheStatus.Error) {
            _cacheVersion.update { it + 1 }
          }
        }
      }

      refreshStorageStats()
    }

    fun refreshStorageStats() {
      viewModelScope.launch {
        withContext(Dispatchers.IO) {
          val used = localCacheRepository.calculateTotalCacheSize()
          val free = localCacheRepository.getAvailableDiskSpace()
          val total = localCacheRepository.getTotalDiskSpace()
          _storageStats.value = StorageStats(used, free, total)
        }
      }
    }

    fun getBookSize(book: DetailedItem) = localCacheRepository.calculateBookSize(book)

    fun calculateChapterSize(
      bookId: String,
      chapter: org.grakovne.lissen.lib.domain.PlayingChapter,
      files: List<org.grakovne.lissen.lib.domain.BookFile>,
    ) = localCacheRepository.calculateChapterSize(bookId, chapter, files)

    fun getBookStorageType(book: DetailedItem) = localCacheRepository.getBookStorageType(book)

    fun getVolumes(book: DetailedItem) =
      localCacheRepository.mapChaptersToVolumes(book) { type, index ->
        when (type) {
          org.grakovne.lissen.content.cache.persistent.VolumeLabelType.FULL_ARCHIVE ->
            context.getString(
              org.grakovne.lissen.R.string.download_volume_full_archive,
            )
          org.grakovne.lissen.content.cache.persistent.VolumeLabelType.VOLUME ->
            context.getString(
              org.grakovne.lissen.R.string.download_volume_name,
              index,
            )
          org.grakovne.lissen.content.cache.persistent.VolumeLabelType.PART ->
            context.getString(
              org.grakovne.lissen.R.string.download_volume_part,
              index,
            )
        }
      }

    suspend fun clearShortTermCache() {
      withContext(Dispatchers.IO) {
        cachedCoverProvider.clearCache()
      }
    }

    fun cache(
      mediaItem: DetailedItem,
      option: DownloadOption,
    ) {
      val task =
        ContentCachingTask(
          item = mediaItem,
          options = option,
          currentPosition = mediaItem.progress?.currentTime ?: 0.0,
        )

      val intent =
        Intent(context, ContentCachingService::class.java).apply {
          action = ContentCachingService.CACHE_ITEM_ACTION
          putExtra(ContentCachingService.CACHING_TASK_EXTRA, task as Serializable)
        }

      context.startForegroundService(intent)
    }

    fun getProgress(bookId: String) =
      _bookCachingProgress
        .getOrPut(bookId) { MutableStateFlow(CacheState(CacheStatus.Idle)) }

    suspend fun dropCache(bookId: String) {
      contentCachingManager.dropCache(bookId)
      _cacheVersion.update { it + 1 }
      refreshStorageStats()
    }

    suspend fun dropCompletedChapters(item: DetailedItem) {
      contentCachingManager.dropCompletedChapters(item)
      _cacheVersion.update { it + 1 }
      refreshStorageStats()
    }

    fun stopCaching(item: DetailedItem) {
      val intent =
        Intent(context, ContentCachingService::class.java).apply {
          action = ContentCachingService.STOP_CACHING_ACTION
          putExtra(ContentCachingService.CACHING_PLAYING_ITEM, item as Serializable)
        }

      context.startForegroundService(intent)
    }

    suspend fun dropCache(
      item: DetailedItem,
      chapter: PlayingChapter,
    ) {
      contentCachingManager.dropCache(item, chapter)
      _cacheVersion.update { it + 1 }
      refreshStorageStats()
    }

    fun toggleCacheForce() {
      when (localCacheUsing()) {
        true -> preferences.disableForceCache()
        false -> preferences.enableForceCache()
      }
    }

    fun localCacheUsing() = preferences.isForceCache()

    fun provideCacheState(
      bookId: String,
      chapterId: String,
    ): LiveData<Boolean> = contentCachingManager.hasMetadataCached(bookId, chapterId)

    fun fetchCachedItems() {
      viewModelScope.launch {
        withContext(Dispatchers.IO) {
          pageSource?.invalidate()
        }
      }
    }

    fun refreshMetadata() {
      viewModelScope.launch {
        withContext(Dispatchers.IO) {
          var page = 0
          var hasMore = true

          while (hasMore) {
            val items =
              localCacheRepository
                .fetchDetailedItems(BATCH_SIZE, page)
                .fold(
                  onSuccess = { it.items },
                  onFailure = { emptyList() },
                )

            items.forEach { localCacheRepository.cacheBookMetadata(it) }

            if (items.size < BATCH_SIZE) {
              hasMore = false
            } else {
              page++
            }
          }

          _cacheVersion.update { it + 1 }
          refreshStorageStats()
        }
      }
    }

    suspend fun fetchLatestUpdate(libraryId: String) = localCacheRepository.fetchLatestUpdate(libraryId)

    companion object {
      private const val PAGE_SIZE = 20
      private const val BATCH_SIZE = 50
    }
  }
