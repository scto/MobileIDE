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

    // Globale Dokka v2 Konfiguration
    plugins.withId("org.jetbrains.dokka") {
        // Sicherstellen, dass das Mermaid-Plugin als Abhängigkeit verfügbar ist
        dependencies {
            dokkaPlugin(libs.dokka.mermaid)
        }

        // Dokka Konfiguration für alle Subprojekte
        configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            dokkaSourceSets.configureEach {
                moduleName.set(project.name)
                skipEmptyPackages.set(true)
                reportUndocumented.set(false)
                skipDeprecated.set(false)
                jdkVersion.set(17)
                suppressInheritedMembers.set(false)
                suppressObviousFunctions.set(false)
                
                // Pfad zur globalen module.md
                includes.from("${rootDir}/module.md")
                
                sourceLink {
                    localDirectory.set(file("src/main/java"))
                    remoteUrl.set(uri("https://github.com/scto/MobileIDE/tree/main/src/main/java").toURL())
                    remoteLineSuffix.set("#L")
                }
            }

            // Typsichere Plugin-Konfiguration
            pluginsConfiguration {
                create("org.jetbrains.dokka.mermaid.MermaidPlugin")
            }
        }
    }
}