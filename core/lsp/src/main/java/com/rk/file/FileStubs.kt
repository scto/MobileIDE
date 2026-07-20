package com.rk.file
import java.io.File
import com.rk.icons.Icon

fun File.child(name: String): File = File(this, name)

enum class BuiltinFileType(val icon: Icon? = null) {
    JSON, KOTLIN, JAVA, CPP, C, PYTHON, HTMX, TSX, JSX, LUA, GO, RUST, ZIG, FSHARP, TYPST
}

interface FileObject {
    fun getParentFile(): FileObject?
    fun getAbsolutePath(): String
    fun getExtension(): String
}
interface FileType
object FileTypeManager {
    fun fromExtension(ext: Any): BuiltinFileType? = null
}
fun sandboxHomeDir(): File = com.rk.lsp.sandboxHomeDir()
fun localBinDir(): File = com.rk.lsp.localBinDir()
fun File.createDirIfNot() { this.mkdirs() }
object FileOperations
fun File.toFileWrapper(): FileObject = object : FileObject {
    override fun getParentFile(): FileObject? = this@toFileWrapper.parentFile?.toFileWrapper()
    override fun getAbsolutePath(): String = this@toFileWrapper.absolutePath
    override fun getExtension(): String = this@toFileWrapper.extension
}

