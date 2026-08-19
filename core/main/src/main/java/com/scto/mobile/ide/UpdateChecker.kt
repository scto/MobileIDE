package com.scto.mobile.ide





import android.content.Intent
import androidx.core.net.toUri
import com.blankj.utilcode.util.StringUtils.getString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scto.mobile.ide.activities.main.MainActivity
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.utils.okHttpClient
import com.scto.mobile.ide.core.main.BuildConfig
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray












object UpdateChecker {

    @OptIn(DelicateCoroutinesApi::class)
    fun checkForUpdates(branch: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (Settings.check_for_update.not()) {
                    return@launch
                }

                val lastUpdate = Settings.last_update_check_timestamp
                val timeDifferenceInMillis =
                    if (lastUpdate > 0) {
                        (lastUpdate - System.currentTimeMillis()) * 1000
                    } else {
                        Long.MAX_VALUE
                    }

                val fifteenHoursInMillis = 15 * 60 * 60 * 1000

                val has15HoursPassed = timeDifferenceInMillis >= fifteenHoursInMillis

                if (has15HoursPassed.not()) {
                    return@launch
                }

                val url = "https://api.github.com/repos/Xed-Editor/Xed-Editor/commits?sha=$branch"
                val client = okHttpClient

                val request = Request.Builder().url(url).build()

                client.newCall(request).execute().use { response ->
                    val jsonResponse = response.body.string()
                    parseJson(jsonResponse)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                Settings.last_update_check_timestamp = System.currentTimeMillis()
            }
        }
    }

    private suspend inline fun parseJson(jsonStr: String) {
        val updates = mutableListOf<String>()

        val jsonArray = JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val latestCommit = jsonArray.getJSONObject(i)
            val author = latestCommit.getJSONObject("commit").getJSONObject("author").getString("name")

            if (author == "renovate[bot]") {
                continue
            }

            val commitMessage = latestCommit.getJSONObject("commit").getString("message")
            val date = latestCommit.getJSONObject("commit").getJSONObject("author").getString("date")

            if (convertToUnixTimestamp(BuildConfig.GIT_COMMIT_DATE) < convertToUnixTimestamp(date)) {
                if (commitMessage == "." || updates.contains(commitMessage)) {
                    continue
                }
                updates.add(
                    commitMessage
                        .replace("fix:", "Fixed")
                        .replace("fix :", "Fixed")
                        .replace("feat:", "Added")
                        .replace("feat.", "Added")
                        .replace("refactor:", "Improved")
                        .replace("refactor :", "Improved")
                        .replace("refactor.", "Improved")
                        .replace("feat :", "Added")
                        .replace(Regex("\\b(\\w+)\\b\\s+\\b\\1\\b", RegexOption.IGNORE_CASE), "$1")
                        .replaceFirstChar { it.uppercaseChar() }
                )
            }
        }

        withContext(Dispatchers.Main) {
            if (updates.isNotEmpty()) {
                XedHost?.let {
                    MaterialAlertDialogBuilder(it).apply {
                        setTitle(com.scto.mobile.ide.core.main.R.string.update_av.getString())
                        setMessage(updates.joinToString("\n"))
                        setPositiveButton(com.scto.mobile.ide.core.main.R.string.update.getString()) { _, _ ->
                            val url = "https://github.com/Xed-Editor/Xed-Editor"
                            val intent = Intent(Intent.ACTION_VIEW).apply { data = url.toUri() }
                            context.startActivity(intent)
                        }
                        setNegativeButton(com.scto.mobile.ide.core.main.R.string.ignore.getString(), null)
                        setCancelable(false)
                        show()
                    }
                }
            }
        }
    }

    private fun convertToUnixTimestamp(dateString: String): Long {
        val zonedDateTime = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        return zonedDateTime.toEpochSecond()
    }
}
