pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // NEU: JitPack Repository hinzufügen, um die TreeView-Bibliothek zu laden
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MobileIDE"

include(":app")

include(
    ":core:analytics",
    ":core:common",
    ":core:coroutines",
    ":core:data",
    ":core:di",
    ":core:lsp-client",
    ":core:navigation",
    ":core:pluginframework:plugin-core",
    ":core:pluginframework:plugin-api",
    ":core:pluginframework:sandbox-agent",
    ":core:pluginframework:sample-host-app",
    ":core:pluginframework:sample-plugin",
    ":core:pluginframework:another-plugin",
    ":core:ui",
    ":core:util",
    ":feature:debug",
    ":feature:editor",
    ":feature:explorer",
    ":feature:git",
    ":feature:gradle",
    ":feature:home",
    ":feature:languages",
    ":feature:onboarding",
    ":feature:projectpicker",
    ":feature:settings",
    ":feature:sidepanel",
    ":feature:svgtoavd",
    ":feature:templates",
    ":feature:terminal",
    ":feature:termux",
    ":feature:xmltocompose"
)