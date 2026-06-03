package io.github.rosemoe.sora.editor.ts

import java.util.concurrent.atomic.AtomicBoolean

internal object TreeSitterNativeLoader {
    private val loaded = AtomicBoolean(false)

    fun ensureLoaded() {
        if (loaded.compareAndSet(false, true)) {
            System.loadLibrary("android-tree-sitter")
        }
    }
}
