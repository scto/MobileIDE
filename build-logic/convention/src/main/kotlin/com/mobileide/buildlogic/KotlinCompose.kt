package com.mobileide.buildlogic

import com.android.build.api.dsl.CommonExtension

import com.mobileide.buildlogic.extension.androidTestImplementation
import com.mobileide.buildlogic.extension.debugImplementation
import com.mobileide.buildlogic.extension.implementation
import com.mobileide.buildlogic.extension.libs

import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Configure Compose-specific options
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }

        dependencies {
            implementation(platform(libs.androidx.compose.bom))
            implementation(libs.bundles.androidx.compose)

            debugImplementation(libs.androidx.ui.test.manifest)

            androidTestImplementation(platform(libs.androidx.compose.bom))
            androidTestImplementation(libs.androidx.ui.test.junit4)
        }
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        enableStrongSkippingMode = true
    }
}