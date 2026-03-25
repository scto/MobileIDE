plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dokka)
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
    }

    buildTypes {
        release {
            isMinifyEnabled   = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
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

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    // Required for Sora Editor's textmate grammar files
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dokka 2.x — API documentation with Mermaid diagram support
//
// Generate:
//   ./gradlew :app:dokkaHtml   →  app/build/dokka/html/
//   ./gradlew :app:dokkaGfm    →  app/build/dokka/gfm/
// ─────────────────────────────────────────────────────────────────────────────

dokka {
    moduleName.set("MobileIDE")
    moduleVersion.set(libs.versions.versionName.get())

    dokkaSourceSets.configureEach {

        displayName.set("MobileIDE")

        // Do not warn about undocumented members
        reportUndocumented.set(false)

        // Include deprecated members in output
        skipDeprecated.set(false)

        // External API links — makes types clickable in the generated HTML
        externalDocumentationLinks.create("android") {
            url.set(uri("https://developer.android.com/reference/"))
            packageListUrl.set(uri("https://developer.android.com/reference/androidx/package-list"))
        }

        externalDocumentationLinks.create("kotlin-stdlib") {
            url.set(uri("https://kotlinlang.org/api/latest/jvm/stdlib/"))
        }

        externalDocumentationLinks.create("compose") {
            url.set(uri("https://developer.android.com/reference/kotlin/androidx/compose/"))
            packageListUrl.set(uri("https://developer.android.com/reference/kotlin/androidx/compose/package-list"))
        }

        externalDocumentationLinks.create("sora-editor") {
            url.set(uri("https://rosemoe.github.io/sora-editor/"))
        }

        // Module-level documentation entry page
        includes.from(project.file("dokka/module.md"))

        // Source root
        sourceRoots.from(file("src/main/java"))

        // Per-package options — Dokka 2.x uses perPackageOption { } (singular)
        perPackageOption {
            matchingRegex.set("com\\.mobileide\\.app\\.ui\\.screens.*")
            suppress.set(false)
            skipDeprecated.set(false)
        }
    }

    pluginsConfiguration.html {
        footerMessage.set(
            "© MobileIDE — Built with Kotlin ${libs.versions.kotlin.get()} · Dokka ${libs.versions.dokka.get()}"
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dependencies
// ─────────────────────────────────────────────────────────────────────────────

dependencies {
    testImplementation(libs.junit)
    // Dokka: Mermaid diagram rendering inside KDoc comments
    dokkaPlugin("com.glureau:html-mermaid-dokka-plugin:${libs.versions.dokkaMermaid.get()}")

    // ── Core ─────────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // ── Compose ──────────────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ── Async & Storage ──────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)

    // ── Sora Editor ──────────────────────────────────────────────────────────
    implementation(libs.sora.editor)
    implementation(libs.sora.language.textmate)
    implementation(libs.sora.language.java)

    // ── Splash Screen ────────────────────────────────────────────────────────
    implementation(libs.androidx.core.splashscreen)

    // ── Theme system ─────────────────────────────────────────────────────────
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.android.material:material:1.12.0")

    // ── Debug ────────────────────────────────────────────────────────────────
    debugImplementation(libs.androidx.ui.tooling)
}
