package com.scto.mobile.ide.search.file





import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.search.index.FileMeta











/** Strategy for searching file names. */
interface FileSearchStrategy {
    suspend fun search(query: String, projectRoot: FileObject): List<FileMeta>
}
