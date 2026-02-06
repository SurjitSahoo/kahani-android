package org.grakovne.lissen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.valentinilk.shimmer.shimmer

@Composable
fun AsyncShimmeringImage(
  imageRequest: ImageRequest,
  thumbnailRequest: ImageRequest? = null,
  imageLoader: ImageLoader,
  contentDescription: String,
  contentScale: ContentScale,
  modifier: Modifier = Modifier,
  error: Painter,
  onLoadingStateChanged: (Boolean) -> Unit = {},
) {
  var isMainLoading by remember { mutableStateOf(true) }
  var isThumbnailLoaded by remember { mutableStateOf(false) }

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    // Thumbnail / Preview layer
    if (thumbnailRequest != null && isMainLoading) {
      AsyncImage(
        model = thumbnailRequest,
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = contentScale,
        modifier =
          Modifier
            .fillMaxSize()
            .blur(if (isThumbnailLoaded) 20.dp else 0.dp),
        onSuccess = { isThumbnailLoaded = true },
      )
    }

    // Shimmer fallback if no thumbnail is loaded yet
    if (isMainLoading && !isThumbnailLoaded) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .shimmer()
            .background(MaterialTheme.colorScheme.surfaceVariant),
      )
    }

    // Main Image layer
    AsyncImage(
      model = imageRequest,
      imageLoader = imageLoader,
      contentDescription = contentDescription,
      contentScale = contentScale,
      modifier = Modifier.fillMaxSize(),
      onSuccess = {
        isMainLoading = false
        onLoadingStateChanged(false)
      },
      onError = {
        isMainLoading = false
        onLoadingStateChanged(false)
      },
      error = error,
    )

    // Center Loader (Instagram style)
    if (isMainLoading && isThumbnailLoaded) {
      CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        strokeWidth = 4.dp,
      )
    }
  }
}
