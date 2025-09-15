import com.android.build.gradle.LibraryExtension

import com.mobileide.buildlogic.Versions
import com.mobileide.buildlogic.configureKotlinAndroid
import com.mobileide.buildlogic.extension.apply
import com.mobileide.buildlogic.extension.libs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.plugins.android.library)
                apply(libs.plugins.jetbrains.kotlin.android)
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = Versions.targetSdk
            }
        }
    }
}