package com.scto.mobile.ide.core.tooling.impl.docs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.zip.ZipFile

data class SymbolDoc(
    val symbolName: String,
    val signature: String,
    val markdownDoc: String,
    val paramDocs: Map<String, String> = emptyMap(),
    val returnDoc: String? = null,
    val sourceModuleOrJar: String = "Stdlib / SDK",
    val timestamp: Long = System.currentTimeMillis()
)

object DocExtractor {

    private val docCache = mutableMapOf<String, SymbolDoc>()
    private val historyList = mutableListOf<SymbolDoc>()

    fun getHistory(): List<SymbolDoc> = historyList.toList()

    suspend fun fetchDocumentation(
        context: Context,
        symbolName: String,
        lspHoverResponse: String? = null
    ): SymbolDoc = withContext(Dispatchers.IO) {
        val cacheKey = symbolName.trim()
        docCache[cacheKey]?.let { return@withContext it }

        // 1. Primary Source: LSP Hover response if present and rich
        if (!lspHoverResponse.isNullOrBlank() && lspHoverResponse.length > 20) {
            val parsedDoc = parseMarkdownContent(symbolName, lspHoverResponse, "LSP Server")
            cacheAndRecord(cacheKey, parsedDoc)
            return@withContext parsedDoc
        }

        // 2. Fallback Source: Scan local -sources.jar in Gradle cache
        val sourcesJarDoc = searchInGradleSourcesJars(context, symbolName)
        if (sourcesJarDoc != null) {
            cacheAndRecord(cacheKey, sourcesJarDoc)
            return@withContext sourcesJarDoc
        }

        // 3. Fallback Synthetic Documentation
        val syntheticDoc = SymbolDoc(
            symbolName = symbolName,
            signature = "fun $symbolName(...)",
            markdownDoc = "### `$symbolName`\n\nAutomatisierte Dokumentation aus Symbol-Index.\n\n*Keine ausführliche KDoc-Beschreibung in `-sources.jar` gefunden.*",
            sourceModuleOrJar = "Standard Kotlin/Android Index"
        )
        cacheAndRecord(cacheKey, syntheticDoc)
        syntheticDoc
    }

    private fun cacheAndRecord(key: String, doc: SymbolDoc) {
        docCache[key] = doc
        historyList.removeAll { it.symbolName == doc.symbolName }
        historyList.add(0, doc)
        if (historyList.size > 25) {
            historyList.removeAt(historyList.lastIndex)
        }
    }

    private fun parseMarkdownContent(symbolName: String, rawContent: String, source: String): SymbolDoc {
        val lines = rawContent.lines()
        val paramDocs = mutableMapOf<String, String>()
        var returnDoc: String? = null
        val docLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("@param") -> {
                    val parts = trimmed.removePrefix("@param").trim().split(" ", limit = 2)
                    if (parts.size == 2) {
                        paramDocs[parts[0]] = parts[1]
                    }
                }
                trimmed.startsWith("@return") -> {
                    returnDoc = trimmed.removePrefix("@return").trim()
                }
                else -> {
                    docLines.add(line)
                }
            }
        }

        return SymbolDoc(
            symbolName = symbolName,
            signature = lines.firstOrNull { it.contains("fun ") || it.contains("class ") || it.contains("val ") } ?: "symbol $symbolName",
            markdownDoc = docLines.joinToString("\n"),
            paramDocs = paramDocs,
            returnDoc = returnDoc,
            sourceModuleOrJar = source
        )
    }

    private fun searchInGradleSourcesJars(context: Context, symbolName: String): SymbolDoc? {
        try {
            val userHome = File(System.getProperty("user.home") ?: "/root")
            val gradleCache = File(userHome, ".gradle/caches/modules-2/files-2.1")
            val targetDirs = listOf(
                gradleCache,
                File(context.filesDir.parentFile ?: context.filesDir, "local/sandbox/root/.gradle/caches/modules-2/files-2.1")
            )

            for (cacheDir in targetDirs) {
                if (!cacheDir.exists()) continue
                cacheDir.walkTopDown()
                    .filter { it.isFile && it.name.endsWith("-sources.jar") }
                    .take(20)
                    .forEach { jarFile ->
                        try {
                            ZipFile(jarFile).use { zip ->
                                val entries = zip.entries()
                                while (entries.hasMoreElements()) {
                                    val entry = entries.nextElement()
                                    if (entry.name.endsWith(".kt") || entry.name.endsWith(".java")) {
                                        val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                                        if (content.contains("fun $symbolName") || content.contains("class $symbolName")) {
                                            val docBlock = extractKDocComment(content, symbolName)
                                            if (docBlock != null) {
                                                return parseMarkdownContent(symbolName, docBlock, jarFile.name)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.d("Skip corrupted jar ${jarFile.name}")
                        }
                    }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error scanning sources jars")
        }
        return null
    }

    private fun extractKDocComment(sourceText: String, symbolName: String): String? {
        val lines = sourceText.lines()
        val symbolLineIdx = lines.indexOfFirst { it.contains("fun $symbolName") || it.contains("class $symbolName") || it.contains("val $symbolName") }
        if (symbolLineIdx <= 0) return null

        val commentLines = mutableListOf<String>()
        var idx = symbolLineIdx - 1
        var inComment = false

        while (idx >= 0 && idx >= symbolLineIdx - 30) {
            val line = lines[idx].trim()
            if (line.endsWith("*/")) inComment = true
            if (inComment) {
                commentLines.add(0, line.removePrefix("/**").removePrefix("/*").removePrefix("*").trim())
            }
            if (line.startsWith("/**") || line.startsWith("/*")) break
            idx--
        }

        return if (commentLines.isNotEmpty()) commentLines.joinToString("\n") else null
    }
}
