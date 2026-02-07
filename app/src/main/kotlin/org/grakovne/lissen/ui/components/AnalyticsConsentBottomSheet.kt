package org.grakovne.lissen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.grakovne.lissen.R
import org.grakovne.lissen.ui.theme.FoxOrange
import org.grakovne.lissen.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsConsentBottomSheet(
  onAccept: () -> Unit,
  onDecline: () -> Unit,
) {
  val sheetState =
    rememberModalBottomSheetState(
      skipPartiallyExpanded = true,
      confirmValueChange = { it != SheetValue.Hidden },
    )

  ModalBottomSheet(
    onDismissRequest = { /* User must make a choice */ },
    sheetState = sheetState,
    containerColor = colorScheme.background,
    dragHandle = null,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 32.dp)
          .padding(top = 48.dp, bottom = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.analytics_consent_title),
        style =
          typography.headlineSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            lineHeight = 28.sp,
            fontSize = 22.sp,
          ),
        textAlign = TextAlign.Center,
        color = colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(Spacing.md))

      Text(
        text = stringResource(R.string.analytics_consent_message),
        style =
          typography.bodyLarge.copy(
            lineHeight = 24.sp,
            letterSpacing = 0.2.sp,
          ),
        textAlign = TextAlign.Center,
        color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
      )

      Spacer(modifier = Modifier.height(48.dp))

      Button(
        onClick = onAccept,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = FoxOrange,
            contentColor = Color.White,
          ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
      ) {
        Text(
          text = stringResource(R.string.analytics_consent_accept),
          style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      }

      // Increased gap to prevent accidental clicks
      Spacer(modifier = Modifier.height(Spacing.md))

      TextButton(
        onClick = onDecline,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
              color = colorScheme.onSurface.copy(alpha = 0.05f),
              shape = CircleShape,
            ),
        shape = CircleShape,
      ) {
        Text(
          text = stringResource(R.string.analytics_consent_decline),
          style =
            typography.bodyLarge.copy(
              fontWeight = FontWeight.SemiBold,
              color = colorScheme.onSurfaceVariant,
            ),
        )
      }
    }
  }
}
