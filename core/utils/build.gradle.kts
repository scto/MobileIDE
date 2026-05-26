// Copyright 2025 Thomas Schmid
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
    namespace = "com.mobile.ide.core.utils"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // ProGuard-Regeln für Bibliotheks-Module
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures { compose = true }
}

dependencies {
    // Interne Modul-Abhängigkeiten
    implementation(project(":core:resources"))
    // implementation(project(":signer"))

    // Lokale Bibliotheken aus dem libs-Ordner (z.B. xml.jar)
    // implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    // DataStore für Konfigurationen (LogConfig, Workspace)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    // Jetpack Compose Unterstützung
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // Notwendig für LocalView, WindowCompat und Aktivitäts-Integration
    implementation(libs.androidx.activity.compose)
    // Koroutinen für asynchrone Aufgaben (Workspace, Log-Writing)
    implementation(libs.kotlinx.coroutines.android)

    // Zipalign für APK-Bauprozesse
    // implementation("com.github.iyxan23:zipalign-java:1.2.2")

    // Standard-Bibliotheken
    implementation(libs.androidx.core.ktx)

    // Hilt Abhängigkeiten
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
