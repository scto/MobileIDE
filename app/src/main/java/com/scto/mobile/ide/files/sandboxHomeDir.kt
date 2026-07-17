package com.scto.mobile.ide.files

import android.content.Context
import java.io.File

/**
 * Returns the sandbox home directory for the given [context].
 * It creates a subdirectory "sandbox" inside the app's private files directory if it does not exist.
 */
fun sandboxHomeDir(context: Context): File {
    val sandboxDir = File(context.filesDir, "sandbox")
    if (!sandboxDir.exists()) {
        sandboxDir.mkdirs()
    }
    return sandboxDir
}
