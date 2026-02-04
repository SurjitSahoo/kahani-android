package org.grakovne.lissen.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import kotlinx.coroutines.launch
import org.grakovne.lissen.R
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.ui.icons.Search
import org.grakovne.lissen.ui.navigation.AppNavigationService
import org.grakovne.lissen.ui.screens.player.composable.ChaptersBottomSheet
import org.grakovne.lissen.ui.screens.player.composable.DownloadsComposable
import org.grakovne.lissen.ui.screens.player.composable.MediaDetailComposable
import org.grakovne.lissen.ui.screens.player.composable.NavigationBarComposable
import org.grakovne.lissen.ui.screens.player.composable.PlaybackButtonsComposable
import org.grakovne.lissen.ui.screens.player.composable.PlayingQueueComposable
import org.grakovne.lissen.ui.screens.player.composable.TrackDetailsComposable
import org.grakovne.lissen.ui.screens.player.composable.TrackProgressComposable
import org.grakovne.lissen.ui.screens.player.composable.common.provideNowPlayingTitle
import org.grakovne.lissen.ui.screens.player.composable.fallback.PlayingQueueFallbackComposable
import org.grakovne.lissen.ui.screens.player.composable.placeholder.PlayingQueuePlaceholderComposable
import org.grakovne.lissen.ui.screens.player.composable.placeholder.TrackControlPlaceholderComposable
import org.grakovne.lissen.ui.screens.player.composable.placeholder.TrackDetailsPlaceholderComposable
import org.grakovne.lissen.viewmodel.CachingModelView
import org.grakovne.lissen.viewmodel.LibraryViewModel
import org.grakovne.lissen.viewmodel.PlayerViewModel
import org.grakovne.lissen.viewmodel.SettingsViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
  navController: AppNavigationService,
  imageLoader: ImageLoader,
  bookId: String,
  bookTitle: String,
  bookSubtitle: String?,
  playInstantly: Boolean,
) {
  val context = LocalContext.current

  val cachingModelView: CachingModelView = hiltViewModel()
  val playerViewModel: PlayerViewModel = hiltViewModel()
  val libraryViewModel: LibraryViewModel = hiltViewModel()
  val settingsViewModel: SettingsViewModel = hiltViewModel()

  val titleTextStyle = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
  val scope = rememberCoroutineScope()

  val isOnline by playerViewModel.isOnline.collectAsState(initial = false)

  var showChaptersList by remember { mutableStateOf(false) }

  val playingBook by playerViewModel.book.observeAsState()
  val isPlaybackReady by playerViewModel.isPlaybackReady.observeAsState(false)
  val playingQueueExpanded by playerViewModel.playingQueueExpanded.observeAsState(false)
  val searchRequested by playerViewModel.searchRequested.observeAsState(false)

  var itemDetailsSelected by remember { mutableStateOf(false) }
  var downloadsExpanded by remember { mutableStateOf(false) }

  val screenTitle =
    when (playingQueueExpanded) {
      true -> provideNowPlayingTitle(libraryViewModel.fetchPreferredLibraryType(), context)
      false -> stringResource(R.string.player_screen_title)
    }

  fun stepBack() {
    when {
      searchRequested -> playerViewModel.dismissSearch()
      playingQueueExpanded -> playerViewModel.collapsePlayingQueue()
      else -> navController.showLibrary(clearHistory = true)
    }
  }

  BackHandler(enabled = searchRequested || playingQueueExpanded || playInstantly) {
    stepBack()
  }

  LaunchedEffect(Unit) {
    bookId
      .takeIf { playingItemChanged(it, playingBook) || cachePolicyChanged(cachingModelView, playingBook) }
      ?.let {
        if (settingsViewModel.hasCredentials().not()) {
          navController.showLogin()
          return@LaunchedEffect
        }

        playerViewModel.preparePlayback(it)
      }

    if (playInstantly) {
      playerViewModel.prepareAndPlay()
    }
  }

  LaunchedEffect(playingQueueExpanded) {
    if (playingQueueExpanded.not()) {
      playerViewModel.dismissSearch()
    }
  }

  // Image request for dynamic background
  val backgroundImageRequest =
    remember<ImageRequest?>(playingBook?.id) {
      playingBook?.id?.let {
        ImageRequest
          .Builder(context)
          .data(it)
          .size(300)
          .memoryCacheKey("${playingBook?.id}_thumb")
          .diskCacheKey("${playingBook?.id}_thumb")
          .build()
      }
    }

  Box(modifier = Modifier.fillMaxSize()) {
    // Dynamic blurred background from book cover
    if (playingBook != null && backgroundImageRequest != null) {
      val blurModifier =
        if (android.os.Build.VERSION.SDK_INT >= 31) {
          Modifier.blur(radius = 80.dp)
        } else {
          Modifier
        }

      AsyncImage(
        model = backgroundImageRequest,
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
          Modifier
            .fillMaxSize()
            .then(blurModifier)
            .alpha(0.6f),
      )

      // Gradient scrim for readability
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colorStops =
                  arrayOf(
                    0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.1f),
                    0.4f to MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                    0.6f to MaterialTheme.colorScheme.background,
                  ),
              ),
            ),
      )
    }

    Scaffold(
      containerColor = Color.Transparent,
      topBar = {
        TopAppBar(
          modifier = Modifier.statusBarsPadding(),
          colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
          actions = {
            if (playingQueueExpanded) {
              AnimatedContent(
                targetState = searchRequested,
                label = "library_action_animation",
                transitionSpec = {
                  fadeIn(animationSpec = keyframes { durationMillis = 150 }) togetherWith
                    fadeOut(animationSpec = keyframes { durationMillis = 150 })
                },
              ) { isSearchRequested ->
                when (isSearchRequested) {
                  true ->
                    ChapterSearchActionComposable(
                      onSearchRequested = { playerViewModel.updateSearch(it) },
                    )

                  false ->
                    Row {
                      IconButton(
                        onClick = { playerViewModel.requestSearch() },
                        modifier = Modifier.padding(end = 4.dp),
                      ) {
                        Icon(
                          imageVector = Search,
                          contentDescription = null,
                        )
                      }
                    }
                }
              }
            } else {
              Row {
                IconButton(
                  onClick = { itemDetailsSelected = true },
                  modifier = Modifier.padding(end = 4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                  )
                }
              }
            }
          },
          title = {
            Text(
              text = screenTitle,
              style = titleTextStyle,
              color = colorScheme.onSurface,
              maxLines = 1,
              modifier = Modifier.fillMaxWidth(),
            )
          },
          navigationIcon = {
            IconButton(onClick = { stepBack() }) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = colorScheme.onSurface,
              )
            }
          },
        )
      },
      bottomBar = {
        playingBook?.let {
          NavigationBarComposable(
            book = it,
            playerViewModel = playerViewModel,
            contentCachingModelView = cachingModelView,
            settingsViewModel = settingsViewModel,
            onDownloadsClick = { downloadsExpanded = true },
            navController = navController,
            libraryType = libraryViewModel.fetchPreferredLibraryType(),
          )
        }
      },
      modifier = Modifier.fillMaxSize(),
      content = { innerPadding ->
        Column(
          modifier =
            Modifier
              .testTag("playerScreen")
              .padding(innerPadding),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          AnimatedVisibility(
            visible = playingQueueExpanded.not(),
            enter = expandVertically(animationSpec = tween(400)),
            exit = shrinkVertically(animationSpec = tween(400)),
            modifier = Modifier.weight(1f),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Spacer(modifier = Modifier.weight(0.5f))

              if (!isPlaybackReady) {
                TrackDetailsPlaceholderComposable(bookTitle, bookSubtitle)
              } else {
                TrackDetailsComposable(
                  viewModel = playerViewModel,
                  imageLoader = imageLoader,
                  libraryViewModel = libraryViewModel,
                  cachingModelView = cachingModelView,
                  onTitleClick = { itemDetailsSelected = true },
                )
              }

              Spacer(modifier = Modifier.weight(1f))

              if (!isPlaybackReady) {
                TrackControlPlaceholderComposable(
                  modifier = Modifier,
                  settingsViewModel = settingsViewModel,
                )
              } else {
                TrackProgressComposable(
                  viewModel = playerViewModel,
                  libraryType = libraryViewModel.fetchPreferredLibraryType(),
                  onChaptersClick = { showChaptersList = true },
                )
              }

              Spacer(modifier = Modifier.weight(1.1f))

              if (isPlaybackReady) {
                PlaybackButtonsComposable(
                  viewModel = playerViewModel,
                  settingsViewModel = settingsViewModel,
                )
              }

              Spacer(modifier = Modifier.weight(1.1f))
            }
          }

          if (showChaptersList) {
            playingBook?.let {
              ChaptersBottomSheet(
                book = it,
                currentPosition = playerViewModel.totalPosition.value ?: 0.0,
                currentChapterIndex = playerViewModel.currentChapterIndex.value ?: 0,
                isOnline = isOnline,
                cachingModelView = cachingModelView,
                onChapterSelected = { chapter: org.grakovne.lissen.lib.domain.PlayingChapter ->
                  val currentChapterIndex = playerViewModel.currentChapterIndex.value
                  val index = it.chapters.indexOf(chapter)

                  if (index == currentChapterIndex) {
                    playerViewModel.togglePlayPause()
                  } else {
                    playerViewModel.setChapter(chapter)
                  }
                  showChaptersList = false
                },
                onDismissRequest = { showChaptersList = false },
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          when {
            isPlaybackReady.not() -> {
              PlayingQueuePlaceholderComposable(
                libraryViewModel = libraryViewModel,
                modifier = Modifier,
              )
            }

            playingBook?.chapters.isNullOrEmpty() -> {
              PlayingQueueFallbackComposable(
                libraryViewModel = libraryViewModel,
                modifier = Modifier,
              )
            }

            else -> {
              PlayingQueueComposable(
                libraryViewModel = libraryViewModel,
                cachingModelView = cachingModelView,
                viewModel = playerViewModel,
                modifier = Modifier,
              )
            }
          }
        }
      },
    )

    if (itemDetailsSelected) {
      MediaDetailComposable(
        playingBook = playingBook,
        playingViewModel = playerViewModel,
        settingsViewModel = settingsViewModel,
        onDismissRequest = { itemDetailsSelected = false },
      )
    }

    if (downloadsExpanded) {
      val cacheProgress by cachingModelView
        .getProgress(
          playingBook?.id.orEmpty(),
        ).collectAsState(
          initial =
            org.grakovne.lissen.content.cache.persistent
              .CacheState(org.grakovne.lissen.lib.domain.CacheStatus.Idle),
        )
      val hasDownloadedChapters by cachingModelView.hasDownloadedChapters(playingBook?.id.orEmpty()).observeAsState(false)

      playingBook?.let { book ->
        DownloadsComposable(
          book = book,
          storageType = cachingModelView.getBookStorageType(book),
          volumes = cachingModelView.getVolumes(book),
          isOnline = isOnline,
          cachingInProgress = cacheProgress.status is org.grakovne.lissen.lib.domain.CacheStatus.Caching,
          onRequestedDownload = { option ->
            cachingModelView.cache(
              mediaItem = book,
              option = option,
            )
          },
          onRequestedDrop = {
            scope.launch {
              cachingModelView.dropCache(book.id)
            }
          },
          onRequestedDropCompleted = {
            scope.launch {
              cachingModelView.dropCompletedChapters(book)
            }
          },
          onRequestedStop = {
            scope.launch {
              cachingModelView.stopCaching(book)
            }
          },
          onDismissRequest = { downloadsExpanded = false },
        )
      }
    }
  }
}

@Composable
fun InfoRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  textValue: String,
) {
  Spacer(modifier = Modifier.height(8.dp))

  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = colorScheme.primary,
      modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(8.dp))
    Text(
      text = "$label: ",
      style = typography.bodyMedium,
      color = colorScheme.onSurface.copy(alpha = 0.6f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = textValue,
      style = typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private fun playingItemChanged(
  item: String,
  playingBook: DetailedItem?,
) = item != playingBook?.id

private fun cachePolicyChanged(
  cachingModelView: CachingModelView,
  playingBook: DetailedItem?,
) = cachingModelView.localCacheUsing() != playingBook?.localProvided
