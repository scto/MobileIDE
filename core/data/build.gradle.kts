plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.hilt)
}

android {
    namespace = "com.mobileide.data"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    //implementation(libs.hilt.android)
    //ksp(libs.hilt.compiler)

    // Jetpack DataStore for session management
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.annotation)
}
