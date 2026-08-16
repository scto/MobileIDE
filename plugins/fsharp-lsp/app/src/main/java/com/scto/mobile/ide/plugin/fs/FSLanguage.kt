package com.scto.mobile.ide.plugin.fs

import android.content.res.Resources
import com.scto.mobile.ide.core.common.files.FileType
import com.scto.mobile.ide.core.common.icons.Icon

class FSLanguage(resources: Resources) : FileType {
    override val extensions = listOf("fs", "fsi", "fsx")
    override val textmateScope = "source.fs"
    override val name = "fsharp"
    override val title = "F#"
    override val icon = Icon.ExternalResourceIcon(R.drawable.fsharp, resources)
}