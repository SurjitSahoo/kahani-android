package org.grakovne.lissen.ui.screens.settings.composable

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.grakovne.lissen.R
import org.grakovne.lissen.ui.theme.Spacing
import timber.log.Timber

private const val UPI_ID = "surjitsahoo0@ybl"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateComposable() {
  var showBottomSheet by remember { mutableStateOf(false) }
  var showFullscreenQr by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val uriHandler = LocalUriHandler.current
  val context = LocalContext.current

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

  // Fullscreen QR Dialog
  if (showFullscreenQr) {
    Dialog(
      onDismissRequest = { showFullscreenQr = false },
      properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable { showFullscreenQr = false }
            .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_upi_qr),
          contentDescription = stringResource(R.string.donate_upi_qr_description),
          modifier =
            Modifier
              .fillMaxWidth()
              .aspectRatio(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White)
              .padding(8.dp),
        )
      }
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
          modifier = Modifier.padding(bottom = 20.dp),
        )

        // UPI Logo
        Image(
          painter = painterResource(id = R.drawable.ic_upi_logo),
          contentDescription = "UPI",
          modifier = Modifier.height(32.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // QR Code (tappable for fullscreen)
        Image(
          painter = painterResource(id = R.drawable.ic_upi_qr),
          contentDescription = stringResource(R.string.donate_upi_qr_description),
          modifier =
            Modifier
              .size(200.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White)
              .clickable { showFullscreenQr = true }
              .padding(8.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = stringResource(R.string.donate_upi_scan_instruction),
          style = typography.bodySmall,
          color = colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Copy UPI ID Button
        Row(
          modifier =
            Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(colorScheme.secondaryContainer)
              .clickable {
                copyToClipboard(context, UPI_ID)
              }.padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Text(
            text = UPI_ID,
            style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = colorScheme.onSecondaryContainer,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = stringResource(R.string.donate_copy_upi_id),
            modifier = Modifier.size(18.dp),
            tint = colorScheme.onSecondaryContainer,
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        HorizontalDivider(
          modifier = Modifier.padding(horizontal = 32.dp),
          color = colorScheme.outlineVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

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
      }
    }
  }
}

private fun copyToClipboard(
  context: Context,
  text: String,
) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText("UPI ID", text)
  clipboard.setPrimaryClip(clip)
  Toast.makeText(context, R.string.donate_upi_copied, Toast.LENGTH_SHORT).show()
}
