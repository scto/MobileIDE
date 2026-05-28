plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.mobile.ide.core.files"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        // Hinzugefügt für Compose-Support
        vectorDrawables { useSupportLibrary = true }
    }

    buildFeatures { compose = true }
}

dependencies {
    // Projekt Abhängigkeiten
    implementation(project(":core:resources"))
    implementation(project(":core:utils"))
    
    // Hilt Abhängigkeiten
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Notwendige Compose Abhängigkeiten für FileTree.kt
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    // Notwendig für Icons wie Icons.Default.ChevronRight etc.
    implementation(libs.androidx.compose.material.icons.extended)
}
