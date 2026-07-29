package com.scto.mobile.ide.commands.editor

import com.scto.mobile.ide.ui.editor.viewmodel.CodeEditorState
import io.github.rosemoe.sora.widget.CodeEditor
import com.scto.mobile.ide.commands.*

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.EditorFileCommand
import com.scto.mobile.ide.file.FileWrapper
import com.scto.mobile.ide.utils.toast
import kotlinx.coroutines.launch

class ShareCommand : EditorFileCommand() {
    override val id: String = "editor.share"

    override val title: String = "share"

    override suspend fun executeFileCommand(context: MobileIDECommandContext, tab: CodeEditorState, editor: CodeEditor, file: java.io.File) {
        val activity = (context as? MobileIDECommandContext)?.androidContext as? android.app.Activity
        val file = context.file

        DefaultScope.launch {
            if (file.getAbsolutePath().contains(activity.filesDir.parentFile!!.absolutePath)) {
                toast(strings.permission_denied)
                return@launch
            }

            val fileUri =
                if (file is FileWrapper) {
                    FileProvider.getUriForFile(activity as Context, "${activity.packageName}.fileprovider", file.file)
                } else {
                    file.toUri()
                }

            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = activity.contentResolver.getType(fileUri) ?: "*/*"
                    setDataAndType(fileUri, activity.contentResolver.getType(fileUri) ?: "*/*")
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

            activity.startActivity(Intent.createChooser(intent, "Share file"))
        }
    }

    override val icon: Any? = null // Icon.ResourceIcon(drawables.send)
}
