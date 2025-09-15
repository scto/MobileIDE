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

include(":core:common")
include(":core:data")
include(":core:di")
include(":core:lsp-client")
include(":core:navigation")
include(":core:ui")
include(":core:util")

include(":feature:debug")
include(":feature:editor")
include(":feature:explorer")
include(":feature:git")
include(":feature:gradle")
include(":feature:home")
include(":feature:languages")
include(":feature:onboarding")
include(":feature:projectpicker")
include(":feature:settings")
include(":feature:sidepanel")
include(":feature:svgtoavd")
include(":feature:templates")
include(":feature:terminal")
include(":feature:termux")
include(":feature:xmltocompose")
