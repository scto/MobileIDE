plugins {
    id("com.mobileide.android.library")
    id("com.mobileide.android.hilt")
}

android {
    namespace = "com.mobileide.svgtoavd"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:common")) // Hinzugefügt für Resource-Wrapper

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
