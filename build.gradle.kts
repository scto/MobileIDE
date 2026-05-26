// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ktfmt) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.dokka) apply false
}

subprojects {
    // Ktfmt Konfiguration
    plugins.withId(rootProject.libs.plugins.ktfmt.get().pluginId) {
        configure<com.ncorti.ktfmt.gradle.KtfmtExtension> {
            kotlinLangStyle()
            maxWidth.set(120)
        }
    }

    plugins.withId("org.jetbrains.dokka") {
        dependencies {
            dokkaPlugin(libs.dokka.mermaid)
        }
        
        configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            dokkaSourceSets.configureEach {
                moduleName.set(project.name)
                // Die deprecated suppress-Zeilen hier entfernen!
                
                includes.from("${rootDir}/module.md")
     
                sourceLink {
                    localDirectory.set(file("src/main/java"))
                    // Korrektur: URI statt URL
                    remoteUrl.set(uri("https://github.com/scto/MobileIDE/tree/main/src/main/java").toURI())
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }
}