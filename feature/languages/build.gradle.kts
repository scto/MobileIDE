plugins {
    id("com.mobileide.android.library") // Dein Convention Plugin
    id("com.mobileide.android.hilt")
}

android {
    namespace = "com.mobileide.languages"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:lsp-client"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
