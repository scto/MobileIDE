package com.scto.mobile.ide.runner

import android.content.Context
import com.scto.mobile.ide.core.common.files.child
import com.scto.mobile.ide.core.common.files.createDirIfNot
import com.scto.mobile.ide.core.common.files.localDir
import java.io.File

fun runnerDir(context: Context = com.scto.mobile.ide.core.terminal.libcommons.application!!): File {
    val dir = localDir(context).child("runners")
    dir.mkdirs()
    return dir
}
