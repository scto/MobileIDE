package com.mobileide.app.utils

import com.mobileide.app.data.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeFormatterTest {

    @Test
    fun `test formatKotlinJava with simple structure`() {
        val input = """
fun main() {
println("Hello, World!")
if (true) {
println("True")
}
}
        """.trimIndent()

        val expected = """
fun main() {
    println("Hello, World!")
    if (true) {
        println("True")
    }
}
        """.trimIndent()

        val result = CodeFormatter.format(input, Language.KOTLIN)
        assertEquals(expected, result)
    }

    @Test
    fun `test formatKotlinJava with multiple empty lines`() {
        val input = """
fun main() {
println("Hello")



println("World")
}
        """.trimIndent()

        val expected = """
fun main() {
    println("Hello")

    println("World")
}
        """.trimIndent()

        val result = CodeFormatter.format(input, Language.JAVA)
        assertEquals(expected, result)
    }

    @Test
    fun `test formatXml with simple structure`() {
        val input = """
<LinearLayout>
<TextView
android:layout_width="wrap_content"
android:layout_height="wrap_content" />
<!-- Comment -->
</LinearLayout>
        """.trimIndent()

        val result = CodeFormatter.format(input, Language.XML)
        assertEquals("<LinearLayout>\n    <TextView\nandroid:layout_width=\"wrap_content\"\nandroid:layout_height=\"wrap_content\" />\n    <!-- Comment -->\n</LinearLayout>", result)
    }

    @Test
    fun `test formatJson with simple structure`() {
        val input = """{"name":"John","age":30,"city":"New York","skills":["Kotlin","Java"]}"""

        val expected = """
{
    "name": "John",
    "age": 30,
    "city": "New York",
    "skills": [
        "Kotlin",
        "Java"
    ]
}
        """.trimIndent()

        val result = CodeFormatter.format(input, Language.JSON)
        assertEquals(expected, result)
    }

    @Test
    fun `test formatJson with nested structure`() {
        val input = """{"user":{"id":1,"details":{"active":true}}}"""

        val expected = """
{
    "user": {
        "id": 1,
        "details": {
            "active": true
        }
    }
}
        """.trimIndent()

        val result = CodeFormatter.format(input, Language.JSON)
        assertEquals(expected, result)
    }

    @Test
    fun `test formatJson with escaped characters`() {
        val input = """{"message":"Hello \"World\""}"""

        val expected = """
{
    "message": "Hello \"World\""
}
        """.trimIndent()

        val result = CodeFormatter.format(input, Language.JSON)
        assertEquals(expected, result)
    }

    @Test
    fun `test format other language returns original code`() {
        val input = """
def hello():
print("Hello")
        """.trimIndent()

        val result = CodeFormatter.format(input, Language.PYTHON)
        assertEquals(input, result)
    }

    @Test
    fun `test organizeImports`() {
        val input = """
package com.example

import java.util.List
import java.util.ArrayList

class Main {
}
        """.trimIndent()

        val result = CodeFormatter.organizeImports(input)
        assertEquals("package com.example\n\nimport java.util.ArrayList\nimport java.util.List\nclass Main {\n}", result)
    }

    @Test
    fun `test analyze code`() {
        val input = """
package com.example

import java.util.List

/**
 * Main class
 */
class Main {
    fun hello() {
        // Print hello
        println("Hello")
    }
}
        """.trimIndent()

        val stats = CodeFormatter.analyze(input, Language.KOTLIN)

        assertEquals(13, stats.totalLines)
        assertEquals(4, stats.commentLines)
        assertEquals(2, stats.blankLines) // Blank lines: 1 between pkg/import, 1 between import/comment
        assertEquals(7, stats.codeLines)
        // assertEquals(1, stats.classes)
        assertEquals(1, stats.functions)
        assertEquals(1, stats.imports)
    }
}
