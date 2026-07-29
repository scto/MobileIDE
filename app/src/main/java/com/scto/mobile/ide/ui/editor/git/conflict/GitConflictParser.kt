package com.scto.mobile.ide.ui.editor.git.conflict

data class ConflictChunk(
    val id: Int,
    val localText: String,
    val incomingText: String,
    val baseText: String? = null,
    var resolvedText: String? = null,
    var isResolved: Boolean = false,
)

data class ParsedConflictFile(
    val filePath: String,
    val totalChunksCount: Int,
    val resolvedChunksCount: Int,
    val isFullyResolved: Boolean,
    val chunks: List<ConflictChunk>,
)

object GitConflictParser {

    fun hasConflictMarkers(content: String): Boolean {
        return content.contains("<<<<<<<") && content.contains("=======") && content.contains(">>>>>>>")
    }

    fun parseConflictFile(filePath: String, fileContent: String): ParsedConflictFile {
        val lines = fileContent.lines()
        val chunks = mutableListOf<ConflictChunk>()

        var chunkId = 1
        var inConflict = false
        var inLocal = false
        var inIncoming = false

        val localLines = mutableListOf<String>()
        val incomingLines = mutableListOf<String>()

        for (line in lines) {
            when {
                line.startsWith("<<<<<<<") -> {
                    inConflict = true
                    inLocal = true
                    inIncoming = false
                    localLines.clear()
                    incomingLines.clear()
                }
                line.startsWith("=======") && inConflict -> {
                    inLocal = false
                    inIncoming = true
                }
                line.startsWith(">>>>>>>") && inConflict -> {
                    inConflict = false
                    inIncoming = false
                    chunks.add(
                        ConflictChunk(
                            id = chunkId++,
                            localText = localLines.joinToString("\n"),
                            incomingText = incomingLines.joinToString("\n"),
                        )
                    )
                }
                inConflict -> {
                    if (inLocal) localLines.add(line) else if (inIncoming) incomingLines.add(line)
                }
            }
        }

        val resolvedCount = chunks.count { it.isResolved }
        return ParsedConflictFile(
            filePath = filePath,
            totalChunksCount = chunks.size,
            resolvedChunksCount = resolvedCount,
            isFullyResolved = chunks.isNotEmpty() && resolvedCount == chunks.size,
            chunks = chunks,
        )
    }

    fun rebuildResolvedFileContent(fileContent: String, resolvedChunks: Map<Int, String>): String {
        val lines = fileContent.lines()
        val resultLines = mutableListOf<String>()

        var currentChunkIndex = 1
        var inConflict = false

        for (line in lines) {
            when {
                line.startsWith("<<<<<<<") -> {
                    inConflict = true
                }
                line.startsWith(">>>>>>>") && inConflict -> {
                    inConflict = false
                    val resolved = resolvedChunks[currentChunkIndex] ?: ""
                    if (resolved.isNotEmpty()) {
                        resultLines.add(resolved)
                    }
                    currentChunkIndex++
                }
                !inConflict -> {
                    resultLines.add(line)
                }
            }
        }

        return resultLines.joinToString("\n")
    }
}
