package com.mobileide.languages

import android.content.Context

interface LanguageServerDefinition {
    val languageId: String
    val fileExtensions: List<String>
    fun createStartCommand(context: Context): List<String>
}
