package com.mobileide.app.utils

import android.content.Context
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists open-file session to
 *   /storage/emulated/0/MobileIDEProjects/<project>/.mide/editor/openedFiles.json
 *
 * Format mirrors AndroidIDE's openedFiles.json.
 */
object SessionManager {

    private const val MIDE_DIR     = ".mide"
    private const val EDITOR_DIR   = "editor"
    private const val SESSION_FILE = "openedFiles.json"
    private const val BACKUP_FILE  = "openedFiles.json.bak"

    data class FileSession(
        val filePath: String,
        val cursorLine: Int   = 0,
        val cursorColumn: Int = 0
    )

    data class EditorSession(
        val allFiles: List<FileSession> = emptyList(),
        val selectedFile: String       = ""
    )

    // ── Write ──────────────────────────────────────────────────────────────────

    suspend fun save(projectPath: String, session: EditorSession) =
        withContext(Dispatchers.IO) {
            try {
                val dir = mideEditorDir(projectPath)
                dir.mkdirs()

                val json = encodeSession(session)
                val file    = File(dir, SESSION_FILE)
                val backup  = File(dir, BACKUP_FILE)

                // rotate: current → backup
                if (file.exists()) file.copyTo(backup, overwrite = true)
                file.writeText(json)

                Logger.info(LogTag.WORKSPACE, "session saved: ${session.allFiles.size} files")
            } catch (e: Exception) {
                Logger.error(LogTag.WORKSPACE, "session save failed: ${e.message}")
            }
        }

    // ── Read ───────────────────────────────────────────────────────────────────

    suspend fun load(projectPath: String): EditorSession =
        withContext(Dispatchers.IO) {
            try {
                val file = File(mideEditorDir(projectPath), SESSION_FILE)
                if (!file.exists()) return@withContext EditorSession()
                decodeSession(file.readText())
            } catch (e: Exception) {
                // Try backup
                try {
                    val bak = File(mideEditorDir(projectPath), BACKUP_FILE)
                    if (bak.exists()) decodeSession(bak.readText())
                    else EditorSession()
                } catch (_: Exception) { EditorSession() }
            }
        }

    fun mideDir(projectPath: String): File =
        File(projectPath, MIDE_DIR)

    fun mideEditorDir(projectPath: String): File =
        File(mideDir(projectPath), EDITOR_DIR)

    // ── Encode / decode ────────────────────────────────────────────────────────

    private fun encodeSession(session: EditorSession): String {
        val arr = JSONArray()
        session.allFiles.forEach { fs ->
            val selection = JSONObject().apply {
                put("start", JSONObject().apply {
                    put("line", fs.cursorLine); put("column", fs.cursorColumn); put("index", 0)
                })
                put("end", JSONObject().apply {
                    put("line", fs.cursorLine); put("column", fs.cursorColumn); put("index", 0)
                })
            }
            arr.put(JSONObject().apply {
                put("file", fs.filePath)
                put("selection", selection)
            })
        }
        return JSONObject().apply {
            put("allFiles", arr)
            put("selectedFile", session.selectedFile)
        }.toString(2)
    }

    private fun decodeSession(json: String): EditorSession {
        val obj      = JSONObject(json)
        val arr      = obj.optJSONArray("allFiles") ?: return EditorSession()
        val selected = obj.optString("selectedFile", "")
        val files = (0 until arr.length()).map { i ->
            val entry = arr.getJSONObject(i)
            val sel   = entry.optJSONObject("selection")?.optJSONObject("start")
            FileSession(
                filePath     = entry.getString("file"),
                cursorLine   = sel?.optInt("line", 0) ?: 0,
                cursorColumn = sel?.optInt("column", 0) ?: 0
            )
        }
        return EditorSession(files, selected)
    }
}
