/*
 * MobileIDE - A powerful IDE for Android app development.
 * Copyright (C) 2025  scto  <tschmid35@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 */

package com.scto.mobile.ide.ui.projects

object ProjectTemplates {

    val libsVersionsToml =
        """
        [versions]
        agp = "8.2.2"
        kotlin = "1.9.22"
        coreKtx = "1.12.0"
        junit = "4.13.2"
        junitVersion = "1.1.5"
        espressoCore = "3.5.1"
        lifecycleRuntimeKtx = "2.7.0"
        activityCompose = "1.8.2"
        composeBom = "2023.08.00"
        appcompat = "1.6.1"
        material = "1.11.0"
        navigationCompose = "2.7.7"

        [libraries]
        androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
        junit = { group = "junit", name = "junit", version.ref = "junit" }
        androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
        androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
        androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
        androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
        androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
        androidx-ui = { group = "androidx.compose.ui", name = "ui" }
        androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
        androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
        androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
        androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
        androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
        androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
        androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
        appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
        material = { group = "com.google.android.material", name = "material", version.ref = "material" }

        [plugins]
        android-application = { id = "com.android.application", version.ref = "agp" }
        kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
        """
            .trimIndent()

    val gradlewScript =
        """
        #!/usr/bin/env bash

        # Attempt to run local gradle wrapper
        APP_HOME="${'$'}(cd "${"$"}(dirname "${"$"}(readlink -f "${"$"}(type -P "gradlew" || echo "${"$"}0")"))" && pwd)"
        CLASSPATH="${"$"}/gradle/wrapper/gradle-wrapper.jar"

        if [ -x "${"$"}/gradlew" ] && [ -f "${"$"}/gradle/wrapper/gradle-wrapper.jar" ]; then
            exec java -classpath "${"$"}/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "${"$"}@"
        else
            echo "Gradle wrapper not fully installed. Trying system gradle..."
            exec gradle "${"$"}@"
        fi
        """
            .trimIndent()

    val gradleProperties =
        """
        org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
        android.useAndroidX=true
        android.nonTransitiveRClass=true
        kotlin.code.style=official
        """
            .trimIndent()

    fun getSettingsGradle(projectName: String): String {
        return """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "$projectName"
            include(":app")
        """
            .trimIndent()
    }

    fun getRootBuildGradle(useKotlinDsl: Boolean): String {
        return if (useKotlinDsl) {
            """
            plugins {
                alias(libs.plugins.android.application) apply false
                alias(libs.plugins.kotlin.android) apply false
            }
            """.trimIndent()
        } else {
            """
            plugins {
                alias libs.plugins.android.application apply false
                alias libs.plugins.kotlin.android apply false
            }
            """.trimIndent()
        }
    }

    fun getAppBuildGradleCompose(
        packageName: String,
        addNavigation: Boolean = false,
        minSdk: Int = 24,
        targetSdk: Int = 34,
        useKotlinDsl: Boolean = true
    ): String {
        return if (useKotlinDsl) {
            """
            plugins {
                alias(libs.plugins.android.application)
                alias(libs.plugins.kotlin.android)
            }

            android {
                namespace = "$packageName"
                compileSdk = $targetSdk

                defaultConfig {
                    applicationId = "$packageName"
                    minSdk = $minSdk
                    targetSdk = $targetSdk
                    versionCode = 1
                    versionName = "1.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
                kotlinOptions {
                    jvmTarget = "1.8"
                }
                buildFeatures {
                    compose = true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion = "1.5.8"
                }
                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
            }

            dependencies {
                implementation(libs.androidx.core-ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(platform(libs.androidx.compose.bom))
                implementation(libs.androidx.ui)
                implementation(libs.androidx.ui.graphics)
                implementation(libs.androidx.ui.tooling.preview)
                implementation(libs.androidx.material3)
                ${if (addNavigation) "implementation(libs.androidx.navigation.compose)" else ""}
                
                testImplementation(libs.junit)
                androidTestImplementation(libs.androidx.junit)
                androidTestImplementation(libs.androidx.espresso.core)
                androidTestImplementation(platform(libs.androidx.compose.bom))
                androidTestImplementation(libs.androidx.ui.test.junit4)
                debugImplementation(libs.androidx.ui.tooling)
                debugImplementation(libs.androidx.ui.test.manifest)
            }
            """.trimIndent()
        } else {
            """
            plugins {
                alias libs.plugins.android.application
                alias libs.plugins.kotlin.android
            }

            android {
                namespace '$packageName'
                compileSdk $targetSdk

                defaultConfig {
                    applicationId '$packageName'
                    minSdk $minSdk
                    targetSdk $targetSdk
                    versionCode 1
                    versionName '1.0'
                    testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'
                    vectorDrawables {
                        useSupportLibrary true
                    }
                }

                buildTypes {
                    release {
                        minifyEnabled false
                        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                    }
                }
                compileOptions {
                    sourceCompatibility JavaVersion.VERSION_1_8
                    targetCompatibility JavaVersion.VERSION_1_8
                }
                kotlinOptions {
                    jvmTarget = '1.8'
                }
                buildFeatures {
                    compose true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion '1.5.8'
                }
                packaging {
                    resources {
                        excludes += '/META-INF/{AL2.0,LGPL2.1}'
                    }
                }
            }

            dependencies {
                implementation libs.androidx.core.ktx
                implementation libs.androidx.lifecycle.runtime.ktx
                implementation libs.androidx.activity.compose
                implementation platform(libs.androidx.compose.bom)
                implementation libs.androidx.ui
                implementation libs.androidx.ui.graphics
                implementation libs.androidx.ui.tooling.preview
                implementation libs.androidx.material3
                ${if (addNavigation) "implementation libs.androidx.navigation.compose" else ""}
                
                testImplementation libs.junit
                androidTestImplementation libs.androidx.junit
                androidTestImplementation libs.androidx.espresso.core
                androidTestImplementation platform(libs.androidx.compose.bom)
                androidTestImplementation libs.androidx.ui.test.junit4
                debugImplementation libs.androidx.ui.tooling
                debugImplementation libs.androidx.ui.test.manifest
            }
            """.trimIndent()
        }
    }

    fun getAppBuildGradleCmake(
        packageName: String,
        minSdk: Int = 24,
        targetSdk: Int = 34,
        useKotlinDsl: Boolean = true
    ): String {
        return if (useKotlinDsl) {
            """
            plugins {
                alias(libs.plugins.android.application)
                alias(libs.plugins.kotlin.android)
            }

            android {
                namespace = "$packageName"
                compileSdk = $targetSdk

                defaultConfig {
                    applicationId = "$packageName"
                    minSdk = $minSdk
                    targetSdk = $targetSdk
                    versionCode = 1
                    versionName = "1.0"
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    
                    externalNativeBuild {
                        cmake {
                            cppFlags("")
                        }
                    }
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_1_8
                    targetCompatibility = JavaVersion.VERSION_1_8
                }
                kotlinOptions {
                    jvmTarget = "1.8"
                }
                buildFeatures {
                    compose = true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion = "1.5.8"
                }
                externalNativeBuild {
                    cmake {
                        path = file("CMakeLists.txt")
                        version = "3.22.1"
                    }
                }
            }

            dependencies {
                implementation(libs.androidx.core-ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(platform(libs.androidx.compose.bom))
                implementation(libs.androidx.ui)
                implementation(libs.androidx.ui.graphics)
                implementation(libs.androidx.ui.tooling.preview)
                implementation(libs.androidx.material3)
            }
            """.trimIndent()
        } else {
            """
            plugins {
                alias libs.plugins.android.application
                alias libs.plugins.kotlin.android
            }

            android {
                namespace '$packageName'
                compileSdk $targetSdk

                defaultConfig {
                    applicationId '$packageName'
                    minSdk $minSdk
                    targetSdk $targetSdk
                    versionCode 1
                    versionName '1.0'
                    testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'
                    
                    externalNativeBuild {
                        cmake {
                            cppFlags ''
                        }
                    }
                }

                buildTypes {
                    release {
                        minifyEnabled false
                        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                    }
                }
                compileOptions {
                    sourceCompatibility JavaVersion.VERSION_1_8
                    targetCompatibility JavaVersion.VERSION_1_8
                }
                kotlinOptions {
                    jvmTarget = '1.8'
                }
                buildFeatures {
                    compose true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion '1.5.8'
                }
                externalNativeBuild {
                    cmake {
                        path file('CMakeLists.txt')
                        version '3.22.1'
                    }
                }
            }

            dependencies {
                implementation libs.androidx.core.ktx
                implementation libs.androidx.lifecycle.runtime.ktx
                implementation libs.androidx.activity.compose
                implementation platform(libs.androidx.compose.bom)
                implementation libs.androidx.ui
                implementation libs.androidx.ui.graphics
                implementation libs.androidx.ui.tooling.preview
                implementation libs.androidx.material3
            }
            """.trimIndent()
        }
    }

    fun getAndroidManifest(packageName: String): String {
        return """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application
                android:allowBackup="true"
                android:icon="@mipmap/ic_launcher"
                android:label="@string/app_name"
                android:roundIcon="@mipmap/ic_launcher_round"
                android:supportsRtl="true"
                android:theme="@style/Theme.MyApplication">
                <activity
                    android:name=".MainActivity"
                    android:exported="true">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
            </application>
        </manifest>
        """
            .trimIndent()
    }

    val themesXml =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <resources>
            <style name="Theme.MyApplication" parent="android:Theme.Material.Light.NoActionBar" />
        </resources>
        """
            .trimIndent()

    fun getStringsXml(projectName: String): String {
        return """
            <resources>
                <string name="app_name">$projectName</string>
            </resources>
        """
            .trimIndent()
    }

    fun getEmptyComposeMainActivity(packageName: String): String {
        return """
            package $packageName

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.Surface
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import $packageName.ui.theme.AppTheme

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent {
                        AppTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Greeting("Compose")
                            }
                        }
                    }
                }
            }

            @Composable
            fun Greeting(name: String, modifier: Modifier = Modifier) {
                Text(
                    text = "Hello ${'$'}name!",
                    modifier = modifier
                )
            }
        """
            .trimIndent()
    }

    fun getBasicComposeMainActivity(packageName: String): String {
        return """
            package $packageName

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp
            import $packageName.ui.theme.AppTheme

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent {
                        AppTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                CounterApp()
                            }
                        }
                    }
                }
            }

            @Composable
            fun CounterApp() {
                var count by remember { mutableStateOf(0) }
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Count: ${'$'}count",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { count++ }) {
                        Text("Increment")
                    }
                }
            }
        """
            .trimIndent()
    }

    fun getBottomNavigationMainActivity(packageName: String): String {
        return """
            package $packageName

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.*
            import androidx.compose.material.icons.Icons
            import androidx.compose.material.icons.filled.Home
            import androidx.compose.material.icons.filled.Person
            import androidx.compose.material.icons.filled.Settings
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.graphics.vector.ImageVector
            import $packageName.ui.theme.AppTheme

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent {
                        AppTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                BottomNavigationApp()
                            }
                        }
                    }
                }
            }

            @Composable
            fun BottomNavigationApp() {
                var selectedTab by remember { mutableStateOf(0) }
                val items = listOf("Home", "Profile", "Settings")
                val icons = listOf(Icons.Default.Home, Icons.Default.Person, Icons.Default.Settings)

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    icon = { Icon(icons[index], contentDescription = item) },
                                    label = { Text(item) },
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${'$'}{items[selectedTab]} Screen",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
            }
        """
            .trimIndent()
    }

    fun getNavigationDrawerMainActivity(packageName: String): String {
        return """
            package $packageName

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.*
            import androidx.compose.material.icons.Icons
            import androidx.compose.material.icons.filled.Menu
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp
            import kotlinx.coroutines.launch
            import $packageName.ui.theme.AppTheme

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent {
                        AppTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                NavigationDrawerApp()
                            }
                        }
                    }
                }
            }

            @Composable
            fun NavigationDrawerApp() {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var selectedItem by remember { mutableStateOf("Home") }
                val items = listOf("Home", "Gallery", "Settings")

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(12.dp))
                            items.forEach { item ->
                                NavigationDrawerItem(
                                    label = { Text(item) },
                                    selected = item == selectedItem,
                                    onClick = {
                                        selectedItem = item
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(
                                title = { Text("Drawer App") },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${'$'}selectedItem Content",
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }
                }
            }
        """
            .trimIndent()
    }

    val flutterPubspec =
        """
        name: flutter_app
        description: A new Flutter project generated by MobileIDE.
        publish_to: 'none'
        version: 1.0.0+1

        environment:
          sdk: '>=3.0.0 <4.0.0'

        dependencies:
          flutter:
            sdk: flutter
          cupertino_icons: ^1.0.6

        dev_dependencies:
          flutter_test:
            sdk: flutter
          flutter_lints: ^3.0.0

        flutter:
          uses-material-design: true
        """
            .trimIndent()

    val flutterMainDart =
        """
        import 'package:flutter/material.dart';

        void main() {
          runApp(const MyApp());
        }

        class MyApp extends StatelessWidget {
          const MyApp({super.key});

          @override
          Widget build(BuildContext context) {
            return MaterialApp(
              title: 'Flutter Demo',
              theme: ThemeData(
                colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
                useMaterial3: true,
              ),
              home: const MyHomePage(title: 'Flutter Counter Home'),
            );
          }
        }

        class MyHomePage extends StatefulWidget {
          const MyHomePage({super.key, required this.title});

          final String title;

          @override
          State<MyHomePage> createState() => _MyHomePageState();
        }

        class _MyHomePageState extends State<MyHomePage> {
          int _counter = 0;

          void _incrementCounter() {
            setState(() {
              _counter++;
            });
          }

          @override
          Widget build(BuildContext context) {
            return Scaffold(
              appBar: AppBar(
                backgroundColor: Theme.of(context).colorScheme.inversePrimary,
                title: Text(widget.title),
              ),
              body: Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    const Text('You have pushed the button this many times:'),
                    Text(
                      '${"$"}_counter',
                      style: Theme.of(context).textTheme.headlineMedium,
                    ),
                  ],
                ),
              ),
              floatingActionButton: FloatingActionButton(
                onPressed: _incrementCounter,
                tooltip: 'Increment',
                child: const Icon(Icons.add),
              ),
            );
          }
        }
        """
            .trimIndent()

    val flutterAndroidBuildGradle =
        """
        buildscript {
            ext.kotlin_version = '1.9.0'
            repositories {
                google()
                mavenCentral()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:8.2.2'
                classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${"$"}/kotlin_version"
            }
        }
        allprojects {
            repositories {
                google()
                mavenCentral()
            }
        }
        """
            .trimIndent()

    val flutterAndroidSettingsGradle =
        """
        include ':app'
        """
            .trimIndent()

    val flutterAndroidAppBuildGradle =
        """
        plugins {
            id 'com.android.application'
            id 'kotlin-android'
        }

        android {
            namespace "com.example.flutterapp"
            compileSdk 34

            defaultConfig {
                applicationId "com.example.flutterapp"
                minSdk 21
                targetSdk 34
                versionCode 1
                versionName "1.0"
            }
        }
        """
            .trimIndent()

    fun getFlutterAndroidManifest(packageName: String): String {
        return """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="$packageName">
                <application
                    android:label="flutter_app"
                    android:name="android.app.Application"
                    android:icon="@mipmap/ic_launcher">
                    <activity
                        android:name="io.flutter.embedding.android.FlutterActivity"
                        android:exported="true"
                        android:theme="@android:style/Theme.Black.NoTitleBar"
                        android:configChanges="orientation|keyboardHidden|keyboard|screenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
                        android:hardwareAccelerated="true"
                        android:windowSoftInputMode="adjustResize">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN"/>
                            <category android:name="android.intent.category.LAUNCHER"/>
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """
            .trimIndent()
    }

    fun getFlutterMainActivity(packageName: String): String {
        return """
            package $packageName

            import io.flutter.embedding.android.FlutterActivity

            class MainActivity: FlutterActivity() {
            }
        """
            .trimIndent()
    }

    val cmakeLists =
        """
        cmake_minimum_required(VERSION 3.22.1)
        project("native-lib")

        add_library(
            native-lib
            SHARED
            native-lib.cpp
        )

        find_library(
            log-lib
            log
        )

        target_link_libraries(
            native-lib
            ${"$"}{log-lib}
        )
        """
            .trimIndent()

    fun getNativeLibCpp(packageName: String): String {
        val jniFunctionName = "Java_" + packageName.replace(".", "_") + "_MainActivity_stringFromJNI"
        return """
            #include <jni.h>
            #include <string>

            extern "C" JNIEXPORT jstring JNICALL
            ${jniFunctionName}(
                JNIEnv* env,
                jobject /* this */) {
                std::string hello = "Hello from Native C++ !!";
                return env->NewStringUTF(hello.c_str());
            }
        """
            .trimIndent()
    }

    fun getCmakeMainActivity(packageName: String): String {
        return """
            package $packageName

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.Surface
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent {
                        MaterialTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Greeting(stringFromJNI())
                            }
                        }
                    }
                }

                external fun stringFromJNI(): String

                companion object {
                    init {
                        System.loadLibrary("native-lib")
                    }
                }
            }

            @Composable
            fun Greeting(name: String, modifier: Modifier = Modifier) {
                Text(
                    text = name,
                    modifier = modifier
                )
            }
        """
            .trimIndent()
    }

    fun getColorKt(packageName: String): String {
        return """
            package $packageName.ui.theme

            import androidx.compose.ui.graphics.Color

            val Purple80 = Color(0xFFD0BCFF)
            val PurpleGrey80 = Color(0xFFCCC2DC)
            val Pink80 = Color(0xFFEFB8C8)

            val Purple40 = Color(0xFF6650a4)
            val PurpleGrey40 = Color(0xFF625b71)
            val Pink40 = Color(0xFF7D5260)
        """
            .trimIndent()
    }

    fun getTypeKt(packageName: String): String {
        return """
            package $packageName.ui.theme

            import androidx.compose.ui.text.TextStyle
            import androidx.compose.ui.text.font.FontFamily
            import androidx.compose.ui.text.font.FontWeight
            import androidx.compose.ui.unit.sp
            import androidx.compose.material3.Typography

            val Typography = Typography(
                bodyLarge = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp
                )
            )
        """
            .trimIndent()
    }

    fun getThemeKt(packageName: String): String {
        return """
            package $packageName.ui.theme

            import android.app.Activity
            import android.os.Build
            import androidx.compose.foundation.isSystemInDarkTheme
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.darkColorScheme
            import androidx.compose.material3.dynamicDarkColorScheme
            import androidx.compose.material3.dynamicLightColorScheme
            import androidx.compose.material3.lightColorScheme
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.SideEffect
            import androidx.compose.ui.graphics.toArgb
            import androidx.compose.ui.platform.LocalContext
            import androidx.compose.ui.platform.LocalView
            import androidx.core.view.WindowCompat

            private val DarkColorScheme = darkColorScheme(
                primary = Purple80,
                secondary = PurpleGrey80,
                tertiary = Pink80
            )

            private val LightColorScheme = lightColorScheme(
                primary = Purple40,
                secondary = PurpleGrey40,
                tertiary = Pink40
            )

            @Composable
            fun AppTheme(
                darkTheme: Boolean = isSystemInDarkTheme(),
                dynamicColor: Boolean = true,
                content: @Composable () -> Unit
            ) {
                val colorScheme = when {
                    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        val context = LocalContext.current
                        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    }
                    darkTheme -> DarkColorScheme
                    else -> LightColorScheme
                }
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor = colorScheme.primary.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
                    }
                }

                MaterialTheme(
                    colorScheme = colorScheme,
                    typography = Typography,
                    content = content
                )
            }
        """
            .trimIndent()
    }

    fun downloadGradle(
        context: android.content.Context,
        version: String = "8.5",
        onProgress: (Float) -> Unit = {},
        onSuccess: (java.io.File) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val cacheDir = context.cacheDir
        val gradleZip = java.io.File(cacheDir, "gradle-$version-bin.zip")
        if (gradleZip.exists() && gradleZip.length() > 10 * 1024 * 1024) {
            onSuccess(gradleZip)
            return
        }

        val distributionUrl = "https://services.gradle.org/distributions/gradle-$version-bin.zip"
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL(distributionUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connect()

                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    throw java.io.IOException("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                }

                val fileLength = connection.contentLength
                url.openStream().use { input ->
                    java.io.FileOutputStream(gradleZip).use { output ->
                        val data = ByteArray(4096)
                        var total: Long = 0
                        var count: Int
                        while (input.read(data).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    onProgress(total.toFloat() / fileLength)
                                }
                            }
                            output.write(data, 0, count)
                        }
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess(gradleZip)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }
}
