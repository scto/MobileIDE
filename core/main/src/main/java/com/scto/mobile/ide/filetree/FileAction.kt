package com.scto.mobile.ide.filetree





import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.activities.main.MainActivity
import com.scto.mobile.ide.drawer.DrawerViewModel
import com.scto.mobile.ide.extension.api.IntentHandleRegistry
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.file.FileOperations
import com.scto.mobile.ide.file.unzipTo
import com.scto.mobile.ide.icons.CreateNewFile
import com.scto.mobile.ide.icons.CreateNewFolder
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.icons.XedIcons
import com.scto.mobile.ide.tabs.editor.EditorTab
import com.scto.mobile.ide.utils.logError
import com.scto.mobile.ide.utils.toast
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch











data class FileActionContext(
    val file: FileObject,
    val root: FileObject?,
    val viewModel: FileTreeViewModel,
    val drawerViewModel: DrawerViewModel,
    val context: Context,
)

data class MultiFileActionContext(
    val files: List<FileObject>,
    val root: FileObject?,
    val viewModel: FileTreeViewModel,
    val drawerViewModel: DrawerViewModel,
    val context: Context,
)

data class FileActionType(val file: Boolean, val folder: Boolean, val rootFolder: Boolean) {
    companion object {
        val All = FileActionType(file = true, folder = true, rootFolder = true)
    }
}

interface BaseFileAction {
    val icon: Icon
    val title: String
    val type: FileActionType
    val importance: Int
}

abstract class FileAction : BaseFileAction {
    abstract override val icon: Icon
    abstract override val title: String

    abstract fun action(context: FileActionContext)

    open fun isSupported(file: FileObject): Boolean = true

    open fun isEnabled(file: FileObject): Boolean = true

    abstract override val type: FileActionType
    override val importance = 0
}

abstract class MultiFileAction : BaseFileAction {
    abstract override val icon: Icon
    abstract override val title: String

    abstract fun action(context: MultiFileActionContext)

    open fun isSupported(files: List<FileObject>): Boolean = true

    open fun isEnabled(files: List<FileObject>): Boolean = true

    abstract override val type: FileActionType
    override val importance = 0
}

object CloseAction : FileAction() {
    override val icon = Icon.VectorIcon(Icons.Outlined.Close)
    override val title = com.scto.mobile.ide.core.main.R.string.close.getString()

    override fun action(context: FileActionContext) = context.viewModel.showCloseProjectConfirmation(context.file)

    override val type = FileActionType(file = false, folder = false, rootFolder = true)
}

object RefreshAction : MultiFileAction() {
    override val icon = Icon.VectorIcon(Icons.Outlined.Refresh)
    override val title = com.scto.mobile.ide.core.main.R.string.refresh.getString()

    override fun action(context: MultiFileActionContext) {
        context.files.forEach { context.viewModel.updateCache(it) }
    }

    override val type = FileActionType(file = false, folder = true, rootFolder = true)
}

object CreateNewFileAction : FileAction() {
    override val icon = Icon.VectorIcon(XedIcons.CreateNewFile)
    override val title = com.scto.mobile.ide.core.main.R.string.new_file.getString()

    override fun action(context: FileActionContext) {
        context.viewModel.showCreateDialog(true, context.file, context.root)
    }

    override val type = FileActionType(file = false, folder = true, rootFolder = true)
}

object CreateNewFolderAction : FileAction() {
    override val icon = Icon.VectorIcon(XedIcons.CreateNewFolder)
    override val title = com.scto.mobile.ide.core.main.R.string.new_folder.getString()

    override fun action(context: FileActionContext) {
        context.viewModel.showCreateDialog(false, context.file, context.root)
    }

    override val type = FileActionType(file = false, folder = true, rootFolder = true)
}

object RenameAction : FileAction() {
    override val icon = Icon.VectorIcon(Icons.Outlined.Edit)
    override val title = com.scto.mobile.ide.core.main.R.string.rename.getString()

    override fun action(context: FileActionContext) {
        context.viewModel.showRenameDialog(context.file)
    }

    override val type = FileActionType.All
}

object DeleteAction : MultiFileAction() {
    override val icon = Icon.VectorIcon(Icons.Outlined.Delete)
    override val title = com.scto.mobile.ide.core.main.R.string.delete.getString()

    override fun action(context: MultiFileActionContext) {
        context.viewModel.showDeleteConfirmation(context.files, context.root)
    }

    override val type = FileActionType.All
    override val importance = 3
}

object CopyAction : MultiFileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.copy)
    override val title = com.scto.mobile.ide.core.main.R.string.copy.getString()

    override fun action(context: MultiFileActionContext) {
        FileOperations.copyToClipboard(context.files)
        toast(context.context.getString(com.scto.mobile.ide.core.main.R.string.copied))
    }

    override val type = FileActionType.All
    override val importance = 1
}

object CutAction : MultiFileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.cut)
    override val title = com.scto.mobile.ide.core.main.R.string.cut.getString()

    override fun action(context: MultiFileActionContext) {
        FileOperations.copyToClipboard(context.files, isCut = true)
        context.files.forEach { context.viewModel.markNodeAsCut(it) }
    }

    override val type = FileActionType.All
}

object PasteAction : FileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.paste)
    override val title = com.scto.mobile.ide.core.main.R.string.paste.getString()

    override fun action(context: FileActionContext) {
        context.viewModel.viewModelScope.launch {
            val isCut = FileOperations.isCut
            val clipboardFiles = FileOperations.clipboard

            context.viewModel.withFileOperation {
                for (clipboardFile in clipboardFiles) {
                    FileOperations.pasteFile(
                            context = context.context,
                            sourceFile = clipboardFile,
                            destinationFolder = context.file,
                            isCut = isCut,
                        )
                        .onFailure { toast(it.message ?: com.scto.mobile.ide.core.main.R.string.paste_failed.getString()) }
                        .onSuccess {
                            if (isCut) {
                                XedHost?.apply {
                                    val targetTab =
                                        viewModel.tabs.find { it is EditorTab && it.file == clipboardFile }
                                            as? EditorTab
                                    targetTab?.file = context.file.getChild(clipboardFile.getName())
                                }
                            }
                            clipboardFile.getParentFile()?.let { context.viewModel.updateCache(it) }
                            context.viewModel.updateCache(context.file)
                            context.viewModel.unmarkNodeAsCut(clipboardFile)
                        }
                }
            }
        }
    }

    override fun isEnabled(file: FileObject): Boolean {
        return FileOperations.clipboard.isNotEmpty()
    }

    override val type = FileActionType(file = false, folder = true, rootFolder = true)
    override val importance: Int
        get() = if (FileOperations.clipboard.isEmpty()) super.importance else 2
}

object OpenWithAction : FileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.open_in_new)
    override val title = com.scto.mobile.ide.core.main.R.string.open_with.getString()

    override fun action(context: FileActionContext) {
        context.viewModel.viewModelScope.launch { FileOperations.openWithExternalApp(context.context, context.file) }
    }

    override val type = FileActionType.All
}

object SaveAsAction : FileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.file_symlink)
    override val title = com.scto.mobile.ide.core.main.R.string.save_as.getString()

    override fun action(context: FileActionContext) {
        FileOperations.saveAs(context.file)
    }

    override val type = FileActionType.All
}

object AddFileAction : FileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.arrow_downward)
    override val title = com.scto.mobile.ide.core.main.R.string.add_file.getString()

    override fun action(context: FileActionContext) {
        FileOperations.addFile(context.file)
    }

    override val type = FileActionType(file = false, folder = true, rootFolder = true)
}

object OpenAsProjectAction : FileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.folder_code)
    override val title = com.scto.mobile.ide.core.main.R.string.open_as_project.getString()

    override fun action(context: FileActionContext) {
        context.drawerViewModel.addFileTreeTab(context.file, true)
    }

    override fun isEnabled(file: FileObject): Boolean {
        val drawerViewModel = XedHost?.drawerViewModel ?: return false
        return drawerViewModel.drawerTabs.none { it is FileTreeTab && it.root == file }
    }

    override val type = FileActionType(file = false, folder = true, rootFolder = true)
}

object PropertiesAction : FileAction() {
    override val icon = Icon.VectorIcon(Icons.Outlined.Info)
    override val title = com.scto.mobile.ide.core.main.R.string.properties.getString()

    override fun action(context: FileActionContext) {
        context.viewModel.showPropertiesDialog(context.file)
    }

    override val type = FileActionType.All
}

object UnzipAction : FileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.archive)
    override val title = com.scto.mobile.ide.core.main.R.string.unzip.getString()

    override fun action(context: FileActionContext) {
        context.viewModel.viewModelScope.launch(Dispatchers.IO) {
            val zipFile = File(context.file.getAbsolutePath())
            val targetDir = File(zipFile.parentFile, zipFile.nameWithoutExtension).apply { mkdirs() }

            runCatching {
                context.viewModel.withFileOperation {
                    zipFile.unzipTo(targetDir)
                }
            }
                .onSuccess {
                    val parent = context.file.getParentFile()
                    parent?.let { context.viewModel.updateCache(it) }
                    toast(com.scto.mobile.ide.core.main.R.string.unzip_success.getString())
                }
                .onFailure { e ->
                    logError(e)
                    toast(com.scto.mobile.ide.core.main.R.string.unzip_failed.getFilledString(e.message))
                }
        }
    }

    override fun isSupported(file: FileObject) = file.isZip() || file.isXedPackage()

    override val type = FileActionType(file = true, folder = false, rootFolder = false)
}

object InstallPackageAction : FileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.download)
    override val title = com.scto.mobile.ide.core.main.R.string.install.getString()

    override fun action(context: FileActionContext) {
        context.viewModel.viewModelScope.launch { IntentHandleRegistry.handleIntent(context.file) }
    }

    override fun isSupported(file: FileObject) = file.isXedPackage()

    override val type = FileActionType(file = true, folder = false, rootFolder = false)
}
