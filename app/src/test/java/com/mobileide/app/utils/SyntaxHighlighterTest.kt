package com.mobileide.app.utils

import com.mobileide.app.data.OutlineSymbol
import com.mobileide.app.data.SymbolKind
import org.junit.Assert.assertEquals
import org.junit.Test

class SyntaxHighlighterTest {

    @Test
    fun extractOutline_identifiesClasses() {
        val code = """
            class SimpleClass
            data class DataClass(val a: Int)
            sealed class SealedClass
            abstract class AbstractClass
        """.trimIndent()

        val expected = listOf(
            OutlineSymbol("SimpleClass", SymbolKind.CLASS, 1),
            OutlineSymbol("DataClass", SymbolKind.CLASS, 2),
            OutlineSymbol("SealedClass", SymbolKind.CLASS, 3),
            OutlineSymbol("AbstractClass", SymbolKind.CLASS, 4)
        )

        val actual = SyntaxHighlighter.extractOutline(code)
        assertEquals(expected, actual)
    }

    @Test
    fun extractOutline_identifiesObjects() {
        val code = """
            object Singleton
            companion object Factory
        """.trimIndent()

        // The regex `^(object)\s+(\w+)` only captures `object Singleton`, not `companion object`
        val expected = listOf(
            OutlineSymbol("Singleton", SymbolKind.OBJECT, 1)
        )

        val actual = SyntaxHighlighter.extractOutline(code)
        assertEquals(expected, actual)
    }

    @Test
    fun extractOutline_identifiesInterfacesAndEnums() {
        val code = """
            interface IService
            enum class Status
        """.trimIndent()

        val expected = listOf(
            OutlineSymbol("IService", SymbolKind.INTERFACE, 1),
            OutlineSymbol("Status", SymbolKind.ENUM, 2)
        )

        val actual = SyntaxHighlighter.extractOutline(code)
        assertEquals(expected, actual)
    }

    @Test
    fun extractOutline_identifiesFunctions() {
        val code = """
            fun main() { }
                fun indentedFun() { }
        """.trimIndent()

        val expected = listOf(
            OutlineSymbol("main", SymbolKind.FUNCTION, 1),
            OutlineSymbol("indentedFun", SymbolKind.FUNCTION, 2)
        )

        val actual = SyntaxHighlighter.extractOutline(code)
        assertEquals(expected, actual)
    }

    @Test
    fun extractOutline_identifiesPropertiesAndVariables() {
        val code = """
            val constant = 42
            var mutable = 0
                val indentedVal = 1
                var indentedVar = 2
        """.trimIndent()

        val expected = listOf(
            OutlineSymbol("constant", SymbolKind.PROPERTY, 1),
            OutlineSymbol("mutable", SymbolKind.VARIABLE, 2),
            OutlineSymbol("indentedVal", SymbolKind.PROPERTY, 3),
            OutlineSymbol("indentedVar", SymbolKind.VARIABLE, 4)
        )

        val actual = SyntaxHighlighter.extractOutline(code)
        assertEquals(expected, actual)
    }

    @Test
    fun extractOutline_handlesEmptyCode() {
        val actual = SyntaxHighlighter.extractOutline("")
        assertEquals(emptyList<OutlineSymbol>(), actual)
    }

    @Test
    fun extractOutline_handlesCodeWithoutSymbols() {
        val code = """
            // This is a comment
            println("Hello World")
            /* Block comment */
        """.trimIndent()

        val actual = SyntaxHighlighter.extractOutline(code)
        assertEquals(emptyList<OutlineSymbol>(), actual)
    }

    @Test
    fun extractOutline_sortsByLineNumber() {
        val code = """
            val first = 1
            class Middle
            fun last() {}
        """.trimIndent()

        val expected = listOf(
            OutlineSymbol("first", SymbolKind.PROPERTY, 1),
            OutlineSymbol("Middle", SymbolKind.CLASS, 2),
            OutlineSymbol("last", SymbolKind.FUNCTION, 3)
        )

        val actual = SyntaxHighlighter.extractOutline(code)
        assertEquals(expected, actual)
    }
}
