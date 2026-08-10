plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.rosemoe.sora.lsp"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        multiDexEnabled = true
        consumerProguardFiles("consumer-rules.pro")
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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    compileOnly(project(":editor"))
    implementation(project(":core:lsp"))
    implementation(project(":features:extensions"))
    api(libs.lsp4j)
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.androidsvg)
}
