plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.hilt)
    //alias(libs.plugins.mobileide.android.compose)
}

android {
    namespace = "com.mobileide.feature.languages"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:lsp-client"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
}