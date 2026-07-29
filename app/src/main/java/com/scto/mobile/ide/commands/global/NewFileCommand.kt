package com.scto.mobile.ide.commands.global
import com.scto.mobile.ide.commands.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewFileCommand : GlobalCommand() {
    override val id: String = "global.newfile"
    override val title: String = "newfile"
    override suspend fun execute(context: CommandContext) {
        val ideContext = context as? MobileIDECommandContext ?: return
        withContext(Dispatchers.Main) {
            Toast.makeText(ideContext.androidContext, "NewFileCommand not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }
}
