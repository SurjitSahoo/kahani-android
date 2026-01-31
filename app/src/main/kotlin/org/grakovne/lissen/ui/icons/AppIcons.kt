package org.grakovne.lissen.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom User-Provided Lucide Icons.
 * perfectly converted from source SVGs (Stroke Width 2).
 */
object AppIcons {
  // Lucide: skip-back
  val SkipPrevious: ImageVector
    get() =
      ImageVector
        .Builder(
          name = "SkipPrevious",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        ).apply {
          // Path 1: Triangle/Arrow
          path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
          ) {
            moveTo(17.971f, 4.285f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 6f)
            verticalLineTo(18f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.971f, 19.715f)
            lineTo(7.974f, 13.717f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7.971f, 10.285f)
            close()
          }
          // Path 2: Vertical Line
          path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
          ) {
            moveTo(3f, 20f)
            verticalLineTo(4f)
          }
        }.build()

  // Lucide: skip-forward
  val SkipNext: ImageVector
    get() =
      ImageVector
        .Builder(
          name = "SkipNext",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        ).apply {
          // Path 1: Vertical Line
          path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
          ) {
            moveTo(21f, 4f)
            verticalLineTo(20f)
          }
          // Path 2: Triangle/Arrow
          path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
          ) {
            moveTo(6.029f, 4.285f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3f, 6f)
            verticalLineTo(18f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6.029f, 19.715f)
            lineTo(16.026f, 13.717f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16.029f, 10.285f)
            close()
          }
        }.build()

  // Lucide: rotate-ccw (Seek Prev)
  val SeekBack: ImageVector
    get() =
      ImageVector
        .Builder(
          name = "SeekBack",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        ).apply {
          path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
          ) {
            moveTo(3f, 12f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = false, 12f, 3f)
            arcTo(9.75f, 9.75f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5.26f, 5.74f)
            lineTo(3f, 8f)
          }
          path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
          ) {
            moveTo(3f, 3f)
            verticalLineTo(8f)
            horizontalLineTo(8f)
          }
        }.build()

  // Lucide: rotate-cw (Seek Next)
  val SeekForward: ImageVector
    get() =
      ImageVector
        .Builder(
          name = "SeekForward",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        ).apply {
          path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
          ) {
            moveTo(21f, 12f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 3f)
            curveTo(14.52f, 3f, 16.93f, 4f, 18.74f, 5.74f)
            lineTo(21f, 8f)
          }
          path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
          ) {
            moveTo(21f, 3f)
            verticalLineTo(8f)
            horizontalLineTo(16f)
          }
        }.build()

  // Lucide: play (Filled)
  val PlayFilled: ImageVector
    get() =
      ImageVector
        .Builder(
          name = "PlayFilled",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        ).apply {
          path(
            fill = SolidColor(Color.White),
          ) {
            moveTo(5f, 5f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8.008f, 3.272f)
            lineTo(20.005f, 10.27f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20.008f, 13.728f)
            lineTo(8.008f, 20.728f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 19f)
            close()
          }
        }.build()
}
