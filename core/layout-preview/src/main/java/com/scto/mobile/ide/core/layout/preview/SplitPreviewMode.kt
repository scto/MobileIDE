package com.scto.mobile.ide.core.layout.preview

/** Anzeigemodus der geteilten Vorschau */
enum class SplitPreviewMode {
    SIDE_BY_SIDE,   // links/rechts
    TOP_BOTTOM,     // oben/unten
    OVERLAY,        // übereinander mit Deckkraft-Schieber
    SWIPE_COMPARE,  // Vorher/Nachher mit Wischgrenze (wie Bild-Diff)
    DIFF            // ★ NEU: Screenshot-Pixel-Diff
}
