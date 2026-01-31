package org.grakovne.lissen.content.cache.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.hoko.blur.HokoBlur
import com.hoko.blur.HokoBlur.MODE_GAUSSIAN
import com.hoko.blur.HokoBlur.SCHEME_NATIVE
import okio.Buffer
import okio.BufferedSource

fun Buffer.withBlur(
  context: Context,
  width: Int? = null,
): Buffer {
  val dimensions = getImageDimensions(this)
  val isSquare = dimensions != null && dimensions.first == dimensions.second && dimensions.first > 0

  return if (isSquare && width == null) {
    this
  } else {
    runCatching { sourceWithBackdropBlur(this, context, width) }.getOrElse { this }
  }
}

fun BufferedSource.withBlur(
  context: Context,
  width: Int? = null,
): Buffer {
  val buffer = Buffer()
  this.readAll(buffer)
  return buffer.withBlur(context, width)
}

private fun sourceWithBackdropBlur(
  source: BufferedSource,
  context: Context,
  targetWidth: Int? = null,
): Buffer {
  val peeked = source.peek()
  val original = BitmapFactory.decodeStream(peeked.inputStream()) ?: return Buffer().apply { source.readAll(this) }

  val srcWidth = original.width
  val srcHeight = original.height
  val maxDim = maxOf(srcWidth, srcHeight)

  val size = targetWidth ?: maxDim
  val scaleFactor = size.toFloat() / maxDim

  val blurSize = 100
  val blurScaled = original.scale(blurSize, blurSize)

  val blurredBackground =
    HokoBlur
      .with(context)
      .scheme(SCHEME_NATIVE)
      .mode(MODE_GAUSSIAN)
      .radius(10)
      .forceCopy(true)
      .blur(blurScaled)

  val result = createBitmap(size, size, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(result)

  val backgroundScaled = blurredBackground.scale(size, size)
  canvas.drawBitmap(backgroundScaled, 0f, 0f, null)

  val finalWidth = (srcWidth * scaleFactor).toInt()
  val finalHeight = (srcHeight * scaleFactor).toInt()
  val centeredHero =
    if (finalWidth != srcWidth || finalHeight != srcHeight) {
      original.scale(finalWidth, finalHeight)
    } else {
      original
    }

  val left = (size - finalWidth) / 2f
  val top = (size - finalHeight) / 2f

  canvas.drawBitmap(centeredHero, left, top, null)

  // Cleanup
  blurScaled.recycle()
  blurredBackground.recycle()
  backgroundScaled.recycle()
  if (centeredHero != original) centeredHero.recycle()
  original.recycle()

  return Buffer().apply {
    result.compress(Bitmap.CompressFormat.JPEG, 100, this.outputStream())
    result.recycle()
  }
}
