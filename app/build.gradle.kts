/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.aboutlibraries)
}

// 1. Update the helper function to return a String directly or handle the provider
fun getGitInfo(vararg args: String): String {
    return try {
        // Execute and get the result immediately during configuration
        val output = providers.exec { commandLine("git", *args) }.standardOutput.asText.get()
        output.trim()
    } catch (e: Exception) {
        "unknown"
    }
}

// 2. Initialize variables
val gitCommitHash = getGitInfo("rev-parse", "--short=8", "HEAD")
val fullGitCommitHash = getGitInfo("rev-parse", "HEAD")
val gitCommitDate = getGitInfo("show", "-s", "--format=%cI", "HEAD")

// ACS_SIGNING_CONFIG_START
val keystorePropertiesFile = file("keystore.properties")
val acsProps =
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile
            .readLines()
            .filter { '=' in it }
            .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }
    } else emptyMap()

// ACS_SIGNING_CONFIG_END

android {
    namespace = libs.versions.applicationId.get()
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = libs.versions.applicationId.get()
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        vectorDrawables { useSupportLibrary = true }
    }

    androidResources {
        noCompress.add("tar.gz")
        noCompress.add("tar.xz")
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    signingConfigs {
        maybeCreate("release").apply {
            val sf = acsProps["storeFile"]
            val sp = acsProps["storePassword"]
            val ka = acsProps["keyAlias"]
            val kp = acsProps["keyPassword"]
            if (!sf.isNullOrBlank()) storeFile = file(sf)
            if (!sp.isNullOrBlank()) storePassword = sp
            if (!ka.isNullOrBlank()) keyAlias = ka
            if (!kp.isNullOrBlank()) keyPassword = kp
        }
    }

    buildTypes {
        release {
            buildConfigField("String", "GIT_COMMIT_HASH", "\"$fullGitCommitHash\"")
            buildConfigField("String", "GIT_SHORT_COMMIT_HASH", "\"$gitCommitHash\"")
            buildConfigField("String", "GIT_COMMIT_DATE", "\"$gitCommitDate\"")

            isMinifyEnabled = false
            isCrunchPngs = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            resValue("string", "app_name", "MobileIDE")
        }
        debug {
            buildConfigField("String", "GIT_COMMIT_HASH", "\"$fullGitCommitHash\"")
            buildConfigField("String", "GIT_SHORT_COMMIT_HASH", "\"$gitCommitHash\"")
            buildConfigField("String", "GIT_COMMIT_DATE", "\"$gitCommitDate\"")

            // applicationIdSuffix removed so data path matches release:
            // /data/user/0/com.scto.mobile.ide/files/ (not .debug)
            versionNameSuffix = "-DEBUG"
            resValue("string", "app_name", "MobileIDE")
        }
    }

    flavorDimensions += "store"

    productFlavors {
        create("Fdroid") {
            dimension = "store"
            targetSdk = 28
        }

        create("PlayStore") {
            dimension = "store"
            targetSdk = 35
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    packaging {
        jniLibs { useLegacyPackaging = true }
        resources {
            excludes.add("OSGI-INF/**")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/license.txt")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/notice.txt")
            excludes.add("META-INF/ASL2.0")
            excludes.add("META-INF/versions/**")
            excludes.add("**/MANIFEST.MF")
        }
    }
}

android.applicationVariants.configureEach {
    val variant = this
    outputs.all {
        val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
        val appName = "MobileIDE"
        val buildType = variant.buildType.name
        val versionName = variant.versionName

        output?.outputFileName = "${appName}-${versionName}-${buildType}.apk"
    }
}

aboutLibraries {
    collect { fetchRemoteLicense = true }
    export {
        prettyPrint = true
        outputFile = file("src/main/res/raw/aboutlibraries.json")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)

        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        freeCompilerArgs.add("-Xdeprecation=warn")
    }
}

fun downloadFile(localUrl: String, remoteUrl: String, expectedChecksum: String) {
    val digest = MessageDigest.getInstance("SHA-256")

    val file = File(projectDir, localUrl)
    if (file.exists()) {
        val buffer = ByteArray(8192)
        val input = FileInputStream(file)
        while (true) {
            val readBytes = input.read(buffer)
            if (readBytes < 0) break
            digest.update(buffer, 0, readBytes)
        }
        var checksum = BigInteger(1, digest.digest()).toString(16)
        while (checksum.length < 64) {
            checksum = "0$checksum"
        }
        if (checksum == expectedChecksum) {
            return
        } else {
            logger.warn(
                "Deleting old local file with wrong hash: $localUrl: expected: $expectedChecksum, actual: $checksum"
            )
            file.delete()
        }
    }

    logger.quiet("Downloading $remoteUrl ...")

    file.parentFile.mkdirs()
    val out = BufferedOutputStream(FileOutputStream(file))

    val connection = URI(remoteUrl).toURL().openConnection()
    val digestStream = DigestInputStream(connection.inputStream, digest)
    digestStream.transferTo(out)
    out.close()

    var checksum = BigInteger(1, digest.digest()).toString(16)
    while (checksum.length < 64) {
        checksum = "0$checksum"
    }
    if (checksum != expectedChecksum) {
        file.delete()
        throw GradleException("Wrong checksum for $remoteUrl:\n Expected: $expectedChecksum\n Actual:   $checksum")
    }
}

tasks.register("downloadPrebuilt") {
    doLast {
        val prootTag = "proot-2025.01.15-r2"
        val prootVersion = "5.1.107-66"
        var prootUrl =
            "https://github.com/termux-play-store/termux-packages/releases/download/${prootTag}/libproot-loader-ARCH-${prootVersion}.so"

        downloadFile(
            "src/main/jniLibs/armeabi-v7a/libproot-loader.so",
            prootUrl.replace("ARCH", "arm"),
            "eb1d64e9ef875039534ce7a8eeffa61bbc4c0ae5722cb48c9112816b43646a3e",
        )
        downloadFile(
            "src/main/jniLibs/arm64-v8a/libproot-loader.so",
            prootUrl.replace("ARCH", "aarch64"),
            "8814b72f760cd26afe5350a1468cabb6622b4871064947733fcd9cd06f1c8cb8",
        )
        downloadFile(
            "src/main/jniLibs/x86_64/libproot-loader.so",
            prootUrl.replace("ARCH", "x86_64"),
            "1a52cc9cc5fdecbf4235659ffeac8c51e4fefd7c75cc205f52d4884a3a0a0ba1",
        )
        prootUrl =
            "https://github.com/termux-play-store/termux-packages/releases/download/${prootTag}/libproot-loader32-ARCH-${prootVersion}.so"
        downloadFile(
            "src/main/jniLibs/arm64-v8a/libproot-loader32.so",
            prootUrl.replace("ARCH", "aarch64"),
            "ff56a5e3a37104f6778420d912e3edf31395c15d1528d28f0eb7d13a64481b99",
        )
        downloadFile(
            "src/main/jniLibs/x86_64/libproot-loader32.so",
            prootUrl.replace("ARCH", "x86_64"),
            "5460a597e473f57f0d33405891e35ca24709173ca0a38805d395e3544ab8b1b4",
        )
    }
}

afterEvaluate {
    android.applicationVariants.all { variant ->
        variant.javaCompileProvider.dependsOn("downloadPrebuilt")
        true
    }
}

dependencies {
    implementation(libs.material.kolor)
    implementation(libs.jsoup)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    implementation(libs.accompanist.navigation.animation)
    implementation(libs.compose.icons.simple)
    implementation(libs.compose.icons.font.awesome)

    implementation(libs.aboutlibraries.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.multiplatform.markdown.renderer.m3)
    implementation(libs.multiplatform.markdown.renderer.code)
    implementation(libs.multiplatform.markdown.renderer.coil2)
    implementation(libs.highlights)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui)

    // Git dependencies
    // Source: https://mvnrepository.com/artifact/org.eclipse.jgit/org.eclipse.jgit
    implementation(libs.org.eclipse.jgit)
    // Source: https://mvnrepository.com/artifact/org.eclipse.jgit/org.eclipse.jgit.ssh.apache
    implementation(libs.org.eclipse.jgit.ssh.apache)
    //noinspection UseTomlInstead
    implementation("org.slf4j:slf4j-simple:2.0.17")

    // Add terminal dependencies
    implementation(project(":core:main"))
    implementation(libs.semver)
    implementation(libs.androidsvg)

    // LSP support
    implementation(project(":editor-lsp"))
    implementation(libs.lsp4j)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.ui)

    // TreeSitter language packs
    //implementation(libs.tree.sitter)
    //implementation(libs.tree.sitter.json)
    implementation(libs.androidide.ts)
    implementation(libs.androidide.ts.java)
    implementation(libs.androidide.ts.json)
    implementation(libs.androidide.ts.kotlin)
    implementation(libs.androidide.ts.cpp)
    implementation(libs.androidide.ts.log)
    implementation(libs.androidide.ts.xml)
    // Media3 (Video Player)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // Editor
    implementation(project(":editor"))
    implementation(project(":language-treesitter"))
    implementation(project(":core:apk-builder"))
    implementation(libs.timber)
    implementation(libs.language.textmate)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // DataStore dependencies
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    implementation(files("libs/xml.jar"))

    implementation(libs.zipalign.java)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.volley)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material3)
    implementation(libs.appcompat)

    // Core Library Desugaring (translates newer Java APIs for older Android versions)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
