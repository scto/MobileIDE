package com.mobileide.editor.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

val kotlinKeywords = setOf("fun", "val", "var", "if", "else", "when", "return", "class", "import", "package", "private", "public", "internal", "override")

fun highlightSyntax(code: String): AnnotatedString {
    return buildAnnotatedString {
        append(code)
        
        // Keywords
        val keywordRegex = "\\b(${kotlinKeywords.joinToString("|")})\\b".toRegex()
        keywordRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFFCF86E8), fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
        
        // String Literale
        val stringRegex = "\".*?\"".toRegex()
        stringRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFF6A8759)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Kommentare
        val commentRegex = "//.*".toRegex()
        commentRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color.Gray),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
        
        // Annotationen
        val annotationRegex = "@[A-Za-z]+".toRegex()
        annotationRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFFBBB529)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }
}