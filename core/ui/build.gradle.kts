plugins {
    id("mobileide.library.compose")
}

android {
    namespace = "com.mobileide.ui"
}

dependencies {
    api(project(":core:analytics"))
    implementation(libs.coil)
    api(platform(libs.androidx.compose.bom))
    api(libs.bundles.androidx.compose)
    
    //implementation(libs.androidx.compose.ui)
    //implementation(libs.androidx.compose.material3)
    //implementation(libs.androidx.compose.ui.tooling.preview)
}
