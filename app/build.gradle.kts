plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.dokka)
}

val copyWebAppApk = tasks.register<Copy>("copyWebAppApk") {
    // Explicitly depend on the webapp build task
    dependsOn(":webapp:assembleRelease")

    // Set input source: webapp output directory
    // Note: Use provider mechanism to ensure the path is resolved only during execution
    from(project(":webapp").layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
        // If you need to specify an exact name, you can use rename
        // rename { "webapp.apk" }
        // Or keep the original name, or rename it as you did before
        rename { _ -> "webapp.apk" }
    }

    // Set output destination: app module build directory (do not pollute src directory)
    into(layout.buildDirectory.dir("generated/assets/webapp"))
}

android {
    namespace  = libs.versions.applicationId.get()
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = libs.versions.applicationId.get()
        minSdk        = libs.versions.minSdk.get().toInt()
        targetSdk     = libs.versions.targetSdk.get().toInt()
        versionCode   = libs.versions.versionCode.get().toInt()
        versionName   = libs.versions.versionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("WebIDE.jks")
            keyAlias = "WebIDE"
            storePassword = "WebIDE"
            keyPassword = "WebIDE"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-beta"
            isDebuggable = true
            signingConfig = signingConfigs.getByName("release")
        }
        release {
           // applicationIdSuffix = ".release"
            isMinifyEnabled = true
            isShrinkResources = true // Resource shrinking
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility            = JavaVersion.VERSION_17
        targetCompatibility            = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
    //kotlin { jvmToolchain(libs.versions.java.get()) }
    //kotlin { jvmToolchain(17) }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            // Key point: Add copyWebAppApk task as a directory source
            // Gradle will automatically identify: copyWebAppApk task must run before packaging assets
            assets.srcDir(copyWebAppApk)
        }
    }

    // Required for Sora Editor's textmate grammar files
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

android.applicationVariants.configureEach {
    outputs.configureEach {
        val appName = "MobileIDE"
        val buildType = buildType.name
        val ver = versionName
        (this as? com.android.build.gradle.internal.api.ApkVariantOutputImpl)?.let {
            it.outputFileName = "${appName}-${ver}-${buildType}.apk"
        }
    }
}

aboutLibraries() {
    export {
        prettyPrint = true
        outputFile = file("src/main/res/raw/aboutlibraries.json")
    }
}

dependencies {
    //implementation(project(":signer"))
    
    implementation(project(":core:build"))
    implementation(project(":core:files"))
    implementation(project(":core:projects"))
    implementation(project(":core:resources"))
    implementation(project(":core:ui"))
    implementation(project(":core:utils"))
    
    //implementation("com.google.accompanist:accompanist-navigation-animation:0.36.0")
    implementation(libs.accompanist.navigation.animation)

    //implementation("com.mikepenz:aboutlibraries-compose:13.1.0")
    implementation(libs.aboutlibraries.compose)
    
    val editorVersion = "0.24.0"
    implementation("io.github.rosemoe:editor:$editorVersion")
    implementation("io.github.rosemoe:language-textmate:$editorVersion")
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    
    // Hilt dependencies
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    testImplementation(libs.junit)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}