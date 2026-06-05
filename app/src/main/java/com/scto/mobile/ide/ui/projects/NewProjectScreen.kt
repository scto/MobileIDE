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

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.scto.mobile.ide.R
import com.scto.mobile.ide.core.utils.WorkspaceManager
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProjectType {
    BASIC_COMPOSE_ACTIVITY,
    EMPTY_COMPOSE_ACTIVITY,
    BOTTOM_NAVIGATION,
    NAVIGATION_DRAWER_ACTIVITY,
    FLUTTER_APP,
    CMAKE_APP,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(navController: NavController) {
    var projectName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ProjectType.BASIC_COMPOSE_ACTIVITY) }
    var packageName by remember { mutableStateOf("com.example.myapp") }

    var isScreenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isScreenVisible = true }

    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun handleCreate() {
        if (projectName.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.new_project_enter_name_error)) }
            return
        }

        isLoading = true
        focusManager.clearFocus()

        createNewProject(
            context,
            projectName,
            packageName,
            selectedType,
            onSuccess = { dir ->
                isLoading = false
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.new_project_created, dir.name))
                    delay(800)
                    navController.popBackStack()
                }
            },
            onError = { msg ->
                isLoading = false
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.new_project_title), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isScreenVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Surface(modifier = Modifier.fillMaxWidth().imePadding(), color = MaterialTheme.colorScheme.background) {
                    BouncyButton(
                        onClick = { handleCreate() },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding().height(54.dp),
                    ) {
                        if (isLoading)
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else
                            Text(
                                stringResource(R.string.new_project_create_now),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                    }
                }
            }
        },
    ) { innerPadding ->
        AnimatedVisibility(
            visible = isScreenVisible,
            enter =
                slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                    fadeIn(tween(500)),
            modifier = Modifier.padding(innerPadding),
        ) {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(scrollState)
                        .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                        .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    stringResource(R.string.new_project_type_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MinimalTypeCard(
                            "Basic Compose",
                            Icons.Default.Android,
                            selectedType == ProjectType.BASIC_COMPOSE_ACTIVITY,
                            Modifier.weight(1f),
                        ) {
                            selectedType = ProjectType.BASIC_COMPOSE_ACTIVITY
                        }
                        MinimalTypeCard(
                            "Empty Compose",
                            Icons.Default.Layers,
                            selectedType == ProjectType.EMPTY_COMPOSE_ACTIVITY,
                            Modifier.weight(1f),
                        ) {
                            selectedType = ProjectType.EMPTY_COMPOSE_ACTIVITY
                        }
                        MinimalTypeCard(
                            "Bottom Nav",
                            Icons.Default.Menu,
                            selectedType == ProjectType.BOTTOM_NAVIGATION,
                            Modifier.weight(1f),
                        ) {
                            selectedType = ProjectType.BOTTOM_NAVIGATION
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MinimalTypeCard(
                            "Drawer Nav",
                            Icons.Default.VerticalSplit,
                            selectedType == ProjectType.NAVIGATION_DRAWER_ACTIVITY,
                            Modifier.weight(1f),
                        ) {
                            selectedType = ProjectType.NAVIGATION_DRAWER_ACTIVITY
                        }
                        MinimalTypeCard(
                            "Flutter App",
                            Icons.Default.Code,
                            selectedType == ProjectType.FLUTTER_APP,
                            Modifier.weight(1f),
                        ) {
                            selectedType = ProjectType.FLUTTER_APP
                        }
                        MinimalTypeCard(
                            "CMake C++",
                            Icons.Default.Build,
                            selectedType == ProjectType.CMAKE_APP,
                            Modifier.weight(1f),
                        ) {
                            selectedType = ProjectType.CMAKE_APP
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                CleanTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        if (packageName.startsWith("com.example.")) {
                            val clean = it.filter { c -> c.isLetter() }.lowercase(Locale.ROOT)
                            if (clean.isNotEmpty()) {
                                packageName = "com.example.$clean"
                            }
                        }
                    },
                    placeholder = stringResource(R.string.new_project_name),
                    icon = Icons.Outlined.Edit,
                )

                Spacer(modifier = Modifier.height(16.dp))
                CleanTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    placeholder = stringResource(R.string.new_project_package_name),
                    icon = Icons.Outlined.AlternateEmail,
                    keyboardType = KeyboardType.Ascii,
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "buttonScale")

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = CircleShape,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun MinimalTypeCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg by
        animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
            label = "bg",
        )
    val contentColor by
        animateColorAsState(
            if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "content",
        )
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f, label = "scale")

    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = contentColor)
    }
}

@Composable
fun CleanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector?,
    keyboardType: KeyboardType = KeyboardType.Text,
    isSmall: Boolean = false,
    isPassword: Boolean = false,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                placeholder,
                style = if (isSmall) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        },
        leadingIcon =
            if (icon != null) {
                {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isSmall) 18.dp else 24.dp),
                    )
                }
            } else null,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        textStyle = if (isSmall) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
    )
}

@OptIn(DelicateCoroutinesApi::class)
private fun createNewProject(
    context: Context,
    name: String,
    packageName: String,
    type: ProjectType,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit,
) {
    val wsPath = WorkspaceManager.getWorkspacePath(context)
    val appPkg = context.packageName

    GlobalScope.launch(Dispatchers.IO) {
        try {
            val parentDir =
                if (wsPath.contains("/Android/data/$appPkg")) context.getExternalFilesDir(null)!! else File(wsPath)
            val projectDir = File(parentDir, name)
            if (projectDir.exists()) {
                withContext(Dispatchers.Main) { onError(context.getString(R.string.new_project_exists)) }
                return@launch
            }
            projectDir.mkdirs()

            setupGradleWrapper(projectDir, context)
            setupTomlVersions(projectDir)

            when (type) {
                ProjectType.BASIC_COMPOSE_ACTIVITY -> createBasicComposeStructure(projectDir, name, packageName)
                ProjectType.EMPTY_COMPOSE_ACTIVITY -> createEmptyComposeStructure(projectDir, name, packageName)
                ProjectType.BOTTOM_NAVIGATION -> createBottomNavigationStructure(projectDir, name, packageName)
                ProjectType.NAVIGATION_DRAWER_ACTIVITY -> createNavigationDrawerStructure(projectDir, name, packageName)
                ProjectType.FLUTTER_APP -> createFlutterStructure(projectDir, name, packageName)
                ProjectType.CMAKE_APP -> createCmakeStructure(projectDir, name, packageName)
            }

            withContext(Dispatchers.Main) { onSuccess(projectDir) }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onError(e.message ?: context.getString(R.string.new_project_unknown_error))
            }
        }
    }
}

private fun setupGradleWrapper(dir: File, context: Context) {
    val gradleDir = File(dir, "gradle/wrapper")
    gradleDir.mkdirs()

    val sourceJar = File("/data/data/com.termux/files/home/MobileIDE/gradle/wrapper/gradle-wrapper.jar")
    val sourceProps = File("/data/data/com.termux/files/home/MobileIDE/gradle/wrapper/gradle-wrapper.properties")
    val sourceGradlew = File("/data/data/com.termux/files/home/MobileIDE/gradlew")
    val sourceGradlewBat = File("/data/data/com.termux/files/home/MobileIDE/gradlew.bat")

    val targetJar = File(gradleDir, "gradle-wrapper.jar")
    val targetProps = File(gradleDir, "gradle-wrapper.properties")
    val targetGradlew = File(dir, "gradlew")
    val targetGradlewBat = File(dir, "gradlew.bat")

    var copiedLocal = false
    try {
        if (sourceJar.exists() && sourceProps.exists() && sourceGradlew.exists()) {
            sourceJar.copyTo(targetJar, overwrite = true)
            sourceProps.copyTo(targetProps, overwrite = true)
            sourceGradlew.copyTo(targetGradlew, overwrite = true)
            if (sourceGradlewBat.exists()) {
                sourceGradlewBat.copyTo(targetGradlewBat, overwrite = true)
            }
            targetGradlew.setExecutable(true)
            copiedLocal = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (!copiedLocal) {
        safeWrite(
            targetProps,
            """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
            """
                .trimIndent(),
        )

        safeWrite(targetGradlew, ProjectTemplates.gradlewScript)
        targetGradlew.setExecutable(true)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url =
                    URL("https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar")
                url.openStream().use { input -> FileOutputStream(targetJar).use { output -> input.copyTo(output) } }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun setupTomlVersions(dir: File) {
    val gradleDir = File(dir, "gradle")
    gradleDir.mkdirs()
    val tomlFile = File(gradleDir, "libs.versions.toml")
    safeWrite(tomlFile, ProjectTemplates.libsVersionsToml)
}

data class AndroidDirs(val appDir: File, val mainDir: File, val resDir: File, val valuesDir: File, val javaDir: File)

private fun setupCommonAndroidDirs(dir: File, packageName: String): AndroidDirs {
    val appDir = File(dir, "app")
    val mainDir = File(appDir, "src/main")
    val resDir = File(mainDir, "res")
    val valuesDir = File(resDir, "values")
    val packagePath = packageName.replace('.', '/')
    val javaDir = File(mainDir, "java/$packagePath")
    javaDir.mkdirs()
    valuesDir.mkdirs()
    return AndroidDirs(appDir, mainDir, resDir, valuesDir, javaDir)
}

private fun createEmptyComposeStructure(dir: File, name: String, packageName: String) {
    val dirs = setupCommonAndroidDirs(dir, packageName)
    safeWrite(File(dir, "settings.gradle.kts"), ProjectTemplates.getSettingsGradle(name))
    safeWrite(File(dir, "build.gradle.kts"), ProjectTemplates.rootBuildGradleAndroid)
    safeWrite(File(dir, "gradle.properties"), ProjectTemplates.gradleProperties)

    safeWrite(File(dirs.appDir, "build.gradle.kts"), ProjectTemplates.getAppBuildGradleCompose(packageName, false))
    safeWrite(File(dirs.mainDir, "AndroidManifest.xml"), ProjectTemplates.getAndroidManifest(packageName))
    safeWrite(File(dirs.valuesDir, "strings.xml"), ProjectTemplates.getStringsXml(name))
    safeWrite(File(dirs.valuesDir, "themes.xml"), ProjectTemplates.themesXml)
    safeWrite(File(dirs.javaDir, "MainActivity.kt"), ProjectTemplates.getEmptyComposeMainActivity(packageName))
}

private fun createBasicComposeStructure(dir: File, name: String, packageName: String) {
    val dirs = setupCommonAndroidDirs(dir, packageName)
    safeWrite(File(dir, "settings.gradle.kts"), ProjectTemplates.getSettingsGradle(name))
    safeWrite(File(dir, "build.gradle.kts"), ProjectTemplates.rootBuildGradleAndroid)
    safeWrite(File(dir, "gradle.properties"), ProjectTemplates.gradleProperties)

    safeWrite(File(dirs.appDir, "build.gradle.kts"), ProjectTemplates.getAppBuildGradleCompose(packageName, false))
    safeWrite(File(dirs.mainDir, "AndroidManifest.xml"), ProjectTemplates.getAndroidManifest(packageName))
    safeWrite(File(dirs.valuesDir, "strings.xml"), ProjectTemplates.getStringsXml(name))
    safeWrite(File(dirs.valuesDir, "themes.xml"), ProjectTemplates.themesXml)
    safeWrite(File(dirs.javaDir, "MainActivity.kt"), ProjectTemplates.getBasicComposeMainActivity(packageName))
}

private fun createBottomNavigationStructure(dir: File, name: String, packageName: String) {
    val dirs = setupCommonAndroidDirs(dir, packageName)
    safeWrite(File(dir, "settings.gradle.kts"), ProjectTemplates.getSettingsGradle(name))
    safeWrite(File(dir, "build.gradle.kts"), ProjectTemplates.rootBuildGradleAndroid)
    safeWrite(File(dir, "gradle.properties"), ProjectTemplates.gradleProperties)

    safeWrite(File(dirs.appDir, "build.gradle.kts"), ProjectTemplates.getAppBuildGradleCompose(packageName, true))
    safeWrite(File(dirs.mainDir, "AndroidManifest.xml"), ProjectTemplates.getAndroidManifest(packageName))
    safeWrite(File(dirs.valuesDir, "strings.xml"), ProjectTemplates.getStringsXml(name))
    safeWrite(File(dirs.valuesDir, "themes.xml"), ProjectTemplates.themesXml)
    safeWrite(File(dirs.javaDir, "MainActivity.kt"), ProjectTemplates.getBottomNavigationMainActivity(packageName))
}

private fun createNavigationDrawerStructure(dir: File, name: String, packageName: String) {
    val dirs = setupCommonAndroidDirs(dir, packageName)
    safeWrite(File(dir, "settings.gradle.kts"), ProjectTemplates.getSettingsGradle(name))
    safeWrite(File(dir, "build.gradle.kts"), ProjectTemplates.rootBuildGradleAndroid)
    safeWrite(File(dir, "gradle.properties"), ProjectTemplates.gradleProperties)

    safeWrite(File(dirs.appDir, "build.gradle.kts"), ProjectTemplates.getAppBuildGradleCompose(packageName, false))
    safeWrite(File(dirs.mainDir, "AndroidManifest.xml"), ProjectTemplates.getAndroidManifest(packageName))
    safeWrite(File(dirs.valuesDir, "strings.xml"), ProjectTemplates.getStringsXml(name))
    safeWrite(File(dirs.valuesDir, "themes.xml"), ProjectTemplates.themesXml)
    safeWrite(File(dirs.javaDir, "MainActivity.kt"), ProjectTemplates.getNavigationDrawerMainActivity(packageName))
}

private fun createFlutterStructure(dir: File, name: String, packageName: String) {
    val libDir = File(dir, "lib")
    val androidDir = File(dir, "android")
    val packagePath = packageName.replace('.', '/')
    val androidAppDir = File(androidDir, "app")
    val androidMainDir = File(androidAppDir, "src/main")
    val androidKotlinDir = File(androidMainDir, "kotlin/$packagePath")
    val androidResValuesDir = File(androidMainDir, "res/values")

    libDir.mkdirs()
    androidKotlinDir.mkdirs()
    androidResValuesDir.mkdirs()

    safeWrite(File(dir, "pubspec.yaml"), ProjectTemplates.flutterPubspec)
    safeWrite(File(libDir, "main.dart"), ProjectTemplates.flutterMainDart)

    safeWrite(File(androidDir, "build.gradle"), ProjectTemplates.flutterAndroidBuildGradle)
    safeWrite(File(androidDir, "settings.gradle"), ProjectTemplates.flutterAndroidSettingsGradle)
    safeWrite(File(androidAppDir, "build.gradle"), ProjectTemplates.flutterAndroidAppBuildGradle)
    safeWrite(File(androidMainDir, "AndroidManifest.xml"), ProjectTemplates.getFlutterAndroidManifest(packageName))
    safeWrite(File(androidKotlinDir, "MainActivity.kt"), ProjectTemplates.getFlutterMainActivity(packageName))
    safeWrite(File(androidResValuesDir, "strings.xml"), ProjectTemplates.getStringsXml(name))
    safeWrite(File(androidResValuesDir, "themes.xml"), ProjectTemplates.themesXml)
}

private fun createCmakeStructure(dir: File, name: String, packageName: String) {
    val dirs = setupCommonAndroidDirs(dir, packageName)
    val cppDir = File(dirs.appDir, "src/main/cpp")
    cppDir.mkdirs()

    safeWrite(File(dir, "settings.gradle.kts"), ProjectTemplates.getSettingsGradle(name))
    safeWrite(File(dir, "build.gradle.kts"), ProjectTemplates.rootBuildGradleAndroid)
    safeWrite(File(dir, "gradle.properties"), ProjectTemplates.gradleProperties)

    safeWrite(File(dirs.appDir, "build.gradle.kts"), ProjectTemplates.getAppBuildGradleCmake(packageName))
    safeWrite(File(dirs.appDir, "CMakeLists.txt"), ProjectTemplates.cmakeLists)
    safeWrite(File(cppDir, "native-lib.cpp"), ProjectTemplates.getNativeLibCpp(packageName))

    safeWrite(File(dirs.mainDir, "AndroidManifest.xml"), ProjectTemplates.getAndroidManifest(packageName))
    safeWrite(File(dirs.valuesDir, "strings.xml"), ProjectTemplates.getStringsXml(name))
    safeWrite(File(dirs.valuesDir, "themes.xml"), ProjectTemplates.themesXml)
    safeWrite(File(dirs.javaDir, "MainActivity.kt"), ProjectTemplates.getCmakeMainActivity(packageName))
}

private fun safeWrite(file: File, content: String) {
    try {
        file.parentFile!!.mkdirs()
        file.writeText(content)
    } catch (_: Exception) {}
}
