pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MobileIDE"

// ── App ───────────────────────────────────────────────────────────────────────
include(":app")

// ── Core ──────────────────────────────────────────────────────────────────────
include(":core:logger")

// ── Feature submodules ────────────────────────────────────────────────────────
include(":feature:editor")    // standalone Sora editor with tab bar
include(":feature:settings")  // all settings screens as library module
