package org.grakovne.lissen.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import kotlinx.coroutines.launch
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.ui.navigation.AppNavigationService
import org.grakovne.lissen.ui.screens.library.composables.MiniPlayerComposable
import org.grakovne.lissen.ui.screens.player.composable.ChaptersBottomSheet
import org.grakovne.lissen.ui.screens.player.composable.DownloadsComposable
import org.grakovne.lissen.ui.screens.player.composable.NavigationBarComposable
import org.grakovne.lissen.ui.screens.player.composable.PlaybackButtonsComposable
import org.grakovne.lissen.ui.screens.player.composable.TrackDetailsComposable
import org.grakovne.lissen.ui.screens.player.composable.TrackProgressComposable
import org.grakovne.lissen.ui.screens.player.composable.placeholder.TrackControlPlaceholderComposable
import org.grakovne.lissen.ui.screens.player.composable.placeholder.TrackDetailsPlaceholderComposable
import org.grakovne.lissen.viewmodel.CachingModelView
import org.grakovne.lissen.viewmodel.LibraryViewModel
import org.grakovne.lissen.viewmodel.PlayerViewModel
import org.grakovne.lissen.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalPlayerBottomSheet(
  navController: AppNavigationService,
  imageLoader: ImageLoader,
  content: @Composable () -> Unit,
) {
  val playerViewModel: PlayerViewModel = hiltViewModel()
  val libraryViewModel: LibraryViewModel = hiltViewModel()
  val settingsViewModel: SettingsViewModel = hiltViewModel()
  val cachingModelView: CachingModelView = hiltViewModel()

  val playingBook by playerViewModel.book.observeAsState()
  val isPlaybackReady by playerViewModel.isPlaybackReady.observeAsState(false)

  var showBottomSheet by remember { mutableStateOf(false) }

  // Container
  Box(modifier = Modifier.fillMaxSize()) {
    // Main App Content
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(bottom = if (playingBook != null && !showBottomSheet) 66.dp else 0.dp),
    ) {
      content()
    }

    // Mini Player (Bottom Bar)
    // Show only if we have a book and the sheet is NOT open
    AnimatedVisibility(
      visible = playingBook != null && !showBottomSheet,
      enter = slideInVertically { it } + expandVertically(),
      exit = slideOutVertically { it } + shrinkVertically(),
      modifier = Modifier.align(Alignment.BottomCenter),
    ) {
      playingBook?.let { book ->
        Surface(
          shadowElevation = 0.dp,
          tonalElevation = 0.dp,
          color = androidx.compose.ui.graphics.Color.Transparent,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.navigationBarsPadding()) {
            GlobalMiniPlayer(
              book = book,
              imageLoader = imageLoader,
              playerViewModel = playerViewModel,
              onOpenPlayer = { showBottomSheet = true },
            )
          }
        }
      }
    }

    // Full Player Overlay (Replaces ModalBottomSheet for true edge-to-edge)
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(showBottomSheet) {
      if (showBottomSheet) {
        offsetY.snapTo(0f)
      }
    }

    AnimatedVisibility(
      visible = showBottomSheet,
      enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)),
      exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)),
      modifier = Modifier.fillMaxSize(),
    ) {
      BackHandler(enabled = true) {
        showBottomSheet = false
      }

      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .pointerInput(Unit) {
              detectVerticalDragGestures(
                onDragEnd = {
                  scope.launch {
                    if (offsetY.value > 300f) {
                      showBottomSheet = false
                    } else {
                      offsetY.animateTo(0f)
                    }
                  }
                },
                onVerticalDrag = { change, dragAmount ->
                  change.consume()
                  scope.launch {
                    offsetY.snapTo((offsetY.value + dragAmount).coerceAtLeast(0f))
                  }
                },
              )
            },
      ) {
        PlayerContent(
          navController = navController,
          playerViewModel = playerViewModel,
          libraryViewModel = libraryViewModel,
          settingsViewModel = settingsViewModel,
          cachingModelView = cachingModelView,
          imageLoader = imageLoader,
          onCollapse = {
            showBottomSheet = false
          },
        )
      }
    }
  }
}

@Composable
fun GlobalMiniPlayer(
  book: DetailedItem,
  imageLoader: ImageLoader,
  playerViewModel: PlayerViewModel,
  onOpenPlayer: () -> Unit,
) {
  // We pass a dummy NavController or null since we use onContentClick
  // However, MiniPlayerComposable still requires a AppNavigationService in the signature.
  // We can construct a dummy one safely because it won't be used for the click action.
  val context = androidx.compose.ui.platform.LocalContext.current

  org.grakovne.lissen.ui.screens.library.composables.MiniPlayerComposable(
    book = book,
    imageLoader = imageLoader,
    playerViewModel = playerViewModel,
    onContentClick = onOpenPlayer,
  )
}

@Composable
fun PlayerContent(
  navController: AppNavigationService,
  playerViewModel: PlayerViewModel,
  libraryViewModel: LibraryViewModel,
  settingsViewModel: SettingsViewModel,
  cachingModelView: CachingModelView,
  imageLoader: ImageLoader,
  onCollapse: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val playingBook by playerViewModel.book.observeAsState()
  val isPlaybackReady by playerViewModel.isPlaybackReady.observeAsState(false)
  val playingQueueExpanded by playerViewModel.playingQueueExpanded.observeAsState(false)
  var backgroundLoaded by remember { mutableStateOf(false) }

  val contentAlpha by androidx.compose.animation.core.animateFloatAsState(
    targetValue = if (backgroundLoaded) 1f else 0f,
    animationSpec = tween(600),
    label = "player_content_fade",
  )

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
  ) {
    // Dynamic blurred background from book cover
    Box(modifier = Modifier.fillMaxSize()) {
      playingBook?.let { book ->
        val context = LocalContext.current
        val imageRequest =
          remember(book.id) {
            ImageRequest
              .Builder(context)
              .data(book.id)
              .size(Size.ORIGINAL)
              .build()
          }

        val blurModifier =
          if (android.os.Build.VERSION.SDK_INT >= 31) {
            Modifier.blur(radius = 80.dp)
          } else {
            Modifier
          }

        AsyncImage(
          model = imageRequest,
          imageLoader = imageLoader,
          onSuccess = { backgroundLoaded = true },
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
                      0.65f to MaterialTheme.colorScheme.background,
                    ),
                ),
              ),
        )
      }
    }

    // We control the chapters sheet visibility
    var showChaptersList by remember { mutableStateOf(false) }
    var downloadsExpanded by remember { mutableStateOf(false) }

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .alpha(contentAlpha),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Drag Handle / Chevron
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(32.dp)
            .clickable(onClick = onCollapse),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          imageVector = Icons.Rounded.KeyboardArrowDown,
          contentDescription = "Close",
          colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)),
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.size(width = 64.dp, height = 32.dp),
        )
      }

      Spacer(modifier = Modifier.weight(0.5f))

      // Track Details
      AnimatedVisibility(
        visible = playingQueueExpanded.not(),
        enter = expandVertically(animationSpec = tween(400)),
        exit = shrinkVertically(animationSpec = tween(400)),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 16.dp),
        ) {
          if (!isPlaybackReady) {
            TrackDetailsPlaceholderComposable("Loading...", null)
          } else {
            TrackDetailsComposable(
              viewModel = playerViewModel,
              imageLoader = imageLoader,
              libraryViewModel = libraryViewModel,
              cachingModelView = cachingModelView,
              onTitleClick = {
                playingBook?.let { book ->
                  navController.showPlayer(book.id, book.title, book.subtitle, false)
                  onCollapse()
                }
              },
              onChapterClick = { showChaptersList = true },
            )
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // Track Progress (Chapter/Slider)
      AnimatedVisibility(
        visible = playingQueueExpanded.not(),
        enter = expandVertically(animationSpec = tween(400)),
        exit = shrinkVertically(animationSpec = tween(400)),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 16.dp),
        ) {
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
        }
      }

      Spacer(modifier = Modifier.weight(1.1f))

      // Playback Buttons
      AnimatedVisibility(
        visible = playingQueueExpanded.not(),
        enter = expandVertically(animationSpec = tween(400)),
        exit = shrinkVertically(animationSpec = tween(400)),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 16.dp),
        ) {
          PlaybackButtonsComposable(
            viewModel = playerViewModel,
            settingsViewModel = settingsViewModel,
          )
        }
      }

      // Final Gap to Navbar
      Spacer(modifier = Modifier.weight(1.1f))

      if (playingBook != null && isPlaybackReady) {
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

          if (showChaptersList) {
            val isOnline by playerViewModel.isOnline.collectAsState(initial = false)

            ChaptersBottomSheet(
              book = it,
              currentPosition = playerViewModel.totalPosition.value ?: 0.0,
              currentChapterIndex = playerViewModel.currentChapterIndex.value ?: 0,
              isOnline = isOnline,
              cachingModelView = cachingModelView,
              onChapterSelected = { chapter ->
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

          if (downloadsExpanded) {
            val isOnline by playerViewModel.isOnline.collectAsState(initial = false)
            val cacheProgress by cachingModelView
              .getProgress(
                it.id,
              ).collectAsState(
                initial =
                  org.grakovne.lissen.content.cache.persistent
                    .CacheState(org.grakovne.lissen.lib.domain.CacheStatus.Idle),
              )

            it.let { book ->
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
    }
  }
}
