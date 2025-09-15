plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.hilt)
    alias(libs.plugins.mobileide.android.compose)
}

android {
    namespace = "com.mobileide.onboarding"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
}
