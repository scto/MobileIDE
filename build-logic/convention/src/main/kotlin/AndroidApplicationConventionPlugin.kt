import com.android.build.api.dsl.ApplicationExtension

import com.mobileide.buildlogic.Versions
import com.mobileide.buildlogic.configureKotlinAndroid
import com.mobileide.buildlogic.extension.apply
import com.mobileide.buildlogic.extension.libs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.plugins.android.application)
                apply(libs.plugins.jetbrains.kotlin.android)
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = Versions.targetSdk
            }
        }
    }
}