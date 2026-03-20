package com.mobileide.app.utils

import com.mobileide.app.logger.Logger
import com.mobileide.app.logger.LogTag
import android.os.Environment
import com.mobileide.app.data.Project
import com.mobileide.app.data.ProjectType
import java.io.File

object ProjectManager {

    /**
     * Primary projects directory on external storage.
     * Falls back to internal app files dir if storage access is unavailable.
     */
    /**
     * Projects root: /storage/emulated/0/MobileIDEProjects
     * Falls back to Android/data if no external storage access.
     */
    val projectsRoot: File
        get() {
            val primary = File(Environment.getExternalStorageDirectory(), "MobileIDEProjects")
            return if (primary.canWrite() || primary.mkdirs()) primary
            else File(
                Environment.getExternalStorageDirectory(),
                "Android/data/com.mobileide.app/files/projects"
            ).also { it.mkdirs() }
        }

    fun listProjects(): List<Project> {
        Logger.info(LogTag.PROJECT_MGR, "listProjects from ${projectsRoot.absolutePath}")
        return projectsRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?.map { Project(it.name, it.absolutePath) }
            ?: emptyList()
    }

    fun createProject(
        name: String,
        packageName: String = "com.example.${name.lowercase().filter { it.isLetterOrDigit() }}",
        minSdk: Int         = 26,
        targetSdk: Int      = 35,
        templateId: String  = "empty_compose",
        type: ProjectType   = ProjectType.KOTLIN_ANDROID
    ): Project {
        val projectDir = File(projectsRoot, name).also { it.mkdirs() }
        val appDir     = File(projectDir, "app").also { it.mkdirs() }
        val mainDir    = File(appDir, "src/main").also { it.mkdirs() }
        val javaDir    = File(mainDir, "java/${packageName.replace('.', '/')}").also { it.mkdirs() }
        val resDir     = File(mainDir, "res/values").also { it.mkdirs() }

        File(projectDir, "settings.gradle.kts").writeText("""
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "$name"
include(":app")
        """.trimIndent())

        File(projectDir, "build.gradle.kts").writeText("""
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}
        """.trimIndent())

        File(appDir, "build.gradle.kts").writeText("""
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "$packageName"
    compileSdk = 35
    defaultConfig {
        applicationId = "$packageName"
        minSdk = $minSdk; targetSdk = $targetSdk; versionCode = 1; versionName = "1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
        """.trimIndent())

        File(mainDir, "AndroidManifest.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:allowBackup="true" android:label="$name"
        android:theme="@style/Theme.AppCompat.DayNight">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
        """.trimIndent())

        File(javaDir, "MainActivity.kt").writeText("""
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Hello from $name!", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("Built with MobileIDE 🚀")
                    }
                }
            }
        }
    }
}
        """.trimIndent())

        File(resDir, "strings.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">$name</string>
</resources>
        """.trimIndent())

        return Project(name, projectDir.absolutePath, type)
    }

    fun deleteProject(project: Project) {
        File(project.path).deleteRecursively()
    }
}
