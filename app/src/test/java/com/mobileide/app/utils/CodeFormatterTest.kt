package com.mobileide.app.utils

import org.junit.Test
import org.junit.Assert.*
import java.io.File

class CodeFormatterTest {

    @Test
    fun testFindTodos() {
        val tempDir = File("build/tmp/test_todos")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        val file = File(tempDir, "Test.kt")
        file.writeText("""
            // TODO: task 1
            fun main() {
                // FIXME: task 2
                println("Hello") // NOTE: task 3
            }
        """.trimIndent())

        val todos = CodeFormatter.findTodos(tempDir)
        assertEquals(3, todos.size)

        assertEquals("task 1", todos[0].text)
        assertEquals(1, todos[0].line)

        assertEquals("task 2", todos[1].text)
        assertEquals(3, todos[1].line)

        assertEquals("task 3", todos[2].text)
        assertEquals(4, todos[2].line)

        tempDir.deleteRecursively()
    }
}
