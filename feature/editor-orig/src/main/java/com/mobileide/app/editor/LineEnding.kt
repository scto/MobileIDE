package com.mobileide.app.editor

import io.github.rosemoe.sora.text.LineSeparator

/** Maps a human-readable line-ending name to Sora's [LineSeparator]. */
enum class LineEnding(val type: LineSeparator) {
    LF(LineSeparator.LF),
    CRLF(LineSeparator.CRLF),
    CR(LineSeparator.CR);

    companion object {
        fun fromValue(value: String?): LineEnding =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LF
    }
}
