package com.scto.mobile.ide.tabs.base





import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.scto.mobile.ide.activities.main.session.TabState
import com.scto.mobile.ide.file.FileObject
import java.util.UUID











abstract class Tab {
    val id = UUID.randomUUID().toString()

    var refreshKey by mutableIntStateOf(0)
    abstract val name: String
    abstract val icon: ImageVector

    /** Can be null if tab is not file-related. */
    open val file: FileObject? = null

    /** Can be null if tab should not be restored. */
    open fun getState(): TabState? = null

    abstract val title: String

    open fun onTabRemoved() {}

    open fun onTabAdded() {}

    open fun onTabSelected() {}

    open fun onTabUnselected() {}

    open fun onDuplicate(tab: Tab) {}

    @Composable abstract fun Content()

    @Composable open fun RowScope.Actions() {}

    open val showGlobalActions: Boolean = true
}
