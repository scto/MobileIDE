package io.kiquar.plugin.fs

import android.content.res.Resources
import com.rk.file.FileType
import com.rk.icons.Icon

class FSLanguage(resources: Resources) : FileType {
    override val extensions = listOf("fs", "fsi", "fsx")
    override val textmateScope = "source.fs"
    override val name = "fsharp"
    override val title = "F#"
    override val icon = Icon.ExternalResourceIcon(R.drawable.fsharp, resources)
}