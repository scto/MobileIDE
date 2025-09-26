import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":plugin-api"))
    // pf4j-core wird von der Host-Anwendung bereitgestellt
    compileOnly(libs.pf4j.core)
}

tasks.register<Zip>("zipPlugin") {
    from(tasks.jar, sourceSets.main.get().resources)
    archiveFileName.set("${project.name}-${project.version}.zip")
    destinationDirectory.set(file("$buildDir/dist"))
}
// Sicherstellen, dass das ZIP nach dem Bauen erstellt wird
tasks.build { dependsOn("zipPlugin") }
