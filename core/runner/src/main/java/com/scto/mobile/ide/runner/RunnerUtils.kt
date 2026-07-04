package com.scto.mobile.ide.runner

import android.content.Context
import com.scto.mobile.ide.file.localDir
import com.scto.mobile.ide.utils.application
import java.io.File
import com.scto.mobile.ide.file.child
import com.scto.mobile.ide.file.createDirIfNot

fun runnerDir(context: Context = application!!): File {
    return localDir(context).child("runners").createDirIfNot()
}
