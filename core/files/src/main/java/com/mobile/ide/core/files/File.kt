// Copyright 2025 Thomas Schmid
/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */
package com.mobile.ide.core.files

import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

data class File(internal val javaPath: Path) {
    constructor(parent: Path, child: String) : this(parent.resolve(child))

    constructor(parent: File, child: String) : this(parent.javaPath.resolve(child))

    constructor(parent: File, child: File) : this(parent.javaPath.resolve(child.javaPath))

    constructor(path: String) : this(Paths.get(path))

    constructor(parent: String, child: String) : this(Paths.get(parent, child))

    val path: String
        get() = javaPath.toString()

    val absolutePath: String
        get() = javaPath.toAbsolutePath().toString()

    val absoluteFile: File
        get() = File(absolutePath)

    val canonicalPath: String by lazy { javaPath.toFile().canonicalPath }
    val canonicalFile: File
        get() = File(canonicalPath)

    val name: String
        get() = javaPath.fileName?.toString()?.removeSuffixIfPresent(separator) ?: ""

    val extension: String
        get() = name.substringAfterLast('.', "")

    val nameSegments: List<String>
        get() = javaPath.map { it.fileName.toString() }

    val parentOrNull: String?
        get() = javaPath.parent?.toString()

    val parent: String
        get() = parentOrNull!!

    val parentFile: File
        get() = File(javaPath.parent)

    val exists: Boolean
        get() = Files.exists(javaPath)

    val isDirectory: Boolean
        get() = Files.isDirectory(javaPath)

    val isFile: Boolean
        get() = Files.isRegularFile(javaPath)

    val isAbsolute: Boolean
        get() = javaPath.isAbsolute

    val listFiles: List<File>
        get() = Files.newDirectoryStream(javaPath).use { stream -> stream.map(::File) }

    val listFilesOrEmpty: List<File>
        get() = if (exists) listFiles else emptyList()

    val size: Long
        get() = if (exists) Files.size(javaPath) else 0L

    fun child(name: String): File = File(this, name)

    fun startsWith(another: File): Boolean = javaPath.startsWith(another.javaPath)

    fun copyTo(destination: File) {
        Files.copy(javaPath, destination.javaPath, StandardCopyOption.REPLACE_EXISTING)
    }

    fun renameTo(destination: File): Boolean = javaPath.toFile().renameTo(destination.javaPath.toFile())

    fun mkdirs(): Path = Files.createDirectories(javaPath)

    fun delete(): Boolean = Files.deleteIfExists(javaPath)

    fun deleteRecursively() = postorder { Files.delete(it) }

    fun deleteOnExitRecursively() = preorder { File(it.toString()).deleteOnExit() }

    fun preorder(task: (Path) -> Unit) {
        if (!this.exists) return
        Files.walkFileTree(
            javaPath,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    task(file)
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    task(dir)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    fun postorder(task: (Path) -> Unit) {
        if (!this.exists) return
        Files.walkFileTree(
            javaPath,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    task(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    task(dir)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    fun map(
        mode: FileChannel.MapMode = FileChannel.MapMode.READ_ONLY,
        start: Long = 0,
        size: Long = -1,
    ): MappedByteBuffer {
        val file = RandomAccessFile(path, if (mode == FileChannel.MapMode.READ_ONLY) "r" else "rw")
        val fileSize = if (mode == FileChannel.MapMode.READ_ONLY) file.length() else size
        val channel = file.channel
        return channel.map(mode, start, fileSize).also { channel.close() }
    }

    fun deleteOnExit(): File {
        javaPath.toFile().deleteOnExit()
        return this
    }

    fun createNew(): Boolean = javaPath.toFile().createNewFile()

    fun readBytes(): ByteArray = Files.readAllBytes(javaPath)

    fun writeBytes(bytes: ByteArray): Path = Files.write(javaPath, bytes)

    fun appendBytes(bytes: ByteArray): Path = Files.write(javaPath, bytes, StandardOpenOption.APPEND)

    fun writeLines(lines: Iterable<String>) {
        Files.write(javaPath, lines)
    }

    fun writeText(text: String) = writeLines(listOf(text))

    fun appendLines(lines: Iterable<String>) {
        Files.write(javaPath, lines, StandardOpenOption.APPEND)
    }

    fun appendText(text: String) = appendLines(listOf(text))

    fun forEachLine(action: (String) -> Unit) {
        Files.lines(javaPath).use { it.forEach(action) }
    }

    fun readStrings(): MutableList<String> = mutableListOf<String>().also { list -> forEachLine { list.add(it) } }

    fun createAsSymlink(target: String) {
        val targetPath = Paths.get(target)
        if (!Files.isSymbolicLink(this.javaPath)) Files.createSymbolicLink(this.javaPath, targetPath)
    }

    override fun toString(): String = path

    fun bufferedReader(): BufferedReader = Files.newBufferedReader(javaPath)

    fun outputStream(): OutputStream = Files.newOutputStream(javaPath)

    fun printWriter(): PrintWriter = javaPath.toFile().printWriter()

    override fun equals(other: Any?): Boolean {
        val otherFile = other as? File ?: return false
        return otherFile.javaPath.toAbsolutePath() == javaPath.toAbsolutePath()
    }

    override fun hashCode(): Int = javaPath.toAbsolutePath().hashCode()

    companion object {
        val userDir: File
            get() = File(System.getProperty("user.dir"))

        val userHome: File
            get() = File(System.getProperty("user.home"))

        val javaHome: File
            get() = File(System.getProperty("java.home"))

        val pathSeparator: String = java.io.File.pathSeparator
        val separator: String = java.io.File.separator
        val separatorChar: Char = java.io.File.separatorChar
    }
}

fun String.File(): File = File(this)

fun Path.File(): File = File(this)

fun createTempFile(name: String, suffix: String? = null): File = Files.createTempFile(name, suffix).File()

fun createTempDir(name: String): File = Files.createTempDirectory(name).File()

inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this?.close()
        } catch (ignored: Exception) {}
        throw e
    } finally {
        if (!closed) this?.close()
    }
}
