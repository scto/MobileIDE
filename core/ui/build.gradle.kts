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
    namespace = "com.mobile.ide.core.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

    buildFeatures { compose = true }
}

dependencies {
    // Projekt Abhängigkeiten
    implementation(project(":core:resources"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    // Notwendig für Icons wie Icons.Default.ChevronRight etc.
    implementation(libs.androidx.compose.material.icons.extended)

    // Notwendig für LocalView, WindowCompat und Aktivitäts-Integration
    implementation(libs.androidx.activity.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // DataStore (für ThemeDataStore.kt)
    implementation(libs.androidx.datastore.preferences)

    // Lifecycle ViewModel Compose (für ThemeViewModel.kt)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
