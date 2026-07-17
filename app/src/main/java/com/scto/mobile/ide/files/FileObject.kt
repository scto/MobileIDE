package com.scto.mobile.ide.files

import java.io.File

/**
 * Simple wrapper for a file with an associated builtin file type.
 */
class FileObject(
    val file: File,
    val type: BuiltinFileType
) {
    /** Returns an output stream for writing to the file. */
    fun getOutPutStream(append: Boolean = false) = if (append) file.outputStream().buffered() else file.outputStream().buffered()

    /** Returns an input stream for reading the file. */
    fun getInputStream() = file.inputStream().buffered()
}
