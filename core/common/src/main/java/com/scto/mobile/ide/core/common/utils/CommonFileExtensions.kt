package com.scto.mobile.ide.core.common.utils

import java.io.File

fun File.child(name: String): File {
    return File(this, name)
}

fun File.createDirIfNot() {
    if (!this.exists()) {
        this.mkdirs()
    }
}
