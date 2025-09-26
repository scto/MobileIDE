plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.compose)
    alias(libs.plugins.mobileide.android.hilt)
}

android {
    namespace = "com.mobileide.feature.debug"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    // Timber for logging
    implementation(libs.timber)
}