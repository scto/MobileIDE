package com.mobileide.languages.servers

import android.content.Context
import com.mobileide.languages.LanguageServerDefinition
import javax.inject.Inject

class GradleGroovyLanguageServer @Inject constructor() : LanguageServerDefinition {
    override val languageId: String = "groovy"
    override val fileExtensions: List<String> = listOf("gradle")

    override fun createStartCommand(context: Context): List<String> {
        // Placeholder
        return emptyList()
    }
}
