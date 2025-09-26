plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.hilt)
}

android {
    namespace = "com.mobileide.core.analytics"
}

dependencies {
    implementation(platform(libs.firebase.bom))
    api(libs.bundles.firebase)
    //api(libs.firebase.analytics)
    //api(libs.firebase.crashlytics)
    //api(libs.firebase.performance)
}