plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.hilt)
}

android {
    namespace = "com.mobileide.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
