package org.grakovne.lissen.ui.extensions

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.iconGlow(
  color: Color,
  radius: Dp = 12.dp,
  alpha: Float = 0.4f,
): Modifier =
  this.shadow(
    elevation = radius,
    shape = CircleShape,
    spotColor = color.copy(alpha = alpha),
    ambientColor = color.copy(alpha = alpha),
  )
