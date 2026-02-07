package org.grakovne.lissen.ui.screens.settings.advanced.cache

import android.text.format.Formatter
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownloadOff
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.ImageLoader
import coil3.request.ImageRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.grakovne.lissen.R
import org.grakovne.lissen.common.withHaptic
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.PlayingChapter
import org.grakovne.lissen.ui.components.AsyncShimmeringImage
import org.grakovne.lissen.ui.components.withScrollbar
import org.grakovne.lissen.ui.extensions.withMinimumTime
import org.grakovne.lissen.viewmodel.CachingModelView
import org.grakovne.lissen.viewmodel.PlayerViewModel

data class VolumeIdentifier(
  val bookId: String,
  val fileId: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CachedItemsSettingsScreen(
  onBack: () -> Unit,
  onNavigateToLibrary: () -> Unit,
  imageLoader: ImageLoader,
  viewModel: CachingModelView = hiltViewModel(),
  playerViewModel: PlayerViewModel = hiltViewModel(),
) {
  val view: View = LocalView.current
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  var pullRefreshing by remember { mutableStateOf(false) }
  val cachedItems = viewModel.libraryPager.collectAsLazyPagingItems()

  var selectionMode by remember { mutableStateOf(false) }
  val selectedVolumes = remember { mutableStateListOf<VolumeIdentifier>() }

  fun refreshContent(showPullRefreshing: Boolean) {
    scope.launch {
      if (showPullRefreshing) {
        pullRefreshing = true
      }

      val minimumTime =
        when (showPullRefreshing) {
          true -> 500L
          false -> 0L
        }

      withMinimumTime(minimumTime) {
        listOf(
          async { viewModel.fetchCachedItems() },
          async { viewModel.refreshStorageStats() },
          async { viewModel.refreshMetadata() },
        ).awaitAll()
      }

      pullRefreshing = false
    }
  }

  val pullRefreshState =
    rememberPullRefreshState(
      refreshing = pullRefreshing,
      onRefresh = {
        withHaptic(view) { refreshContent(showPullRefreshing = true) }
      },
    )

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.settings_screen_cached_items_title),
            style = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colorScheme.onSurface,
          )
        },
        navigationIcon = {
          IconButton(onClick = { if (selectionMode) selectionMode = false else onBack() }) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              tint = colorScheme.onSurface,
            )
          }
        },
        actions = {
          if (cachedItems.itemCount > 0) {
            TextButton(onClick = {
              selectionMode = !selectionMode
              if (!selectionMode) selectedVolumes.clear()
            }) {
              Text(
                text =
                  if (selectionMode) {
                    stringResource(
                      R.string.settings_screen_cached_items_cancel,
                    )
                  } else {
                    stringResource(R.string.settings_screen_cached_items_edit)
                  },
                style = typography.labelLarge,
                color = colorScheme.primary,
              )
            }
          }
        },
      )
    },
    bottomBar = {
      if (selectionMode && selectedVolumes.isNotEmpty()) {
        val totalSizeToReclaim =
          remember(selectedVolumes.toList(), cachedItems.itemCount) {
            calculateReclaimSize(selectedVolumes, cachedItems, viewModel)
          }
        val formattedSize = Formatter.formatFileSize(context, totalSizeToReclaim)
        val playingBook by playerViewModel.book.observeAsState()

        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(Color.Transparent)
              .navigationBarsPadding()
              .padding(bottom = if (playingBook != null) 16.dp else 0.dp),
        ) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
          ) {
            Button(
              onClick = {
                withHaptic(view) {
                  scope.launch {
                    deleteSelectedVolumes(selectedVolumes, cachedItems, viewModel, playerViewModel)
                    selectionMode = false
                    selectedVolumes.clear()
                    refreshContent(false)
                  }
                }
              },
              modifier = Modifier.fillMaxWidth(),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = colorScheme.error,
                  contentColor = colorScheme.onError,
                ),
            ) {
              Text(stringResource(R.string.manage_downloads_free_up, formattedSize))
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .padding(innerPadding)
          .testTag("libraryScreen")
          .pullRefresh(pullRefreshState)
          .fillMaxSize(),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        StorageHeader(viewModel)

        when (cachedItems.itemCount == 0) {
          true -> PolishedCachedItemsEmptyState(onNavigateToLibrary)
          false ->
            CachedItemsComposable(
              cachedItems = cachedItems,
              imageLoader = imageLoader,
              viewModel = viewModel,
              playerViewModel = playerViewModel,
              selectionMode = selectionMode,
              selectedVolumes = selectedVolumes,
              onItemRemoved = { refreshContent(showPullRefreshing = false) },
            )
        }
      }

      PullRefreshIndicator(
        refreshing = pullRefreshing,
        state = pullRefreshState,
        contentColor = colorScheme.primary,
        modifier = Modifier.align(Alignment.TopCenter),
      )
    }
  }
}

@Composable
private fun StorageHeader(viewModel: CachingModelView) {
  val stats by viewModel.storageStats.collectAsState(null)
  val context = LocalContext.current

  stats?.let {
    val usedFormatted = Formatter.formatFileSize(context, it.usedBytes)
    val freeFormatted = Formatter.formatFileSize(context, it.freeBytes)
    val progress = it.usedBytes.toFloat() / (it.usedBytes + it.freeBytes).coerceAtLeast(1L)

    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .background(colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
          .padding(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.SdStorage,
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.settings_screen_cached_items_storage_stats, usedFormatted, freeFormatted),
            style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colorScheme.onSurface,
          )
        }
      }

      Spacer(Modifier.height(12.dp))

      LinearProgressIndicator(
        progress = { progress },
        modifier =
          Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
        color = colorScheme.primary,
        trackColor = colorScheme.onSurface.copy(alpha = 0.1f),
      )
    }
  }
}

@Composable
private fun PolishedCachedItemsEmptyState(onBack: () -> Unit) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Icon(
      imageVector = Icons.Outlined.FileDownloadOff,
      contentDescription = null,
      modifier = Modifier.size(80.dp),
      tint = colorScheme.onSurface.copy(alpha = 0.2f),
    )

    Spacer(Modifier.height(24.dp))

    Text(
      text = stringResource(R.string.settings_screen_cached_items_empty_title),
      style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = colorScheme.onSurface,
    )

    Spacer(Modifier.height(8.dp))

    Text(
      text = stringResource(R.string.settings_screen_cached_items_empty_description),
      style = typography.bodyLarge,
      color = colorScheme.onSurface.copy(alpha = 0.6f),
      textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(32.dp))

    Button(
      onClick = onBack,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = colorScheme.primary,
          contentColor = colorScheme.onPrimary,
        ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.height(48.dp),
    ) {
      Text(
        text = stringResource(R.string.settings_screen_cached_items_empty_action),
        style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
      )
    }
  }
}

@Composable
private fun CachedItemsComposable(
  cachedItems: LazyPagingItems<DetailedItem>,
  imageLoader: ImageLoader,
  viewModel: CachingModelView,
  playerViewModel: PlayerViewModel,
  selectionMode: Boolean,
  selectedVolumes: MutableList<VolumeIdentifier>,
  onItemRemoved: () -> Unit,
) {
  val state = rememberLazyListState()
  val itemsCount by viewModel.totalCount.observeAsState()

  val showScrollbar by remember {
    derivedStateOf {
      val scrolledDown = state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
      state.isScrollInProgress && scrolledDown
    }
  }

  val scrollbarAlpha by animateFloatAsState(
    targetValue = if (showScrollbar) 1f else 0f,
    animationSpec = tween(durationMillis = 300),
  )

  val onBackground = colorScheme.onBackground

  LazyColumn(
    state = state,
    modifier =
      Modifier
        .withScrollbar(
          state = state,
          color = { onBackground.copy(alpha = scrollbarAlpha) },
          totalItems = itemsCount,
        ).fillMaxSize(),
  ) {
    items(count = cachedItems.itemCount, key = { index -> cachedItems[index]?.id ?: "cached_library_item_$index" }) {
      val item = cachedItems[it] ?: return@items
      CachedItemComposable(
        book = item,
        imageLoader = imageLoader,
        viewModel = viewModel,
        playerViewModel = playerViewModel,
        selectionMode = selectionMode,
        selectedVolumes = selectedVolumes,
        onItemRemoved = onItemRemoved,
      )
    }
  }
}

@Composable
private fun CachedItemComposable(
  book: DetailedItem,
  imageLoader: ImageLoader,
  viewModel: CachingModelView,
  playerViewModel: PlayerViewModel,
  selectionMode: Boolean,
  selectedVolumes: MutableList<VolumeIdentifier>,
  onItemRemoved: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val view = LocalView.current
  var expanded by remember { mutableStateOf(false) }

  val bookSize =
    remember(book) {
      Formatter.formatFileSize(context, viewModel.getBookSize(book))
    }

  val imageRequest =
    remember(book.id) {
      ImageRequest
        .Builder(context)
        .data(book.id)
        .build()
    }

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .background(
          color = if (expanded) colorScheme.surfaceVariant.copy(alpha = 0.2f) else colorScheme.surface,
          shape = RoundedCornerShape(12.dp),
        ).animateContentSize()
        .clickable { expanded = expanded.not() }
        .padding(8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      AsyncShimmeringImage(
        imageRequest = imageRequest,
        imageLoader = imageLoader,
        contentDescription = "${book.title} cover",
        contentScale = ContentScale.FillBounds,
        modifier =
          Modifier
            .size(64.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
        error = painterResource(R.drawable.cover_fallback),
      )

      Spacer(Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = book.title,
            style =
              typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onBackground,
              ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )

          Text(
            text = bookSize,
            style =
              typography.labelLarge.copy(
                color = colorScheme.primary,
                fontWeight = FontWeight.Medium,
              ),
            modifier = Modifier.padding(start = 8.dp),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          book
            .author
            ?.takeIf { it.isNotBlank() }
            ?.let {
              Text(
                modifier = Modifier.weight(1f),
                text = it,
                style =
                  typography.bodyMedium.copy(
                    color = colorScheme.onBackground.copy(alpha = 0.6f),
                  ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

          Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colorScheme.onBackground.copy(alpha = 0.4f),
          )
        }
      }

      Spacer(Modifier.width(8.dp))

      if (selectionMode) {
        val downloadedVolumes = remember(book) { viewModel.getVolumes(book).filter { it.isDownloaded } }
        val bookVolumes = downloadedVolumes.map { VolumeIdentifier(book.id, it.id) }
        val isFullySelected = selectedVolumes.containsAll(bookVolumes)

        Checkbox(
          checked = isFullySelected,
          onCheckedChange = { checked ->
            if (checked) {
              bookVolumes.forEach { if (!selectedVolumes.contains(it)) selectedVolumes.add(it) }
            } else {
              selectedVolumes.removeAll(bookVolumes)
            }
          },
          modifier = Modifier.padding(end = 8.dp),
        )
      }

      if (!selectionMode) {
        IconButton(onClick = {
          withHaptic(view) {
            scope.launch {
              dropCache(
                item = book,
                cachingModelView = viewModel,
                playerViewModel = playerViewModel,
              )

              onItemRemoved()
            }
          }
        }) {
          Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            tint = colorScheme.error.copy(alpha = 0.8f),
          )
        }
      }
    }

    AnimatedVisibility(
      visible = expanded,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut(),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
      ) {
        CachedItemVolumeComposable(
          item = book,
          onItemRemoved = onItemRemoved,
          viewModel = viewModel,
          playerViewModel = playerViewModel,
          selectionMode = selectionMode,
          selectedVolumes = selectedVolumes,
        )
      }
    }
  }
}

@Composable
private fun CachedItemVolumeComposable(
  item: DetailedItem,
  onItemRemoved: () -> Unit,
  viewModel: CachingModelView,
  playerViewModel: PlayerViewModel,
  selectionMode: Boolean,
  selectedVolumes: MutableList<VolumeIdentifier>,
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val view = LocalView.current

  val volumes = remember(item) { viewModel.getVolumes(item).filter { it.isDownloaded } }

  volumes.forEachIndexed { index, volume ->
    val volumeSize = remember(volume) { Formatter.formatFileSize(context, volume.size) }
    val isSelected = selectedVolumes.contains(VolumeIdentifier(item.id, volume.id))

    key(volume.id) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable(enabled = selectionMode) {
              val identifier = VolumeIdentifier(item.id, volume.id)
              if (isSelected) selectedVolumes.remove(identifier) else selectedVolumes.add(identifier)
            }.padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = volume.name, style = typography.bodyMedium)
          Text(
            text = volumeSize,
            style = typography.labelMedium.copy(color = colorScheme.onSurface.copy(alpha = 0.5f)),
          )
        }

        if (selectionMode) {
          androidx.compose.material3.Checkbox(
            checked = isSelected,
            onCheckedChange = {
              val identifier = VolumeIdentifier(item.id, volume.id)
              if (it) selectedVolumes.add(identifier) else selectedVolumes.remove(identifier)
            },
            modifier = Modifier.padding(start = 8.dp),
          )
        } else if (volumes.size > 1) {
          IconButton(
            onClick = {
              withHaptic(view) {
                scope.launch {
                  playerViewModel.book.value?.let { playingBook ->
                    if (playingBook.id == item.id) {
                      playerViewModel.clearPlayingBook()
                    }
                  }
                  volume.chapters.forEach { chapter ->
                    viewModel.dropCache(
                      item = item,
                      chapter = chapter,
                    )
                  } // dropCache by chapter handles file deletion
                  onItemRemoved()
                }
              }
            },
            modifier = Modifier.size(32.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Delete,
              contentDescription = null,
              tint = colorScheme.onSurface.copy(alpha = 0.6f),
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      if (index < volumes.lastIndex) {
        HorizontalDivider(
          thickness = 0.5.dp,
          modifier = Modifier.padding(horizontal = 8.dp),
          color = colorScheme.onSurface.copy(alpha = 0.1f),
        )
      }
    }
  }
}

private fun calculateReclaimSize(
  selectedIds: List<VolumeIdentifier>,
  cachedItems: LazyPagingItems<DetailedItem>,
  viewModel: CachingModelView,
): Long {
  if (selectedIds.isEmpty()) return 0L

  val selectedByBook = selectedIds.groupBy { it.bookId }
  var total = 0L

  for (i in 0 until cachedItems.itemCount) {
    val book = cachedItems[i] ?: continue
    val selectionsForBook = selectedByBook[book.id] ?: continue

    val volumes = viewModel.getVolumes(book)
    selectionsForBook.forEach { selection ->
      val volume = volumes.find { it.id == selection.fileId }
      total += volume?.size ?: 0L
    }
  }

  return total
}

private suspend fun deleteSelectedVolumes(
  selectedIds: List<VolumeIdentifier>,
  cachedItems: LazyPagingItems<DetailedItem>,
  viewModel: CachingModelView,
  playerViewModel: PlayerViewModel,
) {
  selectedIds.forEach { selection ->
    val book = (0 until cachedItems.itemCount).mapNotNull { cachedItems[it] }.find { it.id == selection.bookId }
    book?.let {
      val volumes = viewModel.getVolumes(it)
      val volume = volumes.find { v -> v.id == selection.fileId }
      volume?.chapters?.forEach { chapter ->
        playerViewModel.book.value?.let { playingBook ->
          if (playingBook.id == it.id) {
            playerViewModel.clearPlayingBook()
          }
        }
        viewModel.dropCache(it, chapter)
      }
    }
  }
}

private suspend fun dropCache(
  item: DetailedItem,
  chapter: PlayingChapter,
  cachingModelView: CachingModelView,
  playerViewModel: PlayerViewModel,
) {
  playerViewModel.book.value?.let { playingBook ->
    if (playingBook.id == item.id) {
      playerViewModel.clearPlayingBook()
    }
  }

  val isLatestChapter =
    item
      .chapters
      .filter { it.available }
      .let { it - chapter }
      .isEmpty()

  when (isLatestChapter) {
    true -> dropCache(item, cachingModelView, playerViewModel)
    false -> cachingModelView.dropCache(item, chapter)
  }
}

private suspend fun dropCache(
  item: DetailedItem,
  cachingModelView: CachingModelView,
  playerViewModel: PlayerViewModel,
) {
  playerViewModel.book.value?.let { playingBook ->
    if (playingBook.id == item.id) {
      playerViewModel.clearPlayingBook()
    }
  }

  cachingModelView.dropCache(item.id)
}
