import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.bytebuddy.agent)
    implementation(libs.bytebuddy.core)
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("sandbox-agent")
    archiveClassifier.set("") // Erzeugt sandbox-agent.jar statt sandbox-agent-all.jar
    manifest {
        attributes["Premain-Class"] = "com.mobileide.core.pluginframework.sandbox-agent.SandboxAgent"
    }
}