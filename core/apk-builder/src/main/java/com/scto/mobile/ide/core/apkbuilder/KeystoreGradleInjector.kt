package com.scto.mobile.ide.core.apkbuilder

import java.io.File

object KeystoreGradleInjector {

    fun findAppGradleFile(projectPath: String): File? {
        val kts = File(projectPath, "app/build.gradle.kts")
        if (kts.exists()) return kts
        val groovy = File(projectPath, "app/build.gradle")
        if (groovy.exists()) return groovy
        return null
    }

    fun hasReleaseSigningConfig(projectPath: String): Boolean {
        val file = findAppGradleFile(projectPath) ?: return false
        val text = file.readText()
        return text.contains("signingConfigs") && (text.contains("release") || text.contains("keystore.properties"))
    }

    fun generateGradleSnippet(isKotlinDsl: Boolean): String {
        return if (isKotlinDsl) {
            """
// --- Added by MobileIDE Signing Wizard ---
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = java.util.Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile", "keystores/release.jks"))
            storePassword = keystoreProperties.getProperty("storePassword", "")
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
// ------------------------------------------
""".trimIndent()
        } else {
            """
// --- Added by MobileIDE Signing Wizard ---
def keystorePropertiesFile = rootProject.file('keystore.properties')
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        release {
            storeFile file(keystoreProperties['storeFile'] ?: 'keystores/release.jks')
            storePassword keystoreProperties['storePassword'] ?: ''
            keyAlias keystoreProperties['keyAlias'] ?: ''
            keyPassword keystoreProperties['keyPassword'] ?: ''
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
// ------------------------------------------
""".trimIndent()
        }
    }

    fun injectSigningConfig(projectPath: String): Boolean {
        val file = findAppGradleFile(projectPath) ?: return false
        val isKotlinDsl = file.name.endsWith(".kts")
        val backupFile = File(file.parentFile, "${file.name}.bak")

        try {
            // Backup
            file.copyTo(backupFile, overwrite = true)

            val originalContent = file.readText()
            val snippet = generateGradleSnippet(isKotlinDsl)

            val newContent = "$originalContent\n\n$snippet"
            file.writeText(newContent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
