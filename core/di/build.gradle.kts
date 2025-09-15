plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.compose)
    alias(libs.plugins.mobileide.android.hilt)
}

dependencies {
    api(libs.bundles.koin)
}