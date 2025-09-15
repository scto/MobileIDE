plugins {
    alias(libs.plugins.mobileide.android.library)
    alias(libs.plugins.mobileide.android.compose)
}

android {
    namespace = "com.mobileide.navigation"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:di"))
    implementation(project(":feature:home"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:people:profile"))
    implementation(project(":feature:people:list"))
    implementation(project(":feature:chat:list"))
    implementation(project(":feature:user:profile"))
    implementation(project(":feature:subscription"))


    api(libs.androidx.navigation.compose)
}
