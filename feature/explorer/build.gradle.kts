plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.hilt)
    alias(libs.plugins.mobileide.android.compose)
}

android {
    namespace = "com.mobileide.feature.explorer"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
}