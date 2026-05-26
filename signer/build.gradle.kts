plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.2.0"
}

dependencies {
    // Kotlin Standard Bibliothek
    //implementation(kotlin("stdlib"))
    // Source: https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
    
    // Spongycastle Abhängigkeiten
    implementation("org.bouncycastle:bcprov-jdk15on:1.58")
    implementation("com.madgag.spongycastle:core:1.58.0.0")
    implementation("com.madgag.spongycastle:prov:1.58.0.0")
}

java {
    // Verwendung der Toolchain zur Vermeidung von JVM Target Problemen
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            groupId = "com.mcal"
            artifactId = "apksigner"
            version = "1.2.0"
        }
    }
}