plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.compose)
    alias(libs.plugins.mobileide.android.hilt)
}

android {
    namespace = "com.mobileide.ui"
}

dependencies {
    api(project(":core:analytics"))
    implementation(libs.coil)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
}
