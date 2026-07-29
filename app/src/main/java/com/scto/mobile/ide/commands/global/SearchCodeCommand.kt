package com.scto.mobile.ide.commands.global
import com.scto.mobile.ide.commands.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchCodeCommand : GlobalCommand() {
    override val id: String = "global.searchcode"
    override val title: String = "searchcode"
    override suspend fun execute(context: CommandContext) {
        val ideContext = context as? MobileIDECommandContext ?: return
        withContext(Dispatchers.Main) {
            Toast.makeText(ideContext.androidContext, "SearchCodeCommand not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }
}
