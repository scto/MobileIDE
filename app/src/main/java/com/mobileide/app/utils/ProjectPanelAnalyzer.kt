package com.mobileide.app.utils

import com.mobileide.app.data.AndroidModule
import com.mobileide.app.data.ProjectManifestInfo
import com.mobileide.app.data.StringResource
import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

/**
 * Parses Android project structure for the ProjectPanel side drawer.
 * All parsing is done on IO threads; results are plain data classes.
 */
object ProjectPanelAnalyzer {

    // ── Module discovery ───────────────────────────────────────────────────────

    fun findModules(projectRoot: File): List<AndroidModule> {
        Logger.info(LogTag.PROJECT_MGR, "findModules: ${projectRoot.name}")
        val modules = mutableListOf<AndroidModule>()
        try {
            val settings = File(projectRoot, "settings.gradle.kts")
                .takeIf { it.exists() }
                ?: File(projectRoot, "settings.gradle")
            if (!settings.exists()) return emptyList()

            val includeRe = Regex("""include\s*\(\s*["']([^"']+)["']\s*\)""")
            val includeRe2 = Regex("""include\s+["']([^"']+)["']""")
            val text = settings.readText()

            val moduleNames = mutableSetOf<String>()
            (includeRe.findAll(text) + includeRe2.findAll(text))
                .forEach { moduleNames.add(it.groupValues[1]) }

            moduleNames.forEach { name ->
                val path = name.replace(":", "/").trimStart('/')
                val moduleDir = File(projectRoot, path)
                val deps = parseDependencies(File(moduleDir, "build.gradle.kts")
                    .takeIf { it.exists() }
                    ?: File(moduleDir, "build.gradle"))
                modules += AndroidModule(
                    name         = name,
                    path         = moduleDir.absolutePath,
                    dependencies = deps,
                    isLibrary    = File(moduleDir, "src/main/java").exists().not()
                )
            }
        } catch (e: Exception) {
            Logger.error(LogTag.PROJECT_MGR, "findModules error: ${e.message}")
        }
        return modules.sortedBy { it.name }
    }

    private fun parseDependencies(buildFile: File?): List<String> {
        if (buildFile == null || !buildFile.exists()) return emptyList()
        val deps = mutableListOf<String>()
        val projectDep = Regex("""project\s*\(\s*["']([^"']+)["']\s*\)""")
        try {
            buildFile.readText().lines().forEach { line ->
                projectDep.find(line)?.let { deps.add(it.groupValues[1]) }
            }
        } catch (_: Exception) {}
        return deps
    }

    // ── AndroidManifest parsing ────────────────────────────────────────────────

    fun parseManifest(moduleDir: File): ProjectManifestInfo {
        val manifest = File(moduleDir, "src/main/AndroidManifest.xml")
        if (!manifest.exists()) return ProjectManifestInfo()

        val activities  = mutableListOf<String>()
        val permissions = mutableListOf<String>()
        val services    = mutableListOf<String>()
        val receivers   = mutableListOf<String>()
        val providers   = mutableListOf<String>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser  = factory.newPullParser()
            parser.setInput(StringReader(manifest.readText()))

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val name = parser.getAttributeValue(
                        "http://schemas.android.com/apk/res/android", "name"
                    ) ?: ""
                    when (parser.name) {
                        "activity"              -> if (name.isNotEmpty()) activities.add(simpleName(name))
                        "uses-permission",
                        "uses-permission-sdk-23"-> if (name.isNotEmpty()) permissions.add(name.substringAfterLast("."))
                        "service"               -> if (name.isNotEmpty()) services.add(simpleName(name))
                        "receiver"              -> if (name.isNotEmpty()) receivers.add(simpleName(name))
                        "provider"              -> if (name.isNotEmpty()) providers.add(simpleName(name))
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Logger.error(LogTag.PROJECT_MGR, "parseManifest error: ${e.message}")
        }

        return ProjectManifestInfo(activities, permissions, services, receivers, providers)
    }

    private fun simpleName(fqn: String) =
        if (fqn.startsWith(".")) fqn else fqn.substringAfterLast(".")

    // ── String resources ───────────────────────────────────────────────────────

    fun parseStrings(moduleDir: File): List<StringResource> {
        val stringsFile = File(moduleDir, "src/main/res/values/strings.xml")
        if (!stringsFile.exists()) return emptyList()
        val result = mutableListOf<StringResource>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser  = factory.newPullParser()
            parser.setInput(StringReader(stringsFile.readText()))
            var event = parser.eventType
            var lastName = ""
            while (event != XmlPullParser.END_DOCUMENT) {
                when {
                    event == XmlPullParser.START_TAG && parser.name == "string" -> {
                        lastName = parser.getAttributeValue(null, "name") ?: ""
                    }
                    event == XmlPullParser.TEXT && lastName.isNotEmpty() -> {
                        result += StringResource(lastName, parser.text.trim())
                        lastName = ""
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Logger.error(LogTag.PROJECT_MGR, "parseStrings error: ${e.message}")
        }
        return result
    }

    // ── Icon detection ─────────────────────────────────────────────────────────

    fun detectIconStyle(moduleDir: File): String {
        val vectorDir = File(moduleDir, "src/main/res/drawable")
        val hasVector = vectorDir.exists() && vectorDir.listFiles()
            ?.any { it.extension == "xml" } == true
        val hasMaterial = File(moduleDir, "build.gradle.kts")
            .takeIf { it.exists() }?.readText()
            ?.contains("material") == true
        return when {
            hasMaterial -> "Material"
            hasVector   -> "Vector"
            else        -> "PNG"
        }
    }

    // ── App data files (RKB Data tab) ──────────────────────────────────────────

    fun getAppDataDir(packageName: String): File =
        File("/data/data/$packageName")

    fun listAppDataDirs(packageName: String): List<File> {
        val base = getAppDataDir(packageName)
        return base.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    // ── File tree flattening ───────────────────────────────────────────────────

    fun flattenTree(root: File, depth: Int = 0, maxDepth: Int = 8): List<Pair<Int, File>> {
        if (depth > maxDepth) return emptyList()
        val result = mutableListOf<Pair<Int, File>>()
        root.listFiles()
            ?.filter { !it.name.startsWith(".") }
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.forEach { file ->
                result += depth to file
                if (file.isDirectory && depth < maxDepth) {
                    result += flattenTree(file, depth + 1, maxDepth)
                }
            }
        return result
    }

    // ── Project-level file count stats ────────────────────────────────────────

    data class FileStats(
        val totalFiles: Int,
        val ktFiles:    Int,
        val xmlFiles:   Int,
        val javaFiles:  Int,
        val otherFiles: Int,
        val totalSize:  Long
    )

    fun computeStats(root: File): FileStats {
        var total = 0; var kt = 0; var xml = 0; var java = 0; var other = 0; var size = 0L
        root.walkTopDown()
            .filter { it.isFile && !it.path.contains("/build/") && !it.path.contains("/.gradle/") }
            .forEach { f ->
                total++; size += f.length()
                when (f.extension.lowercase()) {
                    "kt", "kts" -> kt++
                    "xml"       -> xml++
                    "java"      -> java++
                    else        -> other++
                }
            }
        return FileStats(total, kt, xml, java, other, size)
    }
}
