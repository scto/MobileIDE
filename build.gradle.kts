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
        // 1. Plugins als Dependency definieren
        dependencies {
            "dokkaPlugin"("org.jetbrains.dokka:mermaid-plugin:0.6.0")
            //"dokkaPlugin"(libs.dokka.mermaid)
        }
        
         // 2. Zentrale Konfiguration über die DokkaExtension
        configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            dokkaSourceSets.configureEach {
                moduleName.set(project.name)
                skipEmptyPackages.set(true)
                reportUndocumented.set(false)
                skipDeprecated.set(false)
                jdkVersion.set(17)

                // Pfad zur globalen module.md
                // NEU: Pfad zeigt jetzt in das Unterverzeichnis 'dokka'
                // Nutze 'fileTree', um alle .md Dateien in diesem Ordner einzuschließen
                includes.from(project.layout.projectDirectory.dir("dokka").asFileTree.matching {
                    include("*.md")
                })
                
                /*
                includes.from(rootProject.layout.projectDirectory.file("module.md"))
                */
                
                sourceLink {
                    localDirectory.set(file("src/main/java"))
                    // Korrektur: .toURI() verwenden
                    remoteUrl.set(uri("https://github.com/scto/MobileIDE/tree/main/src/main/java"))
                    remoteLineSuffix.set("#L")
                }
            }

            // 3. Plugin Konfiguration
            /*
            pluginsConfiguration {
                create("org.jetbrains.dokka.mermaid.MermaidPlugin")
            }
            */
        }
    }
}