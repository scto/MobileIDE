package com.scto.mobile.ide.plugin.fs

import android.content.res.Resources
import com.scto.mobile.ide.core.common.files.FileType
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.core.common.files.BuiltinFileType

class FSProjLanguage(resources: Resources) : FileType {
    override val extensions = listOf("fsproj")
    override val textmateScope = "source.fsproj"
    override val name = "fsproj"
    override val title = "F# Project"
    override val icon = BuiltinFileType.PROPERTIES.icon
}