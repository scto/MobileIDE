plugins {
    id("mobileide.library.compose")
    id("mobileide.code.quality")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mobileide.core.navigation"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:di"))
    implementation(project(":feature:home"))
    implementation(project(":feature:onboarding"))
    api(libs.androidx.navigation.compose)
}
