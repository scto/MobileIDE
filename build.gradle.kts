// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//apply(from = "${rootDir}/scripts/read-arguments.gradle")

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.ktlint) apply false
}

buildscript {
    dependencies {
        classpath(libs.gradle.build)
        classpath(libs.kotlin.gradle.plugin)
        classpath(libs.spotless.plugin)
    }
}

fun Project.configureBaseExtension() {
    extensions.findByType(BaseExtension::class)?.run {
        compileSdkVersion(35)

        defaultConfig {
            minSdk = 28
            targetSdk = 35
            versionCode = 100
            versionName = "1.0.0"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

subprojects {
    plugins.withId("com.android.application") { configureBaseExtension() }
    plugins.withId("com.android.library") { configureBaseExtension() }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            apiVersion = KotlinVersion.KOTLIN_2_0
            languageVersion = KotlinVersion.KOTLIN_2_0
            jvmTarget = JvmTarget.JVM_17
            jvmTargetValidationMode = JvmTargetValidationMode.WARNING
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
}

tasks.register<Delete>("clean") { delete(rootProject.layout.buildDirectory) }
