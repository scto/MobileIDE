package com.mobileide.data

import com.mobileide.common.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Das Repository ist die einzige Quelle der Wahrheit für alle Dateioperationen.
 * Es abstrahiert den Zugriff auf das Dateisystem.
 */
interface FileRepository {
    /** Listet Dateien und Ordner in einem Verzeichnis auf. (Wird vom Explorer verwendet) */
    suspend fun getFiles(path: String): Result<List<FileItem>>

    /** Liest den Textinhalt einer Datei. (Wird vom Editor verwendet) */
    suspend fun readFile(path: String): Result<String>

    /** Speichert Textinhalt in einer Datei. (Wird vom Editor verwendet) */
    suspend fun saveFile(path: String, content: String): Result<Unit>
}

class FileRepositoryImpl @Inject constructor() : FileRepository {
    override suspend fun getFiles(path: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)
            if (!directory.exists() || !directory.isDirectory) {
                return@withContext Result.failure(IllegalArgumentException("Path is not a valid directory."))
            }

            val files = directory.listFiles()?.map { file ->
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    lastModified = file.lastModified()
                )
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) // Ordner zuerst, dann alphabetisch

            Result.success(files ?: emptyList())
        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied to access $path. Please check storage permissions.", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readFile(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found."))
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveFile(path: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            File(path).writeText(content)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

