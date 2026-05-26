// Copyright 2025 Thomas Schmid
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.mobile.ide.core.build"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // kotlin { jvmToolchain(libs.versions.java.get()) }
    kotlin { jvmToolchain(17) }
}

dependencies {
    // Interne Abhängigkeiten
    implementation(project(":signer"))
    implementation(project(":core:utils"))

    // --- Einbindung der Drittanbieter-Bibliotheken ---

    // Methode A: Falls sie als lokale .jar/.aar Dateien im 'libs' Ordner liegen:
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation(files("libs/xml.jar"))

    // Methode B: Falls 'ApkXmlEditor' ein eigenes Modul im Projekt ist (empfohlen für sauberere Struktur):
    // implementation(project(":libs:ApkXmlEditor"))

    // Methode C: Falls 'apksigner' über ein Repository kommt (hier als Beispiel):
    // implementation("com.mcal:apksigner:1.0.0")

    implementation("com.github.iyxan23:zipalign-java:1.2.2")

    // Kotlin Standard-Bibliotheken
    // implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.2.0"))
    implementation(platform(libs.kotlin.bom))

    implementation(libs.androidx.core.ktx)

    // Android API Support
    // compileOnly wird genutzt, da die Android-Klassen zur Laufzeit vom System bereitgestellt werden
    compileOnly("com.google.android:android:4.1.1.4")

    // Hilt Abhängigkeiten
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
