package com.scto.mobile.ide.core.layout.preview

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

/**
 * Zentraler State für die Vorschau: Splitter-Position, Zoom, Pan,
 * aktueller Modus und Sync-Scroll-Flag.
 */
@Stable
class SplitPreviewState(
    initialMode: SplitPreviewMode = SplitPreviewMode.SIDE_BY_SIDE,
    initialSplitFraction: Float = 0.5f
) {
    var mode by mutableStateOf(initialMode)
    var splitFraction by mutableFloatStateOf(initialSplitFraction) // 0f..1f
    var overlayAlpha by mutableFloatStateOf(0.5f)
    var syncScroll by mutableStateOf(true)
    var syncZoom by mutableStateOf(true)

    // Zoom/Pan für Compose-Seite
    var scaleCompose by mutableFloatStateOf(1f)
    var offsetCompose by mutableStateOf(Offset.Zero)

    // Zoom/Pan für XML-Seite
    var scaleXml by mutableFloatStateOf(1f)
    var offsetXml by mutableStateOf(Offset.Zero)

    fun updateSplit(fraction: Float) {
        splitFraction = fraction.coerceIn(0.05f, 0.95f)
    }

    /** Wendet Zoom/Pan synchron auf beide Seiten an, falls syncZoom aktiv */
    fun applyZoomPan(scale: Float, offset: Offset, fromCompose: Boolean) {
        if (fromCompose) {
            scaleCompose = scale
            offsetCompose = offset
            if (syncZoom) {
                scaleXml = scale
                offsetXml = offset
            }
        } else {
            scaleXml = scale
            offsetXml = offset
            if (syncZoom) {
                scaleCompose = scale
                offsetCompose = offset
            }
        }
    }

    fun resetZoom() {
        scaleCompose = 1f; scaleXml = 1f
        offsetCompose = Offset.Zero; offsetXml = Offset.Zero
    }
}

@Composable
fun rememberSplitPreviewState(
    initialMode: SplitPreviewMode = SplitPreviewMode.SIDE_BY_SIDE,
    initialSplitFraction: Float = 0.5f
): SplitPreviewState = remember { SplitPreviewState(initialMode, initialSplitFraction) }