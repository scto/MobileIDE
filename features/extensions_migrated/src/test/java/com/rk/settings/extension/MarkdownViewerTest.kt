package com.scto.mobile.ide.settings.extension

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MarkdownViewerTest {

    @Test
    fun `removeUnsupportedHtmlTags should keep supported tags`() {
        val markdown =
            """
            # Title
            <br>
            <strong>Bold Text</strong>
            <em>Italic Text</em>
            <blockquote>Quote</blockquote>
            <ul><li>Item</li></ul>
            <p>Paragraph</p>
            <a href="https://example.com">Link</a>
            <h1>H1</h1>
            <h2>H2</h2>
            <h3>H3</h3>
            <h4>H4</h4>
            <h5>H5</h5>
            <h6>H6</h6>
            <code>Inline Code</code>
            <pre>Preformatted</pre>
            """
                .trimIndent()

        val result = removeUnsupportedHtmlTags(markdown)
        assertThat(result).isEqualTo(markdown)
    }

    @Test
    fun `removeUnsupportedHtmlTags should remove unsupported tags`() {
        val markdown =
            """
            <script>alert('hack')</script>
            <div class="test">Content</div>
            <span style="color:red">Colored</span>
            <iframe src="https://evil.com"></iframe>
            <img src="icon.png">
            <style>body { color: red; }</style>
            """
                .trimIndent()

        val result = removeUnsupportedHtmlTags(markdown)
        // Note: The regex removes the tags, but might leave whitespace or content inside.
        // We verify that the tags themselves are gone.
        assertThat(result).contains("alert('hack')")
        assertThat(result).contains("Content")
        assertThat(result).contains("Colored")
        assertThat(result).doesNotContain("<script>")
        assertThat(result).doesNotContain("<div>")
        assertThat(result).doesNotContain("<span>")
        assertThat(result).doesNotContain("<iframe>")
        assertThat(result).doesNotContain("<img>")
        assertThat(result).doesNotContain("<style>")
    }

    @Test
    fun `removeUnsupportedHtmlTags should protect code blocks`() {
        val markdown =
            """
            Hello World

            ```html
            <div class="code">This should stay</div>
            <script>This too</script>
            ```

            Inline `<span>code</span>` should stay.

            And ~~~html
            <iframe>Stays</iframe>
            ~~~
            """
                .trimIndent()

        val result = removeUnsupportedHtmlTags(markdown)

        // Inside code blocks, everything should remain
        assertThat(result).contains("<div class=\"code\">")
        assertThat(result).contains("<script>")
        assertThat(result).contains("<span>code</span>")
        assertThat(result).contains("<iframe>Stays</iframe>")
    }

    @Test
    fun `removeUnsupportedHtmlTags should handle mixed content`() {
        val markdown =
            """
            <p>Supported</p>
            <section>Unsupported</section>
            `<code>Protected</code>`
            <pre>Supported Pre</pre>
            <canvas>Unsupported Canvas</canvas>
            """
                .trimIndent()

        val result = removeUnsupportedHtmlTags(markdown)

        assertThat(result).contains("<p>Supported</p>")
        assertThat(result).contains("Unsupported")
        assertThat(result).doesNotContain("<section>")
        assertThat(result).contains("<code>Protected</code>")
        assertThat(result).contains("<pre>Supported Pre</pre>")
        assertThat(result).contains("Unsupported Canvas")
        assertThat(result).doesNotContain("<canvas>")
    }

    @Test
    fun `removeUnsupportedHtmlTags should handle case insensitive tags`() {
        val markdown = "<STRONG>BOLD</STRONG> <DIV>gone</DIV>"
        val result = removeUnsupportedHtmlTags(markdown)
        assertThat(result).contains("<STRONG>BOLD</STRONG>")
        assertThat(result).doesNotContain("<DIV>")
        assertThat(result).contains("gone")
    }
}
