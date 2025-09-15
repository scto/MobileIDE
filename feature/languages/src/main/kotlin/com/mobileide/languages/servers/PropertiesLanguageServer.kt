package com.mobileide.languages.servers

import android.content.Context
import com.mobileide.languages.LanguageServerDefinition
import javax.inject.Inject

class PropertiesLanguageServer @Inject constructor() : LanguageServerDefinition {
    override val languageId: String = "properties"
    override val fileExtensions: List<String> = listOf("properties")

    override fun createStartCommand(context: Context): List<String> {
        // Placeholder
        return emptyList()
    }
}
