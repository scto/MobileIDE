import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.mobileide.buildlogic"

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

gradlePlugin {
    plugins.register("android-application-compose-plugin") {
        id = "mobileide.application.compose"
        implementationClass = "AndroidApplicationComposePlugin"
    }

    plugins.register("android-application-plugin") {
        id = "mobileide.application"
        implementationClass = "AndroidApplicationPlugin"
    }

    plugins.register("android-library-compose-plugin") {
        id = "mobileide.library.compose"
        implementationClass = "AndroidLibraryComposePlugin"
    }

    plugins.register("android-library-plugin") {
        id = "mobileide.library"
        implementationClass = "AndroidLibraryPlugin"
    }

    plugins.register("code-quality-plugin") {
        id = "mobileide.code.quality"
        implementationClass = "CodeQualityPlugin"
    }

    plugins.register("gradle-versions-plguin") {
        id = "mobileide.gradle.versions"
        implementationClass = "GradleVersionPlugin"
    }
}
dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.gradle.build)
    implementation(libs.kotlin.stdlib)

    implementation(libs.detekt.plugin)
    implementation(libs.spotless.plugin)
    implementation(libs.ktlint.jlleitschuh.plugin)
    implementation(libs.gradle.versions.plugin)
}
