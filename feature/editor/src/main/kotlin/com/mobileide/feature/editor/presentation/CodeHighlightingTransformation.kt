package com.mobileide.editor.presentation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Eine VisualTransformation, die die `highlightSyntax`-Logik anwendet.
 */
class SyntaxHighlightingTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Wir nehmen den reinen Text und wenden unsere Hervorhebungsregeln an.
        val highlightedText = highlightSyntax(text.text)

        // Das OffsetMapping bleibt 1:1, da wir keine Zeichen hinzufügen oder entfernen,
        // sondern nur die Styles ändern.
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        }

        return TransformedText(highlightedText, offsetMapping)
    }
}
