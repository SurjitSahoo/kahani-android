package org.grakovne.lissen.ui.screens.player.composable.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.grakovne.lissen.ui.components.GlowIcon
import org.grakovne.lissen.ui.icons.AppIcons

@Composable
fun SeekButton(
  duration: Int,
  isForward: Boolean,
  onClick: () -> Unit,
) {
  IconButton(
    onClick = onClick,
    modifier = Modifier.size(54.dp),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(54.dp),
    ) {
      GlowIcon(
        imageVector = if (isForward) AppIcons.SeekForward else AppIcons.SeekBack,
        contentDescription = null,
        tint = colorScheme.onSurface,
        glowColor = colorScheme.onSurface.copy(alpha = 0.3f),
        glowRadius = 4.dp,
        modifier = Modifier.size(38.dp),
      )

      Text(
        text = duration.toString(),
        fontSize = 11.sp,
        fontWeight = FontWeight.W400,
        color = colorScheme.onSurface,
        textAlign = TextAlign.Center,
        style =
          TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle =
              LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
              ),
          ),
      )
    }
  }
}
