package com.scto.mobile.ide.utils

import android.content.Context
import com.scto.mobile.ide.core.common.utils.LogCatcher
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class TabState(
    val filePath: String,
    val cursorLine: Int = 0,
    val cursorColumn: Int = 0,
)

data class SavedSessionState(
    val timestamp: Long = System.currentTimeMillis(),
    val projectPath: String = "",
    val openTabs: List<TabState> = emptyList(),
    val activeTabPath: String = "",
    val terminalWorkingDirs: List<String> = emptyList(),
)

object SessionStateManager {
    private const val TAG = "SessionStateManager"
    private const val FILE_NAME = "session_state.json"

    private fun getFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun hasSavedState(context: Context): Boolean {
        val file = getFile(context)
        return file.exists() && file.length() > 0
    }

    fun saveState(context: Context, state: SavedSessionState) {
        try {
            val json = JSONObject()
            json.put("timestamp", state.timestamp)
            json.put("projectPath", state.projectPath)
            json.put("activeTabPath", state.activeTabPath)

            val tabsArray = JSONArray()
            for (tab in state.openTabs) {
                val tabJson = JSONObject()
                tabJson.put("filePath", tab.filePath)
                tabJson.put("cursorLine", tab.cursorLine)
                tabJson.put("cursorColumn", tab.cursorColumn)
                tabsArray.put(tabJson)
            }
            json.put("openTabs", tabsArray)

            val termArray = JSONArray()
            for (dir in state.terminalWorkingDirs) {
                termArray.put(dir)
            }
            json.put("terminalWorkingDirs", termArray)

            getFile(context).writeText(json.toString())
            LogCatcher.i(TAG, "Successfully saved session state with ${state.openTabs.size} tabs")
        } catch (e: Exception) {
            LogCatcher.e(TAG, "Failed to save session state", e)
        }
    }

    fun loadState(context: Context): SavedSessionState? {
        val file = getFile(context)
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            val timestamp = json.optLong("timestamp", System.currentTimeMillis())
            val projectPath = json.optString("projectPath", "")
            val activeTabPath = json.optString("activeTabPath", "")

            val openTabs = mutableListOf<TabState>()
            val tabsArray = json.optJSONArray("openTabs")
            if (tabsArray != null) {
                for (i in 0 until tabsArray.length()) {
                    val t = tabsArray.getJSONObject(i)
                    openTabs.add(
                        TabState(
                            filePath = t.optString("filePath", ""),
                            cursorLine = t.optInt("cursorLine", 0),
                            cursorColumn = t.optInt("cursorColumn", 0),
                        )
                    )
                }
            }

            val terminalDirs = mutableListOf<String>()
            val termArray = json.optJSONArray("terminalWorkingDirs")
            if (termArray != null) {
                for (i in 0 until termArray.length()) {
                    terminalDirs.add(termArray.getString(i))
                }
            }

            SavedSessionState(
                timestamp = timestamp,
                projectPath = projectPath,
                openTabs = openTabs,
                activeTabPath = activeTabPath,
                terminalWorkingDirs = terminalDirs,
            )
        } catch (e: Exception) {
            LogCatcher.e(TAG, "Failed to load session state", e)
            null
        }
    }

    fun clearState(context: Context) {
        try {
            val file = getFile(context)
            if (file.exists()) {
                file.delete()
            }
            LogCatcher.i(TAG, "Session state cleared")
        } catch (e: Exception) {
            LogCatcher.e(TAG, "Failed to clear session state", e)
        }
    }
}
