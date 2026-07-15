package com.scto.mobile.ide.core.layout.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.annotation.LayoutRes

/**
 * Hauptkomponente: zeigt Compose-Vorschau und XML-Vorschau
 * je nach [SplitPreviewState.mode] nebeneinander, übereinander,
 * als Overlay oder als Swipe-Vergleich.
 *
 * @param composeContent das zu prüfende Compose-Layout
 * @param xmlLayoutRes Resource-ID des XML-Layouts (z. B. R.layout.activity_main)
 */
@Composable
fun LayoutPreviewScreen(
    @LayoutRes xmlLayoutRes: Int,
    state: SplitPreviewState = rememberSplitPreviewState(),
    modifier: Modifier = Modifier,
    composeContent: @Composable () -> Unit
) {
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier.fillMaxSize()) {
        PreviewToolbar(state = state)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    containerWidthPx = it.size.width.toFloat()
                    containerHeightPx = it.size.height.toFloat()
                }
        ) {
            when (state.mode) {
                SplitPreviewMode.SIDE_BY_SIDE -> SideBySide(
                    state, containerWidthPx, composeContent, xmlLayoutRes
                )
                SplitPreviewMode.TOP_BOTTOM -> TopBottom(
                    state, containerHeightPx, composeContent, xmlLayoutRes
                )
                SplitPreviewMode.OVERLAY -> Overlay(
                    state, composeContent, xmlLayoutRes
                )
                SplitPreviewMode.SWIPE_COMPARE -> SwipeCompare(
                    state, containerWidthPx, composeContent, xmlLayoutRes
                )
            }
        }
    }
}

/* ---------- Modus-Implementierungen ---------- */

@Composable
private fun DiffMode(
    xmlLayoutRes: Int,
    threshold: Int,
    composeContent: @Composable () -> Unit
) {
    var composeBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var xmlBmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var diff by remember { mutableStateOf<PixelDiffResult?>(null) }

    LaunchedEffect(composeBmp, xmlBmp) {
        if (composeBmp != null && xmlBmp != null) {
            diff = computePixelDiff(composeBmp!!, xmlBmp!!, threshold)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Beide unsichtbar (alpha 0) im Hintergrund rendern für die Erfassung
        Box(Modifier.fillMaxSize().graphicsLayer(alpha = 0f)) {
            CaptureBox(onCaptured = { composeBmp = it }) { composeContent() }
        }
        Box(Modifier.fillMaxSize().graphicsLayer(alpha = 0f)) {
            XmlCaptureHost(xmlLayoutRes) { xmlBmp = it }
        }
        ScreenshotDiffOverlay(diffResult = diff, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SideBySide(
    state: SplitPreviewState,
    containerWidthPx: Float,
    composeContent: @Composable () -> Unit,
    @LayoutRes xmlLayoutRes: Int
) {
    Row(Modifier.fillMaxSize()) {
        LabeledPane(
            title = "Compose",
            modifier = Modifier.weight(state.splitFraction)
        ) {
            ZoomPanBox(
                scale = state.scaleCompose,
                offset = state.offsetCompose,
                onTransform = { s, o -> state.applyZoomPan(s, o, fromCompose = true) },
                modifier = Modifier.fillMaxSize()
            ) { composeContent() }
        }

        SplitDivider(
            vertical = true,
            containerSizePx = containerWidthPx,
            onDrag = { delta -> state.updateSplit(state.splitFraction + delta) }
        )

        LabeledPane(
            title = "XML",
            modifier = Modifier.weight(1f - state.splitFraction)
        ) {
            ZoomPanBox(
                scale = state.scaleXml,
                offset = state.offsetXml,
                onTransform = { s, o -> state.applyZoomPan(s, o, fromCompose = false) },
                modifier = Modifier.fillMaxSize()
            ) { XmlLayoutHost(xmlLayoutRes, Modifier.fillMaxSize()) }
        }
    }
}

@Composable
private fun TopBottom(
    state: SplitPreviewState,
    containerHeightPx: Float,
    composeContent: @Composable () -> Unit,
    @LayoutRes xmlLayoutRes: Int
) {
    Column(Modifier.fillMaxSize()) {
        LabeledPane(title = "Compose", modifier = Modifier.weight(state.splitFraction)) {
            ZoomPanBox(
                scale = state.scaleCompose,
                offset = state.offsetCompose,
                onTransform = { s, o -> state.applyZoomPan(s, o, fromCompose = true) },
                modifier = Modifier.fillMaxSize()
            ) { composeContent() }
        }

        SplitDivider(
            vertical = false,
            containerSizePx = containerHeightPx,
            onDrag = { delta -> state.updateSplit(state.splitFraction + delta) }
        )

        LabeledPane(title = "XML", modifier = Modifier.weight(1f - state.splitFraction)) {
            ZoomPanBox(
                scale = state.scaleXml,
                offset = state.offsetXml,
                onTransform = { s, o -> state.applyZoomPan(s, o, fromCompose = false) },
                modifier = Modifier.fillMaxSize()
            ) { XmlLayoutHost(xmlLayoutRes, Modifier.fillMaxSize()) }
        }
    }
}

@Composable
private fun Overlay(
    state: SplitPreviewState,
    composeContent: @Composable () -> Unit,
    @LayoutRes xmlLayoutRes: Int
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer(alpha = 1f)) { composeContent() }
        Box(Modifier.fillMaxSize().graphicsLayer(alpha = state.overlayAlpha)) {
            XmlLayoutHost(xmlLayoutRes, Modifier.fillMaxSize())
        }
        Slider(
            value = state.overlayAlpha,
            onValueChange = { state.overlayAlpha = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.8f)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun SwipeCompare(
    state: SplitPreviewState,
    containerWidthPx: Float,
    composeContent: @Composable () -> Unit,
    @LayoutRes xmlLayoutRes: Int
) {
    Box(Modifier.fillMaxSize()) {
        // Unten: XML komplett
        Box(Modifier.fillMaxSize()) { XmlLayoutHost(xmlLayoutRes, Modifier.fillMaxSize()) }

        // Oben: Compose, aber nur bis zur Splitgrenze sichtbar (geclippt)
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(state.splitFraction)
        ) { composeContent() }

        SplitDivider(
            vertical = true,
            containerSizePx = containerWidthPx,
            onDrag = { delta -> state.updateSplit(state.splitFraction + delta) },
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .padding(start = (containerWidthPxToDp(containerWidthPx * state.splitFraction)))
        )
    }
}

@Composable
private fun containerWidthPxToDp(px: Float): androidx.compose.ui.unit.Dp {
    val density = LocalDensityProvider()
    return with(density) { px.toDp() }
}

@Composable
private fun LocalDensityProvider() = androidx.compose.ui.platform.LocalDensity.current

@Composable
private fun LabeledPane(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Box(Modifier.weight(1f)) { content() }
    }
}

/* ---------- Toolbar ---------- */

@Composable
private fun PreviewToolbar(state: SplitPreviewState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconToggle(
            icon = Icons.Filled.ViewColumn,
            selected = state.mode == SplitPreviewMode.SIDE_BY_SIDE,
            desc = "Nebeneinander"
        ) { state.mode = SplitPreviewMode.SIDE_BY_SIDE }

        IconToggle(
            icon = Icons.Filled.ViewAgenda,
            selected = state.mode == SplitPreviewMode.TOP_BOTTOM,
            desc = "Übereinander"
        ) { state.mode = SplitPreviewMode.TOP_BOTTOM }

        IconToggle(
            icon = Icons.Filled.Layers,
            selected = state.mode == SplitPreviewMode.OVERLAY,
            desc = "Overlay"
        ) { state.mode = SplitPreviewMode.OVERLAY }

        IconToggle(
            icon = Icons.Filled.CompareArrows,
            selected = state.mode == SplitPreviewMode.SWIPE_COMPARE,
            desc = "Swipe-Vergleich"
        ) { state.mode = SplitPreviewMode.SWIPE_COMPARE }

        Spacer(Modifier.weight(1f))

        IconToggle(
            icon = Icons.Filled.SyncAlt,
            selected = state.syncScroll,
            desc = "Scroll-Sync"
        ) { state.syncScroll = !state.syncScroll }

        IconToggle(
            icon = Icons.Filled.ZoomOutMap,
            selected = state.syncZoom,
            desc = "Zoom-Sync"
        ) { state.syncZoom = !state.syncZoom }

        IconButton(onClick = { state.resetZoom() }) {
            Icon(Icons.Filled.RestartAlt, contentDescription = "Zoom zurücksetzen")
        }
    }
}

@Composable
private fun IconToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    desc: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = if (selected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurface
        )
    }
}
```