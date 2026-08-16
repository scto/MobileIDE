package com.scto.mobile.ide.plugin.fs

import android.content.res.Resources
import com.rk.file.FileType
import com.rk.icons.Icon
import com.rk.file.BuiltinFileType

class FSProjLanguage(resources: Resources) : FileType {
    override val extensions = listOf("fsproj")
    override val textmateScope = "source.fsproj"
    override val name = "fsproj"
    override val title = "F# Project"
    override val icon = BuiltinFileType.PROPERTIES.icon
}