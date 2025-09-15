package com.mobileide.languages.servers

import android.content.Context
import com.mobileide.languages.LanguageServerDefinition
import javax.inject.Inject

class JsonLanguageServer @Inject constructor() : LanguageServerDefinition {
    override val languageId: String = "json"
    override val fileExtensions: List<String> = listOf("json")

    override fun createStartCommand(context: Context): List<String> {
        // Placeholder
        return emptyList()
    }
}
