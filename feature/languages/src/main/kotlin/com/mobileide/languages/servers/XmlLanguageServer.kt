package com.mobileide.languages.servers

import android.content.Context

import com.mobileide.languages.LanguageServerDefinition

import javax.inject.Inject

class XmlLanguageServer @Inject constructor() : LanguageServerDefinition {
    override val languageId: String = "xml"
    override val fileExtensions: List<String> = listOf("xml")

    override fun createStartCommand(context: Context): List<String> {
        // Platzhalter:
        return listOf("/data/data/com.mobileide/cache/xml-language-server")
    }
}
