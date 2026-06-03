import java.io.ByteArrayOutputStream

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val gitCommitHash: Provider<String> =
    providers.exec { commandLine("git", "rev-parse", "--short=8", "HEAD") }.standardOutput.asText.map { it.trim() }

val fullGitCommitHash: Provider<String> =
    providers.exec { commandLine("git", "rev-parse", "HEAD") }.standardOutput.asText.map { it.trim() }

val gitCommitDate: Provider<String> =
    providers.exec { commandLine("git", "show", "-s", "--format=%cI", "HEAD") }.standardOutput.asText.map { it.trim() }



android {
    namespace = "com.rk.terminal"
    android.buildFeatures.buildConfig = true
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        debug{
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }


}

dependencies {
    api(libs.appcompat)
    api(libs.material)
    api(libs.constraintlayout)
    api(libs.navigation.fragment)
    api(libs.navigation.ui)
    api(libs.navigation.fragment.ktx)
    api(libs.navigation.ui.ktx)
    api(libs.activity)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.lifecycle.runtime.ktx)
    api(libs.activity.compose)
    api(platform(libs.compose.bom))
    api(libs.ui)
    api(libs.ui.graphics)
    api(libs.material3)
    api(libs.navigation.compose)
    api(project(":core:terminal-view"))
    api(project(":core:terminal-emulator"))
    api(libs.utilcode)
    //api(libs.commons.net)
    api(libs.okhttp)
    api(libs.anrwatchdog)
    api(libs.androidx.material.icons.core)
    api(libs.androidx.palette)
    api(libs.accompanist.systemuicontroller)
//    api(libs.termux.shared)

    api(project(":core:resources"))
    api(project(":core:components"))
}
