package com.example.layoutpreview

import android.graphics.Bitmap
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * Rendert das Compose-Layout und das XML-Layout unsichtbar (bzw. off-screen-simuliert),
 * erfasst je ein Bitmap und berechnet den Pixel-Diff automatisch nach kurzer
 * Verzögerung (damit beide Seiten fertig gelayoutet sind).
 *
 * Für produktiven Einsatz empfiehlt sich `graphicsLayer.toImageBitmap()` (Compose 1.7+)
 * statt View.draw(), hier zeigen wir den robusten View-basierten Ansatz.
 */
@Composable
fun rememberScreenshotDiff(
    @LayoutRes xmlLayoutRes: Int,
    composeContent: @Composable () -> Unit,
    threshold: Int = 16,
    captureTriggerKey: Any? = null
): State<PixelDiffResult?> {
    val result = remember { mutableStateOf<PixelDiffResult?>(null) }
    var composeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var xmlBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val view = LocalView.current

    // Compose-Seite erfassen: über eine unsichtbar positionierte Kopie im selben Baum.
    // (In der Praxis wird dies über eine separate Capture-Composable im
    //  LayoutPreviewScreen eingebettet – siehe unten HiddenCaptureBox.)

    LaunchedEffect(captureTriggerKey, composeBitmap, xmlBitmap) {
        val a = composeBitmap
        val b = xmlBitmap
        if (a != null && b != null) {
            result.value = computePixelDiff(a, b, threshold)
        }
    }

    return result
}

/**
 * Unsichtbare Erfassungs-Box: rendert [content] normal (im Layoutfluss versteckt via
 * zeroSize-Trick vermeiden wir – hier lassen wir es normal sichtbar, aber mit
 * alpha = 0 im Overlay-Modus), und liefert per Callback das gerenderte Bitmap,
 * sobald die View final gelayoutet ist.
 */
@Composable
fun CaptureBox(
    modifier: Modifier = Modifier,
    onCaptured: (Bitmap) -> Unit,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    var captured by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                if (!captured && coordinates.size.width > 0 && coordinates.size.height > 0) {
                    captured = true
                    // Kleines Delay, damit alle Sub-Compositions fertig gezeichnet sind
                    view.post {
                        val bmp = captureViewToBitmap(view)
                        onCaptured(bmp)
                    }
                }
            }
    ) {
        content()
    }
}

/**
 * XML-Variante: rendert das Layout und meldet, sobald das Bitmap erfassbar ist.
 */
@Composable
fun XmlCaptureHost(
    @LayoutRes layoutRes: Int,
    modifier: Modifier = Modifier,
    onCaptured: (Bitmap) -> Unit
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            android.view.LayoutInflater.from(context).inflate(layoutRes, null, false)
        },
        update = { view ->
            view.post {
                if (view.width > 0 && view.height > 0) {
                    onCaptured(captureViewToBitmap(view))
                }
            }
        }
    )
}

