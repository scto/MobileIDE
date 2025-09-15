plugins {
    id("com.mobileide.jvm.library") // Ein neues Convention Plugin für reine Kotlin-Module
}

dependencies {
    // Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}

