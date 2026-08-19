package com.scto.mobile.ide.resources





import com.blankj.utilcode.util.StringUtils





typealias strings = com.scto.mobile.ide.core.main.R.string
typealias drawables = com.scto.mobile.ide.core.main.R.drawable
typealias plurals = com.scto.mobile.ide.core.main.R.plurals
typealias xml = com.scto.mobile.ide.core.main.R.xml

fun getString(id: Int): String {
    return StringUtils.getString(id)
}

fun getString(id: Int, vararg formatArgs: Any): String {
    return StringUtils.getString(id, *formatArgs)
}
