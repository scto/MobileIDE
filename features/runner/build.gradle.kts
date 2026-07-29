plugins {
    id("org.jetbrains.kotlin.android")
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktfmt)
}

android {
    namespace = "com.scto.mobile.ide.features.runner"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
}

dependencies {
    implementation(project(":core:main"))
    implementation(project(":core:components"))
    implementation(project(":core:resources"))

    // Editor dependencies for code runner settings and markdown rendering
    implementation(project(":editor"))
    implementation(project(":editor-lsp"))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.utilcode)
    implementation(libs.okhttp)
    implementation(libs.nanohttpd)
    implementation(libs.androidx.browser)
    implementation(libs.gson)

    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.material.icons.core)
}
