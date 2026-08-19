package com.scto.mobile.ide.search.code





import android.content.Context
import com.scto.mobile.ide.activities.main.MainViewModel
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.file.toFileWrapper
import com.scto.mobile.ide.search.CodeItem
import com.scto.mobile.ide.search.index.IndexDatabase
import com.scto.mobile.ide.search.utils.SearchUtils
import com.scto.mobile.ide.utils.logError
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext











/** Code search using indexed database. Returns results as Flow for streaming. */
class CodeSearchIndexed(
    private val context: Context,
    private val projectRoot: FileObject,
    private val mainViewModel: MainViewModel,
    private val fileMaskFilter: (String) -> Boolean,
    private val ignoreCase: Boolean,
    private val openPaths: Set<String>,
) : CodeSearchStrategy {
    override fun search(query: String): Flow<CodeItem> = channelFlow {
        withContext(Dispatchers.IO) {
            try {
                val dao = IndexDatabase.getDatabase(context, projectRoot).codeIndexDao()
                var resultLimit = 5
                var offset = 0

                while (true) {
                    currentCoroutineContext().ensureActive()

                    val results =
                        if (ignoreCase) {
                            dao.search(query, resultLimit, offset)
                        } else {
                            dao.searchCaseSensitive(query, resultLimit, offset)
                        }
                    if (results.isEmpty()) break

                    for (result in results) {
                        try {
                            if (result.path in openPaths) continue
                            val file = File(result.path).toFileWrapper()
                            val fileExt = file.getExtension()

                            if (!fileMaskFilter(fileExt)) continue

                            val indices =
                                SearchUtils.findAllIndices(
                                    result.content,
                                    query,
                                    ignoreCase = ignoreCase,
                                )
                            for (index in indices) {
                                val absoluteCharIndex = result.chunkStart + index

                                currentCoroutineContext().ensureActive()
                                send(
                                    SearchUtils.createCodeItem(
                                        context = context,
                                        mainViewModel = mainViewModel,
                                        text = result.content,
                                        charIndex = absoluteCharIndex,
                                        query = query,
                                        file = file,
                                        projectRoot = projectRoot,
                                        lineIndex = result.lineNumber,
                                    )
                                )
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logError(e, "Error processing indexed code result")
                        }
                    }
                    offset += resultLimit
                    resultLimit = 20
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError(e, "Error searching code index")
            }
        }
    }
}
