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
include(":core:editor:api")
include(":core:editor:impl")
include(":core:editor:lexers")
include(":core:editor:treesitter")
include(":core:termux:application")
include(":core:termux:emulator")
include(":core:termux:shared")
include(":core:termux:view")

// ── Feature submodules ────────────────────────────────────────────────────────
include(":feature:editor")    // standalone Sora editor with tab bar
include(":feature:settings")  // all settings screens as library module
