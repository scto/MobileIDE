// Pure JVM library — runs inside proot Ubuntu as a standalone server process.
// Does NOT depend on Android APIs.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Shared IPC protocol constants
    implementation(project(":core:tooling:tooling-api"))

    // Gradle Tooling API (Eclipse Buildship) — JVM only
    implementation("org.gradle:gradle-tooling-api:8.11")

    // SLF4J (required by Gradle Tooling API)
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")
}