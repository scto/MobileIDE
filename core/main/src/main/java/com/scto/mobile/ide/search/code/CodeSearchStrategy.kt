package com.scto.mobile.ide.search.code





import com.scto.mobile.ide.search.CodeItem
import kotlinx.coroutines.flow.Flow











/** Strategy for searching code content. */
interface CodeSearchStrategy {
    fun search(query: String): Flow<CodeItem>
}
