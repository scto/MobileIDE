plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.hilt)
    alias(libs.plugins.mobileide.android.compose)
}

android {
    namespace = "com.mobileide.projectpicker"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
