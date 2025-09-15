package com.mobileide.languages.servers

import android.content.Context

import com.mobileide.languages.LanguageServerDefinition

import javax.inject.Inject

class JavaLanguageServer @Inject constructor() : LanguageServerDefinition {
    override val languageId: String = "java"
    override val fileExtensions: List<String> = listOf("java")

    override fun createStartCommand(context: Context): List<String> {
        // Platzhalter:
        return listOf("/data/data/com.mobileide/cache/java-language-server")
    }
}
