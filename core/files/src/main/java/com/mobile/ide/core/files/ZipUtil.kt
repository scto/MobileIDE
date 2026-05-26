// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.files

import java.net.URI
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.util.zip.ZipException

/**
 * Hilfsprogramm für ZIP-Operationen, angepasst für MobileIDE. Ermöglicht die Arbeit mit ZIP-Dateien als virtuelle
 * Dateisysteme.
 */
fun File.zipFileSystem(create: Boolean = false): FileSystem {
    val env = mapOf("create" to create.toString())
    val uri = URI.create("jar:" + this.javaPath.toUri().toString())
    return try {
        FileSystems.newFileSystem(uri, env)
    } catch (e: FileSystemAlreadyExistsException) {
        FileSystems.getFileSystem(uri)
    }
}

fun <T> File.withZipFileSystem(create: Boolean = false, action: (FileSystem) -> T): T {
    return this.zipFileSystem(create).use(action)
}

fun <T> File.withZipFileSystem(action: (FileSystem) -> T): T = this.withZipFileSystem(false, action)

/** Entpackt das ZIP-Archiv in ein Zielverzeichnis. */
fun File.unzipTo(destination: File, resetTimeAttributes: Boolean = false) {
    this.withZipFileSystem { zipFs ->
        val sourcePath = zipFs.getPath("/")
        recursiveCopyTo(sourcePath, destination, resetTimeAttributes)
    }
}

/** Packt ein Verzeichnis rekursiv in eine neue ZIP-Datei. */
fun File.zipTo(destination: File) {
    if (destination.exists) destination.delete()
    destination.withZipFileSystem(create = true) { zipFs ->
        val destRoot = zipFs.getPath("/")
        recursiveCopyTo(this.javaPath, File(destRoot.toString()), false)
    }
}

private fun recursiveCopyTo(sourcePath: Path, destination: File, resetTimeAttributes: Boolean) {
    val destPath = destination.javaPath
    val destFs = destPath.fileSystem
    val normalizedDestPath = destPath.normalize()

    Files.walk(sourcePath).forEach { oldPath ->
        val relative = sourcePath.relativize(oldPath)
        val newPath = destFs.getPath(destPath.toString(), relative.toString())

        // ZipSlip Schutz
        if (!newPath.normalize().startsWith(normalizedDestPath)) {
            throw ZipException("$relative versucht das Zielverzeichnis zu verlassen: $destination")
        }

        if (newPath == newPath.root) return@forEach

        if (Files.isDirectory(oldPath)) {
            if (!Files.exists(newPath)) Files.createDirectories(newPath)
        } else {
            Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING)
        }

        if (resetTimeAttributes) {
            val zero = FileTime.fromMillis(0)
            Files.getFileAttributeView(newPath, BasicFileAttributeView::class.java).setTimes(zero, zero, zero)
        }
    }
}

// Erweiterungen für direkten Zugriff
fun File.isZipFile(): Boolean {
    return try {
        FileSystems.newFileSystem(URI.create("jar:" + this.javaPath.toUri().toString()), mapOf("create" to "false"))
            .use { true }
    } catch (e: Exception) {
        false
    }
}
