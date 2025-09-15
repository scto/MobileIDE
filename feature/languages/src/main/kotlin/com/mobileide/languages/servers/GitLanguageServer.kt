package com.mobileide.languages.servers

import android.content.Context
import com.mobileide.languages.LanguageServerDefinition
import javax.inject.Inject

class GitLanguageServer @Inject constructor() : LanguageServerDefinition {
    override val languageId: String = "git"
    override val fileExtensions: List<String> = listOf("gitignore", "gitattributes", "gitmodules")

    override fun createStartCommand(context: Context): List<String> {
        // Placeholder
        return emptyList()
    }
}
