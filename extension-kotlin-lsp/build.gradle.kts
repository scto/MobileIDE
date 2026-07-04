plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rk.extension.kotlin_lsp"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compile against the MobileIDE APIs
    compileOnly(project(":core:extension"))
    compileOnly(project(":core:lsp"))
    compileOnly(project(":core:commands"))
    
    // Additional dependencies if needed
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.1") // Or whatever version MobileIDE uses
}
