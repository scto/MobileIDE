package com.scto.mobile.ide.components





import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.scto.mobile.ide.activities.main.MainActivity
import com.scto.mobile.ide.activities.main.MainViewModel
import com.scto.mobile.ide.activities.main.ui.drawerStateRef
import com.scto.mobile.ide.activities.main.ui.fileTreeViewModel
import com.scto.mobile.ide.activities.main.ui.searchViewModel
import com.scto.mobile.ide.commands.ActionContext
import com.scto.mobile.ide.commands.ToolbarConfiguration
import com.scto.mobile.ide.drawer.DrawerViewModel
import com.scto.mobile.ide.file.toFileObject
import com.scto.mobile.ide.filetree.FileTreeTab
import com.scto.mobile.ide.icons.CreateNewFile
import com.scto.mobile.ide.icons.XedIcon
import com.scto.mobile.ide.icons.XedIcons
import com.scto.mobile.ide.search.CodeSearchDialog
import com.scto.mobile.ide.search.FileSearchDialog
import com.scto.mobile.ide.utils.application
import com.scto.mobile.ide.utils.errorDialog
import kotlinx.coroutines.launch











var addDialog by mutableStateOf(false)
var fileSearchDialog by mutableStateOf(false)
var codeSearchDialog by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalToolbarActions(viewModel: MainViewModel, drawerViewModel: DrawerViewModel) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    val commands by remember { derivedStateOf { ToolbarConfiguration.globalCommands } }

    if (viewModel.tabs.isEmpty() || viewModel.currentTab?.showGlobalActions == true) {
        for (command in commands) {
            if (command.isSupported()) {
                IconButton(
                    enabled = command.isEnabled(),
                    onClick = {
                        activity?.let {
                            command.performCommand(ActionContext(it))
                        }
                    },
                ) {
                    XedIcon(command.getIcon())
                }
            }
        }
    }

    if (fileSearchDialog && drawerViewModel.currentDrawerTab is FileTreeTab) {
        FileSearchDialog(
            mainViewModel = viewModel,
            searchViewModel = searchViewModel.get()!!,
            projectFile = (drawerViewModel.currentDrawerTab as FileTreeTab).root,
            onFinish = { fileSearchDialog = false },
            onSelect = { projectFile, fileObject ->
                scope.launch {
                    if (fileObject.isFile()) {
                        viewModel.editorManager.openFile(
                            fileObject = fileObject,
                            projectRoot = projectFile,
                            checkDuplicate = true,
                            switchToTab = true,
                        )
                        drawerStateRef.get()?.close()
                    } else {
                        fileTreeViewModel.get()?.goToFolder(projectFile, fileObject)
                        drawerStateRef.get()!!.open()
                    }
                }
            },
        )
    }

    if (codeSearchDialog && drawerViewModel.currentDrawerTab is FileTreeTab) {
        CodeSearchDialog(
            mainViewModel = viewModel,
            searchViewModel = searchViewModel.get()!!,
            projectFile = (drawerViewModel.currentDrawerTab as FileTreeTab).root,
            onFinish = { codeSearchDialog = false },
        )
    }

    if (addDialog) {
        ModalBottomSheet(onDismissRequest = { addDialog = false }) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp)) {
                AddDialogItem(resId = com.scto.mobile.ide.core.main.R.drawable.file, title = stringResource(com.scto.mobile.ide.core.main.R.string.temp_file)) {
                    addDialog = false

                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "application/octet-stream"
                    intent.putExtra(Intent.EXTRA_TITLE, "newfile.txt")

                    val activities =
                        application!!.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

                    if (activities.isEmpty()) {
                        errorDialog(com.scto.mobile.ide.core.main.R.string.unsupported_feature)
                        return@AddDialogItem
                    }

                    val title = viewModel.getNextUntitledTitle()
                    viewModel.editorManager.addEditorTab(file = null, customTitle = title)
                }

                AddDialogItem(icon = XedIcons.CreateNewFile, title = stringResource(com.scto.mobile.ide.core.main.R.string.new_file)) {
                    addDialog = false
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "application/octet-stream"
                    intent.putExtra(Intent.EXTRA_TITLE, "newfile.txt")

                    val activities =
                        application!!.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                    if (activities.isEmpty()) {
                        errorDialog(com.scto.mobile.ide.core.main.R.string.unsupported_feature)
                    } else {
                        XedHost?.apply {
                            fileManager.createNewFile(mimeType = "*/*", title = "newfile.txt") {
                                if (it != null) {
                                    lifecycleScope.launch {
                                        viewModel.editorManager.openFile(
                                            it,
                                            projectRoot = null,
                                            checkDuplicate = true,
                                            switchToTab = true,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AddDialogItem(resId = com.scto.mobile.ide.core.main.R.drawable.file_symlink, title = stringResource(com.scto.mobile.ide.core.main.R.string.open_file)) {
                    addDialog = false
                    XedHost?.apply {
                        fileManager.requestOpenFile(mimeType = "*/*") {
                            if (it != null) {
                                lifecycleScope.launch {
                                    viewModel.editorManager.openFile(
                                        it.toFileObject(expectedIsFile = true),
                                        checkDuplicate = true,
                                        projectRoot = null,
                                        switchToTab = true,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
