plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.kotlin.compose)
//    alias(libs.plugins.ktfmt)
//    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "com.scto.mobile.ide.extension.languages"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
}

dependencies {
    // Compile against the MobileIDE APIs
    compileOnly(project(":core:extension"))
    compileOnly(project(":core:lsp"))
    compileOnly(project(":core:commands"))
    
    // Additional dependencies if needed
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.1") // Or whatever version MobileIDE uses
}