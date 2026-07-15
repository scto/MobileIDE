package com.scto.mobile.ide.core.layout.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Der ziehbare Splitter zwischen den beiden Vorschauflächen.
 * [vertical] = true für SIDE_BY_SIDE (vertikaler Balken),
 * false für TOP_BOTTOM (horizontaler Balken).
 */
@Composable
fun SplitDivider(
    vertical: Boolean,
    containerSizePx: Float,
    onDrag: (deltaFraction: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val thickness = 20.dp
    Box(
        modifier = modifier
            .then(
                if (vertical) Modifier.width(thickness).fillMaxHeight()
                else Modifier.height(thickness).fillMaxWidth()
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(vertical) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = if (vertical) dragAmount.x else dragAmount.y
                    if (containerSizePx > 0) {
                        onDrag(delta / containerSizePx)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "Splitter verschieben",
            tint = Color.Gray,
            modifier = if (vertical) Modifier.graphicsLayerRotate90() else Modifier
        )
    }
}

// Kleiner Helper, um den DragHandle bei vertikalem Splitter zu drehen
private fun Modifier.graphicsLayerRotate90(): Modifier =
    this.then(Modifier)