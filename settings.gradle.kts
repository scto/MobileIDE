/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

val aapt2OverrideKey = "android.aapt2FromMavenOverride"

// 1. Read from gradle properties (command line or gradle.properties)
val gradleAapt2 = providers.gradleProperty(aapt2OverrideKey).orNull

// 2. Read from local.properties
val localPropertiesFile = java.io.File(rootDir, "local.properties")
val localAapt2 = if (localPropertiesFile.exists()) {
    java.util.Properties().apply {
        localPropertiesFile.inputStream().use { load(it) }
    }.getProperty(aapt2OverrideKey)
} else null

// 3. Find first path that actually exists on disk
val customAapt2 = listOf(
    gradleAapt2,
    localAapt2,
    "/usr/bin/aapt2",
    "/data/data/com.termux/files/usr/bin/aapt2"
).filterNotNull().firstOrNull { java.io.File(it).exists() }

if (customAapt2 != null) {
    gradle.beforeProject {
        extensions.extraProperties.set(aapt2OverrideKey, customAapt2)
    }
}

pluginManagement {
    repositories {
        google()
        /*
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        */
        mavenCentral()
        gradlePluginPortal()

    }
    resolutionStrategy {
        eachPlugin {
                if (requested.id.id == "com.google.android.gms.oss-licenses-plugin") {
                    useModule("com.google.android.gms:oss-licenses-plugin:0.10.9")
                }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MobileIDE"

include(":app",":editor",":editor-lsp",":language-treesitter")

include(":core:main")
include(":core:components")
include(":core:resources")
include(":core:terminal-emulator")
include(":core:terminal-view")