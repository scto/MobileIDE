package com.scto.mobile.ide.integration





import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.scto.mobile.ide.file.FileObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob











/**
 * Singleton Adapter mapping Xed's MainActivity/MainViewModel access to MobileIDE components.
 */
object XedHost {
    var context: Context? = null
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val snackbarHostState = SnackbarHostState()

    fun openFile(file: FileObject) {
        // Integration point for EditorViewModel / CodeEditScreen
    }

    fun jumpToPosition(file: FileObject, line: Int, column: Int) {
        // Integration point for navigation
    }

    fun saveAll() {
        // Integration point for saving active buffers
    }

    fun toast(message: String) {
        // Integration point for toasts
    }

    fun errorDialog(title: String, message: String) {
        // Integration point for error dialogs
    }
}
