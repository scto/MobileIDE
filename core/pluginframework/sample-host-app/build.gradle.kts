import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.application)
}

application {
    mainClass.set("com.mobileide.core.pluginframework.sample-host-app.MainKt")
}

dependencies {
    implementation(project(":plugin-core"))
}