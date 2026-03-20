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
import com.mobileide.app.data.LineType
import com.mobileide.app.data.TerminalLine
import com.mobileide.app.ui.theme.*
import com.mobileide.app.viewmodel.IDEViewModel
import com.mobileide.app.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitScreen(vm: IDEViewModel) {
    val project by vm.currentProject.collectAsState()
    var commitMsg by remember { mutableStateOf("") }
    var remoteUrl by remember { mutableStateOf("") }
    var showRemoteDialog by remember { mutableStateOf(false) }
    var showBranchDialog by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    val projectPath = project?.path ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(Screen.EDITOR) }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(IDEBackground)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = IDESurface,
                contentColor     = IDEPrimary
            ) {
                listOf("Commit", "History", "Branches", "Remote").forEachIndexed { i, label ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                        Text(label, modifier = Modifier.padding(vertical = 12.dp), fontSize = 13.sp)
                    }
                }
            }

            when (selectedTab) {
                0 -> CommitTab(vm, projectPath, commitMsg, onMsgChange = { commitMsg = it })
                1 -> HistoryTab(vm, projectPath)
                2 -> BranchTab(vm, projectPath, showBranchDialog, newBranchName,
                    onShowDialog = { showBranchDialog = it },
                    onBranchNameChange = { newBranchName = it }
                )
                3 -> RemoteTab(vm, projectPath, remoteUrl,
                    onUrlChange = { remoteUrl = it },
                    showDialog = showRemoteDialog,
                    onShowDialog = { showRemoteDialog = it }
                )
            }
        }
    }
}

// ── Commit Tab ────────────────────────────────────────────────────────────────
@Composable
private fun CommitTab(
    vm: IDEViewModel,
    path: String,
    commitMsg: String,
    onMsgChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick actions
        Text("QUICK ACTIONS", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GitActionChip("Init", Icons.Default.FolderOpen) {
                vm.runGitCommand(path) { git -> git.init(path) }
            }
            GitActionChip("Status", Icons.Default.Info) {
                vm.runGitCommand(path) { git -> git.status(path) }
            }
            GitActionChip("Diff", Icons.Default.Compare) {
                vm.runGitCommand(path) { git -> git.diff(path) }
            }
        }

        HorizontalDivider(color = IDEOutline)

        // Stage + Commit
        Text("COMMIT", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)
        OutlinedTextField(
            value = commitMsg,
            onValueChange = onMsgChange,
            label = { Text("Commit message") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = IDEPrimary,
                unfocusedBorderColor  = IDEOutline,
                focusedContainerColor = IDESurface,
                unfocusedContainerColor = IDESurface,
                cursorColor           = IDEPrimary
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.runGitCommand(path) { git -> git.addAll(path) } },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Stage All")
            }
            Button(
                onClick = {
                    if (commitMsg.isNotBlank())
                        vm.runGitCommand(path) { git -> git.commit(path, commitMsg) }
                },
                enabled = commitMsg.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Commit")
            }
        }

        HorizontalDivider(color = IDEOutline)

        // Stash
        Text("STASH", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.runGitCommand(path) { git -> git.stash(path) } },
                modifier = Modifier.weight(1f)
            ) { Text("Stash Changes") }
            OutlinedButton(
                onClick = { vm.runGitCommand(path) { git -> git.stashPop(path) } },
                modifier = Modifier.weight(1f)
            ) { Text("Pop Stash") }
        }
    }
}

// ── History Tab ───────────────────────────────────────────────────────────────
@Composable
private fun HistoryTab(vm: IDEViewModel, path: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { vm.runGitCommand(path) { git -> git.log(path) }; vm.navigate(Screen.TERMINAL) },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(Icons.Default.History, null)
            Spacer(Modifier.width(8.dp))
            Text("Show Git Log in Terminal")
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, null, Modifier.size(48.dp), tint = IDEOutline)
                Spacer(Modifier.height(8.dp))
                Text("Open Terminal to view full log", color = IDEOnSurface)
            }
        }
    }
}

// ── Branch Tab ────────────────────────────────────────────────────────────────
@Composable
private fun BranchTab(
    vm: IDEViewModel,
    path: String,
    showDialog: Boolean,
    branchName: String,
    onShowDialog: (Boolean) -> Unit,
    onBranchNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.runGitCommand(path) { git -> git.branch(path) }; vm.navigate(Screen.TERMINAL) },
                modifier = Modifier.weight(1f)
            ) { Text("List Branches") }
            Button(
                onClick = { onShowDialog(true) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AccountTree, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Branch")
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { onShowDialog(false) },
                title = { Text("Create Branch") },
                text = {
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = onBranchNameChange,
                        label = { Text("Branch name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (branchName.isNotBlank()) {
                            vm.runGitCommand(path) { git -> git.createBranch(path, branchName) }
                            onShowDialog(false)
                        }
                    }) { Text("Create") }
                },
                dismissButton = { TextButton(onClick = { onShowDialog(false) }) { Text("Cancel") } }
            )
        }
    }
}

// ── Remote Tab ────────────────────────────────────────────────────────────────
@Composable
private fun RemoteTab(
    vm: IDEViewModel,
    path: String,
    remoteUrl: String,
    onUrlChange: (String) -> Unit,
    showDialog: Boolean,
    onShowDialog: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("REMOTE", style = MaterialTheme.typography.labelSmall, color = IDEPrimary)

        OutlinedTextField(
            value = remoteUrl,
            onValueChange = onUrlChange,
            label = { Text("Remote URL (git@github.com:user/repo.git)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IDEPrimary, unfocusedBorderColor = IDEOutline,
                focusedContainerColor = IDESurface, unfocusedContainerColor = IDESurface
            )
        )
        Button(
            onClick = {
                if (remoteUrl.isNotBlank())
                    vm.runGitCommand(path) { git -> git.addRemote(path, remoteUrl) }
            },
            enabled = remoteUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Set Remote Origin") }

        HorizontalDivider(color = IDEOutline)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { vm.runGitCommand(path) { git -> git.pull(path) }; vm.navigate(Screen.TERMINAL) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowDownward, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Pull")
            }
            Button(
                onClick = { vm.runGitCommand(path) { git -> git.push(path) }; vm.navigate(Screen.TERMINAL) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowUpward, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Push")
            }
        }
    }
}

@Composable
private fun GitActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = IDESurfaceVariant,
        border = BorderStroke(1.dp, IDEOutline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(14.dp), tint = IDEPrimary)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, color = IDEOnBackground)
        }
    }
}
