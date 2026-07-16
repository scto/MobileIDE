package com.scto.mobile.ide.terminal

import android.content.Context
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.files.sandboxDir
import com.scto.mobile.ide.core.common.files.sandboxHomeDir
import com.scto.mobile.ide.utils.getTempDir
import com.scto.mobile.ide.utils.isMainThread
import java.io.File
import kotlinx.coroutines.CoroutineScope

enum class NEXT_STAGE {
    NONE,
    EXTRACTION,
}

suspend fun CoroutineScope.getNextStage(context: Context): NEXT_STAGE {
    if (isMainThread()) {
        throw RuntimeException("IO operation on the main thread")
    }

    val sandboxFile = File(getTempDir(), "sandbox.tar.gz")
    val rootfsFiles =
        sandboxDir().listFiles()?.filter {
            it.absolutePath != sandboxHomeDir().absolutePath &&
                it.absolutePath != sandboxDir().child("tmp").absolutePath
        } ?: emptyList()

    return if (sandboxFile.exists().not() || rootfsFiles.isEmpty().not()) {
        NEXT_STAGE.NONE
    } else {
        NEXT_STAGE.EXTRACTION
    }
}
