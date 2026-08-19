package com.scto.mobile.ide.tabs.base





import com.scto.mobile.ide.activities.main.MainViewModel
import com.scto.mobile.ide.extension.api.XedExtensionPoint
import com.scto.mobile.ide.file.BuiltinFileType
import com.scto.mobile.ide.file.FileObject
import com.scto.mobile.ide.file.FileTypeManager
import com.scto.mobile.ide.tabs.image.ImageTab











@XedExtensionPoint
fun interface TabFactory {
    fun createTab(file: FileObject, projectRoot: FileObject?, viewModel: MainViewModel): Tab
}

object TabRegistry {
    private val registeredTabs = mutableMapOf<String, TabFactory>()

    @XedExtensionPoint
    fun registerTab(tabFactory: TabFactory, fileExtensions: List<String>) {
        fileExtensions.forEach { registeredTabs[it] = tabFactory }
    }

    @XedExtensionPoint
    fun unregisterTab(tabFactory: TabFactory) {
        registeredTabs.values.remove(tabFactory)
    }

    fun getTab(
        file: FileObject,
        projectRoot: FileObject?,
        viewModel: MainViewModel,
        readOnly: Boolean,
        customTitle: String?,
    ): Tab {
        val ext = file.getExtension()
        val type = FileTypeManager.fromExtension(ext)

        if (registeredTabs.containsKey(ext)) {
            return registeredTabs[ext]!!.createTab(file, projectRoot, viewModel)
        }

        return when (type) {
            BuiltinFileType.IMAGE -> ImageTab(file)
            else -> viewModel.editorManager.createEditorTab(file, projectRoot, readOnly, customTitle)
        }
    }
}
