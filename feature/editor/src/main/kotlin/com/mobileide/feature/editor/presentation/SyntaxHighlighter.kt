package com.mobileide.editor.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

val kotlinKeywords = setOf("fun", "val", "var", "if", "else", "when", "return", "class", "import", "package", "private", "public", "internal", "override")

private val keywordRegex = "\\b(${kotlinKeywords.joinToString("|")})\\b".toRegex()
private val stringRegex = "\".*?\"".toRegex()
private val commentRegex = "//.*".toRegex()
private val annotationRegex = "@[A-Za-z]+".toRegex()

fun highlightSyntax(code: String): AnnotatedString {
    return buildAnnotatedString {
        append(code)
        
        // Keywords
        keywordRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFFCF86E8), fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
        
        // String Literale
        stringRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFF6A8759)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Kommentare
        commentRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color.Gray),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
        
        // Annotationen
        annotationRegex.findAll(code).forEach { match ->
            addStyle(
                style = SpanStyle(color = Color(0xFFBBB529)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }
}