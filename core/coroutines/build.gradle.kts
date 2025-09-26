plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.hilt)
}

android {
    namespace = "com.mobileide.core.coroutines"
}

dependencies {
    api(libs.bundles.kotlinx.coroutines)
}