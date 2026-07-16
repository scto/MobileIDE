plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.scto.mobile.ide.plugin.kotlin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.scto.mobile.ide.plugin.kotlin"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    compileOnly(project(":core:extension"))
    compileOnly(project(":core:lsp"))
}

// ── Package .zip plugin ──────────────────────────────────────────────────────
val manifestFile = file("src/main/assets/manifest.json")

tasks.register<Zip>("packageZip") {
    group = "plugin"
    description = "Packages the Kotlin LSP plugin as a .zip file"

    dependsOn("assembleRelease")

    archiveFileName.set("com.scto.mobile.ide.kotlin_lsp.zip")
    destinationDirectory.set(file(System.getProperty("user.home")))

    from(manifestFile)

    val apkFile = layout.buildDirectory.file("outputs/apk/release/kotlin-lsp-release-unsigned.apk")
    from(apkFile) {
        rename { "extension.apk" }
    }
}
