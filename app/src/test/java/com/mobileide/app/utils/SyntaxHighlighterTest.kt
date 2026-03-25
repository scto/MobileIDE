package com.mobileide.app.utils

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mobileide.app.data.Language
import com.mobileide.app.ui.theme.ActiveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterTest {

    @Test
    fun testHighlightKotlin() {
        val code = "val x = 1"
        val result = SyntaxHighlighter.highlight(code, Language.KOTLIN)

        // Assert text is unchanged
        assertEquals(code, result.text)

        // Check that 'val' keyword has been highlighted
        val hasKeywordStyle = result.spanStyles.any { span ->
            val isKeywordColor = span.item.color == ActiveTheme.syntaxKeyword
            val isBold = span.item.fontWeight == FontWeight.Bold
            val coversVal = span.start == 0 && span.end == 3
            isKeywordColor && isBold && coversVal
        }
        assertTrue("Expected 'val' to be highlighted with Keyword color and Bold font weight", hasKeywordStyle)
    }

    @Test
    fun testHighlightGradle() {
        // Gradle uses highlightKotlin internally
        val code = "val version = \"1.0\""
        val result = SyntaxHighlighter.highlight(code, Language.GRADLE)

        assertEquals(code, result.text)

        val hasKeywordStyle = result.spanStyles.any { span ->
            val isKeywordColor = span.item.color == ActiveTheme.syntaxKeyword
            val isBold = span.item.fontWeight == FontWeight.Bold
            val coversVal = span.start == 0 && span.end == 3
            isKeywordColor && isBold && coversVal
        }
        assertTrue("Expected 'val' to be highlighted with Keyword color and Bold font weight in GRADLE", hasKeywordStyle)

        // Also verify string highlighting
        val hasStringStyle = result.spanStyles.any { span ->
            span.item.color == ActiveTheme.syntaxString && span.start == 14 && span.end == 19
        }
        assertTrue("Expected string literal to be highlighted with String color in GRADLE", hasStringStyle)
    }

    @Test
    fun testHighlightJava() {
        val code = "public class Main {}"
        val result = SyntaxHighlighter.highlight(code, Language.JAVA)

        assertEquals(code, result.text)

        val hasPublicStyle = result.spanStyles.any { span ->
            val isKeywordColor = span.item.color == ActiveTheme.syntaxKeyword
            val isBold = span.item.fontWeight == FontWeight.Bold
            val coversPublic = span.start == 0 && span.end == 6
            isKeywordColor && isBold && coversPublic
        }
        assertTrue("Expected 'public' to be highlighted with Keyword color and Bold font weight in JAVA", hasPublicStyle)

        val hasClassStyle = result.spanStyles.any { span ->
            val isKeywordColor = span.item.color == ActiveTheme.syntaxKeyword
            val isBold = span.item.fontWeight == FontWeight.Bold
            val coversClass = span.start == 7 && span.end == 12
            isKeywordColor && isBold && coversClass
        }
        assertTrue("Expected 'class' to be highlighted with Keyword color and Bold font weight in JAVA", hasClassStyle)
    }

    @Test
    fun testHighlightXml() {
        val code = "<!-- comment --><tag>"
        val result = SyntaxHighlighter.highlight(code, Language.XML)

        assertEquals(code, result.text)

        val hasCommentStyle = result.spanStyles.any { span ->
            val isCommentColor = span.item.color == ActiveTheme.syntaxComment
            val isItalic = span.item.fontStyle == FontStyle.Italic
            val coversComment = span.start == 0 && span.end == 16
            isCommentColor && isItalic && coversComment
        }
        assertTrue("Expected '<!-- comment -->' to be highlighted with Comment color and Italic font style in XML", hasCommentStyle)

        val hasTagStyle = result.spanStyles.any { span ->
            val isKeywordColor = span.item.color == ActiveTheme.syntaxKeyword
            val isBold = span.item.fontWeight == FontWeight.Bold
            val coversTag = span.start == 16 && span.end == 20
            isKeywordColor && isBold && coversTag
        }
        assertTrue("Expected '<tag' to be highlighted with Keyword color and Bold font weight in XML", hasTagStyle)
    }

    @Test
    fun testHighlightJson() {
        val code = "{\"key\": 123}"
        val result = SyntaxHighlighter.highlight(code, Language.JSON)

        assertEquals(code, result.text)

        val hasKeyStyle = result.spanStyles.any { span ->
            val isFunctionColor = span.item.color == ActiveTheme.syntaxFunction
            val coversKey = span.start == 1 && span.end == 6 // "key"
            isFunctionColor && coversKey
        }
        assertTrue("Expected '\"key\"' to be highlighted with Function color in JSON", hasKeyStyle)

        val hasNumberStyle = result.spanStyles.any { span ->
            val isNumberColor = span.item.color == ActiveTheme.syntaxNumber
            val coversNumber = span.start == 8 && span.end == 11 // 123
            isNumberColor && coversNumber
        }
        assertTrue("Expected '123' to be highlighted with Number color in JSON", hasNumberStyle)
    }

    @Test
    fun testHighlightUnknownLanguageFallback() {
        // Assuming PLAIN or PYTHON isn't explicitly handled in when block other than `else`
        val code = "print('hello')"
        val result = SyntaxHighlighter.highlight(code, Language.PLAIN)

        assertEquals(code, result.text)
        assertTrue("Expected no spans for fallback language", result.spanStyles.isEmpty())
    }
}
