package com.scto.mobile.ide.commands.global

import com.scto.mobile.ide.commands.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchFileFolderCommand : GlobalCommand() {
    override val id: String = "filetree.search"
    override val title: String = "search_filetree"

    override suspend fun execute(context: CommandContext) {
        val ideContext = context as? MobileIDECommandContext ?: return
        withContext(Dispatchers.Main) {
            Toast.makeText(ideContext.androidContext, "Search File/Folder not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }
}
