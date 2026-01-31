package org.grakovne.lissen.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlowIcon(
  imageVector: ImageVector,
  contentDescription: String?,
  tint: Color,
  glowColor: Color,
  glowRadius: Dp,
  modifier: Modifier = Modifier,
) {
  Box(contentAlignment = Alignment.Center) {
    // Glow Layer (Background)
    // Render the icon with blur.
    // Note: We duplicate the modifier to match size/layout, then add blur.
    Icon(
      imageVector = imageVector,
      contentDescription = null,
      tint = glowColor,
      modifier = modifier.blur(glowRadius),
    )

    // Sharp Layer (Foreground)
    Icon(
      imageVector = imageVector,
      contentDescription = contentDescription,
      tint = tint,
      modifier = modifier,
    )
  }
}
