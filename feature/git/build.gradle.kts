plugins {
    id("com.mobileide.android.library")
    id("com.mobileide.android.hilt")
}

android {
    namespace = "com.mobileide.git"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.eclipse.jgit)
}
