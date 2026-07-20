package com.rk.lsp

import java.io.File


var localBinDirProvider: (() -> File)? = null
var sandboxDirProvider: (() -> File)? = null
var sandboxHomeDirProvider: (() -> File)? = null

fun localBinDir(): File = localBinDirProvider?.invoke() ?: File("/data/data/com.scto.mobile.ide/local/bin")
fun sandboxDir(): File = sandboxDirProvider?.invoke() ?: File("/data/data/com.scto.mobile.ide/local/sandbox")
fun sandboxHomeDir(): File = sandboxHomeDirProvider?.invoke() ?: File("/data/data/com.scto.mobile.ide/local/home")

fun File.child(name: String): File = File(this, name)
fun File.getExtension(): String = name.substringAfterLast('.', "")

fun isTerminalInstalled(): Boolean = true

object NpmUtils {
    fun install(packages: List<String>) {}
    fun isPackageInstalled(packageName: String): Boolean = true
    fun getPackageVersion(packageName: String): String = "1.0.0"
    fun hasUpdate(packageName: String): Boolean = false
}

fun exec(command: String, args: List<String>, workingDir: File? = null): String = ""
fun exec(vararg args: String): String = ""

val Any.file: File
    get() = File(this.toString())

enum class BuiltinFileType(val extensions: List<String> = emptyList(), val icon: Any? = null) {
    HTML, CSS, JAVASCRIPT, TYPESCRIPT, XML, MARKDOWN, JSON, BASH, KOTLIN, JAVA, CPP, C, PYTHON, HTMX, TSX, JSX
}

object FileTypeManager {
    fun isConfigured(fileType: BuiltinFileType): Boolean = true
    fun fromExtension(ext: Any): BuiltinFileType? = BuiltinFileType.HTML
}

interface LspConnector
