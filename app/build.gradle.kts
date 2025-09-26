import java.util.Properties

plugins {
    alias(libs.plugins.mobileide.android.application)
    alias(libs.plugins.mobileide.android.compose)
    alias(libs.plugins.mobileide.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.mobileide"

    defaultConfig {
        val commit = getGitCommit()
        /*
        val githubToken = getSecretProperty("VCSPACE_TOKEN")
        val clientId = getSecretProperty("CLIENT_ID")
        val clientSecret = getSecretProperty("CLIENT_SECRET")
        val callbackUrl = getSecretProperty("OAUTH_REDIRECT_URL")
        */
        applicationId = "com.mobileide"
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "GIT_COMMIT", "\"$commit\"")
        /*
        buildConfigField("String", "GITHUB_TOKEN", "\"$githubToken\"")
        buildConfigField("String", "CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "CLIENT_SECRET", "\"$clientSecret\"")
        buildConfigField("String", "OAUTH_REDIRECT_URL", "\"$callbackUrl\"")
        */
    }

    signingConfigs {
        create("general") {
            storeFile = file("test.keystore")
            keyAlias = "test"
            keyPassword = "teixeira0x"
            storePassword = "teixeira0x"
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("general")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("general")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    kotlin {
        jvmToolchain(17)
    }
    
    compileOptions { isCoreLibraryDesugaringEnabled = true }

    packaging {
        resources.excludes.addAll(
            arrayOf(
                "META-INF/README.md",
                "META-INF/CHANGES",
                "bundle.properties",
                "plugin.properties"
            )
        )
    }

    lint {
        abortOnError = false
        disable += listOf("MaterialDesignInsteadOrbitDesign")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

detekt {
    config.setFrom("$projectDir/../config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:di"))
    implementation(project(":core:lsp-client"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:util"))
    
    implementation(project(":feature:debug"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:explorer"))
    implementation(project(":feature:git"))
    implementation(project(":feature:gradle"))
    implementation(project(":feature:home"))
    implementation(project(":feature:languages"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:projectpicker"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:sidepanel"))
    implementation(project(":feature:svgtoavd"))
    implementation(project(":feature:templates"))
    implementation(project(":feature:terminal"))
    implementation(project(":feature:termux"))
    implementation(project(":feature:xmltocompose"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.timber)
    
    implementation(libs.androidx.hilt.navigation.compose)

    // Permissions
    implementation(libs.accompanist.permissions)
    
    coreLibraryDesugaring(libs.androidx.desugar)
}

fun getGitCommit(): String {
    return try {
        val commit = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
        println("Git commit: $commit")
        commit
    } catch (_: Exception) {
        ""
    }
}

private fun getSecretProperty(name: String): String {
    val file = project.rootProject.file("token.properties")

    return if (file.exists()) {
        val properties = Properties().also { it.load(file.inputStream()) }
        properties.getProperty(name) ?: ""
    } else ""
}