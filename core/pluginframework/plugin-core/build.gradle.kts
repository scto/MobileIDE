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
dependencies {
    api(project(":plugin-api"))
    api(libs.pf4j.core)
    api(libs.pf4j.update)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.slf4j.simple)
}
