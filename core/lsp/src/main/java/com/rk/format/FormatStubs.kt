package com.rk.format
import com.rk.file.FileObject
import com.rk.icons.Icon
interface Formatter {
    val name: String
    val icon: Icon?
    fun format(file: FileObject)
}
object FormatRegistry {
    fun register(formatter: Formatter) {}
    fun unregister(formatter: Formatter) {}
}
