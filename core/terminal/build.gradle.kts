plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktfmt)
}

android {
    namespace = "com.scto.mobile.ide.terminal"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
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
    implementation(project(":core:runner"))

    // Editor dependency for ExtraKeys code editor settings
    implementation(project(":editor"))

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.utilcode)
    implementation(libs.okhttp)
    implementation(libs.nanohttpd)
    implementation("androidx.browser:browser:1.8.0")
    implementation(libs.gson)
    implementation(libs.semver)

    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.material.icons.core)

    implementation(project(":core:terminal-view"))
    implementation(project(":core:terminal-emulator"))
}
