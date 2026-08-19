package com.scto.mobile.ide.commands.editor





import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.commands.EditorFileActionContext
import com.scto.mobile.ide.commands.EditorFileCommand
import com.scto.mobile.ide.file.FileWrapper
import com.scto.mobile.ide.icons.Icon
import com.scto.mobile.ide.utils.toast
import kotlinx.coroutines.launch











class ShareCommand : EditorFileCommand() {
    override val id: String = "editor.share"

    override fun getLabel(): String = com.scto.mobile.ide.core.main.R.string.share.getString()

    override fun action(context: EditorFileActionContext) {
        val activity = context.currentActivity
        val file = context.file

        DefaultScope.launch {
            if (file.getAbsolutePath().contains(activity.filesDir.parentFile!!.absolutePath)) {
                toast(com.scto.mobile.ide.core.main.R.string.permission_denied)
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

    override fun getIcon(): Icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.send)
}
