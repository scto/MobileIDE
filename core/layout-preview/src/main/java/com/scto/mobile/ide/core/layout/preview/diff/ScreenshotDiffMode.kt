package com.example.layoutpreview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.abs
import kotlin.math.max

/**
 * Ergebnis eines Bitmap-Vergleichs: Diff-Bild + Kennzahlen.
 */
data class PixelDiffResult(
    val diffBitmap: Bitmap,
    val totalPixels: Int,
    val differentPixels: Int
) {
    val differencePercent: Float
        get() = if (totalPixels == 0) 0f else (differentPixels.toFloat() / totalPixels) * 100f
}

/**
 * Vergleicht zwei gleich große Bitmaps pixelweise und erzeugt eine Heatmap.
 * Abweichende Pixel werden je nach Abweichungsstärke von Gelb (leicht) bis
 * Rot (stark) eingefärbt; identische Pixel bleiben transparent/schwarz.
 *
 * @param threshold Toleranz pro Farbkanal (0..255). Kleinere Werte = empfindlicher.
 */
fun computePixelDiff(
    bitmapA: Bitmap,
    bitmapB: Bitmap,
    threshold: Int = 16
): PixelDiffResult {
    val width = minOf(bitmapA.width, bitmapB.width)
    val height = minOf(bitmapA.height, bitmapB.height)

    val diffBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    var differentCount = 0

    val pixelsA = IntArray(width * height)
    val pixelsB = IntArray(width * height)
    bitmapA.getPixels(pixelsA, 0, width, 0, 0, width, height)
    bitmapB.getPixels(pixelsB, 0, width, 0, 0, width, height)

    val outPixels = IntArray(width * height)

    for (i in outPixels.indices) {
        val pA = pixelsA[i]
        val pB = pixelsB[i]

        val dR = abs(AndroidColor.red(pA) - AndroidColor.red(pB))
        val dG = abs(AndroidColor.green(pA) - AndroidColor.green(pB))
        val dB = abs(AndroidColor.blue(pA) - AndroidColor.blue(pB))
        val maxDelta = max(dR, max(dG, dB))

        outPixels[i] = if (maxDelta > threshold) {
            differentCount++
            // Heatmap: Intensität 0..255 -> Farbverlauf Gelb -> Rot
            val intensity = (maxDelta.coerceIn(0, 255))
            val alpha = 200
            val red = 255
            val green = (255 - intensity).coerceIn(0, 255)
            AndroidColor.argb(alpha, red, green, 0)
        } else {
            AndroidColor.TRANSPARENT
        }
    }

    diffBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)

    return PixelDiffResult(
        diffBitmap = diffBitmap,
        totalPixels = width * height,
        differentPixels = differentCount
    )
}

/**
 * Rendert eine beliebige [View] (auch AndroidView-Root von Compose-Snapshots
 * über eine ComposeView) in ein [Bitmap]. Funktioniert für bereits gelayoutete Views.
 */
fun captureViewToBitmap(view: View): Bitmap {
    val width = if (view.width > 0) view.width else view.measuredWidth
    val height = if (view.height > 0) view.height else view.measuredHeight
    val bitmap = Bitmap.createBitmap(
        max(width, 1), max(height, 1), Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}

/**
 * Composable UI-Baustein: zeigt das Diff-Ergebnis mit Heatmap-Overlay und
 * Kennzahlen-Badge (z. B. "3.2 % abweichend").
 */
@Composable
fun ScreenshotDiffOverlay(
    diffResult: PixelDiffResult?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        diffResult?.let { result ->
            Image(
                bitmap = result.diffBitmap.asImageBitmap(),
                contentDescription = "Pixel-Diff Heatmap",
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 4.dp
            ) {
                Text(
                    text = "Δ ${"%.2f".format(result.differencePercent)} % " +
                           "(${result.differentPixels}/${result.totalPixels} px)",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}