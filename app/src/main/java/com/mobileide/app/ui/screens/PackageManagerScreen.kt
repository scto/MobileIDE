package com.mobileide.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

data class TermuxPkg(
    val name: String,
    val description: String,
    val category: PkgCategory,
    val essential: Boolean = false
)

enum class PkgCategory(val label: String) {
    BUILD("Build Tools"), LANG("Languages"), VCS("Version Control"),
    UTIL("Utilities"), EDITOR("Editors"), NET("Network"), DB("Database")
}

val KNOWN_PACKAGES = listOf(
    TermuxPkg("openjdk-17",     "OpenJDK 17 – Java Development Kit",     PkgCategory.LANG,  essential = true),
    TermuxPkg("gradle",         "Gradle build system",                   PkgCategory.BUILD, essential = true),
    TermuxPkg("git",            "Distributed version control system",     PkgCategory.VCS,   essential = true),
    TermuxPkg("android-tools",  "ADB, fastboot and other Android tools", PkgCategory.BUILD, essential = true),
    TermuxPkg("python",         "Python 3 interpreter",                  PkgCategory.LANG),
    TermuxPkg("nodejs",         "Node.js JavaScript runtime",            PkgCategory.LANG),
    TermuxPkg("ruby",           "Ruby programming language",             PkgCategory.LANG),
    TermuxPkg("clang",          "C/C++ compiler (LLVM)",                 PkgCategory.LANG),
    TermuxPkg("rust",           "Rust programming language",             PkgCategory.LANG),
    TermuxPkg("golang",         "Go programming language",               PkgCategory.LANG),
    TermuxPkg("wget",           "File download utility",                 PkgCategory.UTIL),
    TermuxPkg("curl",           "URL data transfer tool",                PkgCategory.UTIL),
    TermuxPkg("zip",            "Zip compression utility",               PkgCategory.UTIL),
    TermuxPkg("unzip",          "Zip extraction utility",                PkgCategory.UTIL),
    TermuxPkg("tar",            "Archive utility",                       PkgCategory.UTIL),
    TermuxPkg("htop",           "Interactive process viewer",            PkgCategory.UTIL),
    TermuxPkg("neovim",         "Vim-based text editor",                 PkgCategory.EDITOR),
    TermuxPkg("nano",           "Simple command-line text editor",       PkgCategory.EDITOR),
    TermuxPkg("vim",            "Vi Improved text editor",               PkgCategory.EDITOR),
    TermuxPkg("openssh",        "SSH client and server",                 PkgCategory.NET),
    TermuxPkg("nmap",           "Network scanner",                       PkgCategory.NET),
    TermuxPkg("sqlite",         "SQLite database engine",                PkgCategory.DB),
    TermuxPkg("postgresql",     "PostgreSQL database",                   PkgCategory.DB),
    TermuxPkg("maven",          "Maven build tool",                      PkgCategory.BUILD),
    TermuxPkg("cmake",          "Cross-platform build system",           PkgCategory.BUILD),
    TermuxPkg("gh",             "GitHub CLI tool",                       PkgCategory.VCS),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageManagerScreen(vm: IDEViewModel) {
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<PkgCategory?>(null) }
    var installedPkgs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filtered = remember(query, selectedCat) {
        KNOWN_PACKAGES.filter { pkg ->
            (selectedCat == null || pkg.category == selectedCat) &&
            (query.isEmpty() || pkg.name.contains(query, true) || pkg.description.contains(query, true))
        }.sortedWith(compareBy({ !it.essential }, { it.name }))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Package Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { vm.navigate(Screen.SETTINGS) }) {
                    Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = {
                        vm.navigate(Screen.TERMINAL)
                        vm.runCommand("pkg update -y && pkg upgrade -y 2>&1")
                    }) { Icon(Icons.Default.SystemUpdate, null, tint = IDEPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Search
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search packages…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().padding(12.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                    focusedContainerColor = IDESurface, unfocusedContainerColor = IDESurface,
                    cursorColor = IDEPrimary)
            )

            // Category chips
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = selectedCat == null, onClick = { selectedCat = null },
                    label = { Text("All", fontSize = 11.sp) })
                PkgCategory.values().forEach { cat ->
                    FilterChip(selected = selectedCat == cat, onClick = { selectedCat = if (selectedCat == cat) null else cat },
                        label = { Text(cat.label, fontSize = 11.sp) })
                }
            }

            // Essential section hint
            if (selectedCat == null && query.isEmpty()) {
                Surface(color = IDEPrimary.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = IDEPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Starred packages are essential for building Android apps",
                            fontSize = 11.sp, color = IDEPrimary)
                    }
                }
            }

            HorizontalDivider(color = IDEOutline)

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { pkg ->
                    PackageCard(
                        pkg = pkg,
                        isInstalled = pkg.name in installedPkgs,
                        onInstall = {
                            installedPkgs = installedPkgs + pkg.name
                            vm.navigate(Screen.TERMINAL)
                            vm.runCommand(vm.termux.let { "pkg install -y ${pkg.name} 2>&1" })
                        },
                        onRemove = {
                            installedPkgs = installedPkgs - pkg.name
                            vm.navigate(Screen.TERMINAL)
                            vm.runCommand("pkg remove -y ${pkg.name} 2>&1")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageCard(pkg: TermuxPkg, isInstalled: Boolean, onInstall: () -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isInstalled) IDESecondary.copy(alpha = 0.06f) else IDESurface),
        border = BorderStroke(1.dp, if (isInstalled) IDESecondary.copy(alpha = 0.3f) else IDEOutline)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pkg.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace, color = IDEOnBackground)
                    if (pkg.essential) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = IDEPrimary)
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp),
                        color = IDESurfaceVariant, border = BorderStroke(1.dp, IDEOutline)) {
                        Text(pkg.category.label, fontSize = 9.sp, color = IDEOnSurface,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
                Text(pkg.description, fontSize = 12.sp, color = IDEOnSurface,
                    modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(8.dp))
            if (isInstalled) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = IDETertiary)
                }
            } else {
                IconButton(onClick = onInstall) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp), tint = IDESecondary)
                }
            }
        }
    }
}
