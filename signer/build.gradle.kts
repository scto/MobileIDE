plugins {
    `java-library`
    kotlin("jvm") version "2.2.20"
}

dependencies {
    // Kotlin Standard Bibliothek
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
    
    // Die offizielle Google apksig Bibliothek 
    // Sie enthält bereits die notwendigen kryptografischen Abhängigkeiten
    implementation("com.android.tools.build:apksig:8.8.0")
    
    // Optional: Falls du explizit BouncyCastle für KeyStore-Operationen im 
    // KeyStoreHelper benötigst, reicht diese moderne Version völlig aus.
    implementation("org.bouncycastle:bcprov-jdk15to18:1.78")
}

java {
    // Verwendung der Toolchain zur Vermeidung von JVM-Target-Problemen
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}