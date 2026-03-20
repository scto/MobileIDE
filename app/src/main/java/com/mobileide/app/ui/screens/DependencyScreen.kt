package com.mobileide.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen
import java.io.File

// ── Popular dependency catalog ─────────────────────────────────────────────────
data class Dependency(
    val group: String,
    val artifact: String,
    val latestVersion: String,
    val description: String,
    val category: DepCategory,
    val gradleNotation: String = "implementation(\"$group:$artifact:$latestVersion\")"
)

enum class DepCategory(val label: String) {
    COMPOSE("Compose"),
    NETWORKING("Networking"),
    DATABASE("Database"),
    DI("DI"),
    TESTING("Testing"),
    UTILS("Utils"),
    IMAGE("Image"),
    MEDIA("Media"),
    SERIALIZATION("Serialization")
}

val POPULAR_DEPS = listOf(
    // Compose
    Dependency("androidx.compose", "compose-bom", "2024.06.00", "Compose Bill of Materials", DepCategory.COMPOSE,
        "implementation(platform(\"androidx.compose:compose-bom:2024.06.00\"))"),
    Dependency("androidx.navigation", "navigation-compose", "2.7.7", "Compose Navigation", DepCategory.COMPOSE),
    Dependency("androidx.lifecycle", "lifecycle-viewmodel-compose", "2.8.3", "ViewModel for Compose", DepCategory.COMPOSE),
    Dependency("androidx.compose.material3", "material3", "", "Material Design 3", DepCategory.COMPOSE,
        "implementation(\"androidx.compose.material3:material3\")"),
    Dependency("androidx.compose.animation", "animation", "", "Compose Animations", DepCategory.COMPOSE,
        "implementation(\"androidx.compose.animation:animation\")"),
    Dependency("androidx.constraintlayout", "constraintlayout-compose", "1.0.1", "ConstraintLayout for Compose", DepCategory.COMPOSE),
    Dependency("io.github.raamcosta.compose-destinations", "core", "1.10.2", "Type-safe Compose Navigation", DepCategory.COMPOSE),

    // Networking
    Dependency("com.squareup.retrofit2", "retrofit", "2.11.0", "HTTP client", DepCategory.NETWORKING),
    Dependency("com.squareup.retrofit2", "converter-gson", "2.11.0", "Gson converter for Retrofit", DepCategory.NETWORKING),
    Dependency("com.squareup.okhttp3", "okhttp", "4.12.0", "HTTP + HTTP/2 client", DepCategory.NETWORKING),
    Dependency("com.squareup.okhttp3", "logging-interceptor", "4.12.0", "OkHttp logging", DepCategory.NETWORKING),
    Dependency("io.ktor", "ktor-client-android", "2.3.12", "Kotlin HTTP client", DepCategory.NETWORKING),
    Dependency("io.ktor", "ktor-client-content-negotiation", "2.3.12", "Ktor content negotiation", DepCategory.NETWORKING),

    // Database
    Dependency("androidx.room", "room-runtime", "2.6.1", "Room Database", DepCategory.DATABASE),
    Dependency("androidx.room", "room-ktx", "2.6.1", "Room Kotlin extensions", DepCategory.DATABASE),
    Dependency("io.realm.kotlin", "library-base", "1.16.0", "Realm Database", DepCategory.DATABASE),
    Dependency("app.cash.sqldelight", "android-driver", "2.0.2", "SQLDelight", DepCategory.DATABASE),

    // DI
    Dependency("com.google.dagger", "hilt-android", "2.51.1", "Hilt Dependency Injection", DepCategory.DI),
    Dependency("io.insert-koin", "koin-android", "3.5.6", "Koin DI", DepCategory.DI),
    Dependency("io.insert-koin", "koin-androidx-compose", "3.5.6", "Koin for Compose", DepCategory.DI),

    // Testing
    Dependency("junit", "junit", "4.13.2", "JUnit 4 testing", DepCategory.TESTING,
        "testImplementation(\"junit:junit:4.13.2\")"),
    Dependency("androidx.test.ext", "junit", "1.2.1", "Android JUnit runner", DepCategory.TESTING,
        "androidTestImplementation(\"androidx.test.ext:junit:1.2.1\")"),
    Dependency("io.mockk", "mockk", "1.13.11", "Mocking library for Kotlin", DepCategory.TESTING,
        "testImplementation(\"io.mockk:mockk:1.13.11\")"),
    Dependency("app.cash.turbine", "turbine", "1.1.0", "Flow testing library", DepCategory.TESTING,
        "testImplementation(\"app.cash.turbine:turbine:1.1.0\")"),

    // Image loading
    Dependency("io.coil-kt", "coil-compose", "2.6.0", "Image loading for Compose", DepCategory.IMAGE),
    Dependency("com.github.bumptech.glide", "glide", "4.16.0", "Image loading & caching", DepCategory.IMAGE),

    // Serialization
    Dependency("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.7.1", "Kotlin serialization", DepCategory.SERIALIZATION),
    Dependency("com.google.code.gson", "gson", "2.11.0", "Google JSON library", DepCategory.SERIALIZATION),

    // Utils
    Dependency("org.jetbrains.kotlinx", "kotlinx-coroutines-android", "1.8.1", "Kotlin Coroutines", DepCategory.UTILS),
    Dependency("androidx.datastore", "datastore-preferences", "1.1.1", "DataStore Preferences", DepCategory.UTILS),
    Dependency("androidx.work", "work-runtime-ktx", "2.9.0", "WorkManager", DepCategory.UTILS),
    Dependency("com.jakewharton.timber", "timber", "5.0.1", "Logging utility", DepCategory.UTILS),
    Dependency("org.jetbrains.kotlinx", "kotlinx-datetime", "0.6.0", "Kotlin DateTime", DepCategory.UTILS),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependencyScreen(vm: IDEViewModel) {
    val project by vm.currentProject.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<DepCategory?>(null) }
    var addedDeps by remember { mutableStateOf<Set<String>>(emptySet()) }
    val clipboard = LocalClipboardManager.current
    val snackbarHost = remember { SnackbarHostState() }

    val filtered = remember(query, selectedCat) {
        POPULAR_DEPS.filter { dep ->
            (selectedCat == null || dep.category == selectedCat) &&
            (query.isEmpty() ||
             dep.artifact.contains(query, ignoreCase = true) ||
             dep.group.contains(query, ignoreCase = true) ||
             dep.description.contains(query, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dependencies", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.EDITOR) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search dependencies…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                    focusedContainerColor = IDESurface, unfocusedContainerColor = IDESurface,
                    cursorColor = IDEPrimary
                )
            )

            // Category chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = selectedCat == null, onClick = { selectedCat = null },
                    label = { Text("All", fontSize = 12.sp) })
                DepCategory.values().forEach { cat ->
                    FilterChip(selected = selectedCat == cat, onClick = { selectedCat = if (selectedCat == cat) null else cat },
                        label = { Text(cat.label, fontSize = 12.sp) })
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = IDEOutline)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { dep ->
                    DependencyCard(
                        dep = dep,
                        isAdded = dep.gradleNotation in addedDeps,
                        onAdd = {
                            project?.let { proj ->
                                val added = addDependencyToGradle(proj.path, dep)
                                if (added) {
                                    addedDeps = addedDeps + dep.gradleNotation
                                    vm.showSnackbar("Added: ${dep.artifact}")
                                } else {
                                    vm.showSnackbar("Already present or build.gradle not found")
                                }
                            } ?: vm.showSnackbar("No project open")
                        },
                        onCopy = {
                            clipboard.setText(AnnotatedString(dep.gradleNotation))
                            vm.showSnackbar("Copied to clipboard")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DependencyCard(
    dep: Dependency,
    isAdded: Boolean,
    onAdd: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdded) IDESecondary.copy(alpha = 0.07f) else IDESurface
        ),
        border = BorderStroke(1.dp, if (isAdded) IDESecondary.copy(alpha = 0.3f) else IDEOutline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dep.artifact, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        if (dep.latestVersion.isNotEmpty()) {
                            Surface(shape = RoundedCornerShape(4.dp),
                                color = IDEPrimary.copy(alpha = 0.15f)) {
                                Text(dep.latestVersion, fontSize = 10.sp, color = IDEPrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(dep.group, fontSize = 11.sp, color = IDEOnSurface, fontFamily = FontFamily.Monospace)
                    Text(dep.description, fontSize = 12.sp, color = IDEOnBackground.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp))
                }

                // Category badge
                Surface(shape = RoundedCornerShape(6.dp),
                    color = IDESurfaceVariant, border = BorderStroke(1.dp, IDEOutline)) {
                    Text(dep.category.label, fontSize = 9.sp, color = IDEOnSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Gradle notation
            Surface(shape = RoundedCornerShape(6.dp), color = IDEBackground) {
                Text(
                    dep.gradleNotation,
                    modifier = Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SyntaxString
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy", fontSize = 12.sp)
                }
                Button(
                    onClick = onAdd,
                    enabled = !isAdded,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdded) IDESecondary else IDEPrimary
                    )
                ) {
                    Icon(if (isAdded) Icons.Default.Check else Icons.Default.Add, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isAdded) "Added" else "Add to project", fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Write to build.gradle.kts ──────────────────────────────────────────────────
private fun addDependencyToGradle(projectPath: String, dep: Dependency): Boolean {
    val gradleFile = File(projectPath, "app/build.gradle.kts")
    if (!gradleFile.exists()) return false
    val content = gradleFile.readText()
    if (content.contains(dep.gradleNotation)) return false  // already present

    // Insert before closing brace of dependencies block
    val depsBlock = Regex("""(dependencies\s*\{)""")
    return if (depsBlock.containsMatchIn(content)) {
        val updated = content.replace(depsBlock) {
            "${it.value}\n    ${dep.gradleNotation}"
        }
        gradleFile.writeText(updated)
        true
    } else {
        // Append dependencies block
        gradleFile.appendText("\ndependencies {\n    ${dep.gradleNotation}\n}\n")
        true
    }
}
