plugins {
    id("com.mobileide.android.library")
    id("com.mobileide.android.hilt")
}

android {
    namespace = "com.mobileide.sidepanel"
    buildFeatures {
        compose = true
    }
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

    // NEU: Abhängigkeit für die TreeView-Bibliothek
    //implementation("com.github.AfigAliyev:TreeView:1.0.2")
    implementation(libs.treeview)
}

