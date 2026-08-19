package com.scto.mobile.ide.project





import android.content.Intent
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.filetree.FileAction
import com.scto.mobile.ide.filetree.FileActionContext
import com.scto.mobile.ide.filetree.FileActionType
import com.scto.mobile.ide.icons.Icon
import kotlinx.coroutines.launch











object NewProjectAction : FileAction() {
    override val icon = Icon.ResourceIcon(com.scto.mobile.ide.core.main.R.drawable.folder_managed)
    override val title = com.scto.mobile.ide.core.main.R.string.new_project.getString()

    override fun action(context: FileActionContext) {
        DefaultScope.launch {
            val intent =
                Intent(context.context, ProjectCreatorActivity::class.java).apply {
                    putExtra("root", context.file.toUri())
                }
            context.context.startActivity(intent)
        }
    }

    override fun isSupported(file: FileObject): Boolean {
        return ProjectTemplateRegistry.categories.any { it.templates.isNotEmpty() }
    }

    override val type = FileActionType(file = false, folder = true, rootFolder = true)
}
