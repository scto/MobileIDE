package com.mobileide.languages.servers

import android.content.Context
import com.mobileide.languages.LanguageServerDefinition
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class KotlinLanguageServer @Inject constructor() : LanguageServerDefinition {
    override val languageId: String = "kotlin"
    override val fileExtensions: List<String> = listOf("kt", "kts")

    override fun createStartCommand(context: Context): List<String> {
        val serverBinary = "kotlin-language-server"
        return try {
            val destinationFile = File(context.cacheDir, serverBinary)

            // Copy the binary from assets if it doesn't exist in the cache
            if (!destinationFile.exists()) {
                Timber.d("Copying $serverBinary from assets to cache.")
                context.assets.open(serverBinary).use { inputStream ->
                    destinationFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // Make the file executable
                destinationFile.setExecutable(true, true)
                Timber.d("$serverBinary is now executable at ${destinationFile.absolutePath}")
            }
            listOf(destinationFile.absolutePath)
        } catch (e: Exception) {
            Timber.e(e, "Could not prepare Kotlin Language Server")
            // Return an empty list or handle the error appropriately
            emptyList()
        }
    }
}
