plugins {
    alias(libs.plugins.mobileide.android.library)
    //alias(libs.plugins.mobileide.jvm.library)
    alias(libs.plugins.mobileide.android.hilt)
}

android {
    namespace = "com.mobileide.core.lsp-client"
}

dependencies {
    // Coroutines & Serialization
    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
}