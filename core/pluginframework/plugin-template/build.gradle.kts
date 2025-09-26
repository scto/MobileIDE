import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.yourcompany"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // API des Plugin-Systems. Passen Sie die Gruppe und Version an.
    implementation("com.mobileide.core.pluginframework:plugin-api:1.0.0")

    // PF4J wird zur Compile-Zeit benötigt, aber von der Host-App zur Laufzeit bereitgestellt.
    compileOnly(libs.pf4j.core)
}
/*
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
*/

tasks.register<Zip>("zipPlugin") {
    from(tasks.jar, sourceSets.main.get().resources)
    archiveFileName.set("${project.name}-${project.version}.zip")
    destinationDirectory.set(file("$buildDir/dist"))
}

tasks.build { dependsOn("zipPlugin") }
