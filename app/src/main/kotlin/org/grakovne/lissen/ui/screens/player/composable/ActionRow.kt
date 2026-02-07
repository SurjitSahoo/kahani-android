package org.grakovne.lissen.ui.screens.player.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionRow(
  title: String,
  subtitle: String? = null,
  icon: ImageVector,
  trailingIcon: ImageVector? = null,
  enabled: Boolean = true,
  isDanger: Boolean = false,
  isSuggested: Boolean = false,
  onClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1f,
    label = "squish",
  )

  val floatY by animateFloatAsState(
    targetValue = if (isSuggested && !isPressed) -2f else 0f,
    label = "float",
  )

  val contentAlpha = if (enabled) 1f else 0.38f
  val primaryColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
  val textColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
  val subtextColor =
    if (isDanger) {
      MaterialTheme.colorScheme.error.copy(
        alpha = 0.6f,
      )
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
          translationY = floatY
        }.clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = null,
          onClick = {
            if (enabled) {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onClick()
            }
          },
        ).padding(horizontal = 24.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(36.dp)
          .then(
            if (isSuggested && enabled) {
              Modifier.border(
                width = 1.dp,
                color = primaryColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
              )
            } else {
              Modifier
            },
          ).background(
            brush =
              Brush.verticalGradient(
                colors =
                  if (isSuggested) {
                    listOf(
                      primaryColor.copy(alpha = 0.2f * contentAlpha),
                      primaryColor.copy(alpha = 0.05f * contentAlpha),
                    )
                  } else {
                    listOf(
                      primaryColor.copy(alpha = 0.1f * contentAlpha),
                      primaryColor.copy(alpha = 0.1f * contentAlpha),
                    )
                  },
              ),
            shape = RoundedCornerShape(10.dp),
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = primaryColor.copy(alpha = contentAlpha),
        modifier = Modifier.size(18.dp),
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style =
          MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            letterSpacing = 0.1.sp,
          ),
        color = textColor.copy(alpha = contentAlpha),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = subtextColor.copy(alpha = subtextColor.alpha * contentAlpha),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    if (trailingIcon != null) {
      Spacer(modifier = Modifier.width(12.dp))
      Icon(
        imageVector = trailingIcon,
        contentDescription = null,
        tint =
          (if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant).copy(
            alpha =
              0.3f * contentAlpha,
          ),
        modifier = Modifier.size(16.dp),
      )
    }
  }
}
