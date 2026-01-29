package org.grakovne.lissen.ui.screens.settings.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.grakovne.lissen.R
import org.grakovne.lissen.ui.theme.Spacing
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateComposable() {
  var showBottomSheet by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState()
  val uriHandler = LocalUriHandler.current

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable { showBottomSheet = true }
        .padding(horizontal = Spacing.md, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Filled.Favorite,
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = Color(0xFFE53935),
    )
    Spacer(modifier = Modifier.width(Spacing.md))
    Column(
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text = stringResource(R.string.donate_title),
        style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(bottom = 4.dp),
      )
      Text(
        text = stringResource(R.string.donate_subtitle),
        style = typography.bodyMedium,
        color = colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }

  if (showBottomSheet) {
    ModalBottomSheet(
      onDismissRequest = { showBottomSheet = false },
      sheetState = sheetState,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.donate_sheet_title),
          style = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          modifier = Modifier.padding(bottom = 24.dp),
        )

        // Ko-fi Button
        Button(
          onClick = {
            try {
              uriHandler.openUri("https://ko-fi.com/surjitsahoo")
            } catch (ex: Exception) {
              Timber.d("Unable to open Ko-fi link: ${ex.message}")
            }
          },
          modifier =
            Modifier
              .fillMaxWidth()
              .height(56.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = colorScheme.primaryContainer,
              contentColor = colorScheme.onPrimaryContainer,
            ),
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_kofi),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = stringResource(R.string.donate_kofi_button),
            style = typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // UPI Button
        Button(
          onClick = {
            try {
              uriHandler.openUri("upi://pay?pa=surjitsahoo0@ybl&pn=Surjit%20Kumar%20Sahoo&cu=INR")
            } catch (ex: Exception) {
              Timber.d("Unable to open UPI link: ${ex.message}")
            }
          },
          modifier =
            Modifier
              .fillMaxWidth()
              .height(56.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = colorScheme.secondaryContainer,
              contentColor = colorScheme.onSecondaryContainer,
            ),
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_upi),
            contentDescription = null,
            modifier = Modifier.height(28.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = stringResource(R.string.donate_upi_button),
            style = typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
          )
        }
      }
    }
  }
}
