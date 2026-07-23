package com.scto.mobile.ide.core.common.utils

import com.scto.mobile.ide.core.common.files.BuiltinFileType
import com.scto.mobile.ide.core.common.files.FileObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

private val binaryExtensions: Set<String> =
    (BuiltinFileType.IMAGE.extensions +
            BuiltinFileType.AUDIO.extensions +
            BuiltinFileType.VIDEO.extensions +
            BuiltinFileType.ARCHIVE.extensions +
            BuiltinFileType.APK.extensions +
            BuiltinFileType.EXECUTABLE.extensions)
        .map { it.lowercase() }
        .toSet()

fun isBinaryExtension(fileExt: String): Boolean {
    return fileExt.lowercase() in binaryExtensions
}

suspend fun FileObject.writeObject(obj: Any) =
    withContext(Dispatchers.IO) { ObjectOutputStream(getOutPutStream(false)).use { oos -> oos.writeObject(obj) } }

suspend fun FileObject.readObject(): Any? =
    withContext(Dispatchers.IO) {
        ObjectInputStream(getInputStream()).use { ois ->
            return@withContext ois.readObject()
        }
    }
