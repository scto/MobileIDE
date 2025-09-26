import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

/*
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
*/

// Dieses Modul wird zum Artefakt 'com.mobileide.core.pluginframework:plugin-api:1.0.0'
dependencies {
    implementation(libs.pf4j.core)
    implementation(libs.kotlinx.coroutines)
}
