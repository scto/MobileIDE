package com.wuxianggujun.tinaide.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApkBuilderTemplateReplacementTest {

    @Test
    fun `shouldSkipTemplateLibEntry skips template library when replacement exists`() {
        val shouldSkip = ApkBuilder.shouldSkipTemplateLibEntry(
            entryName = "lib/arm64-v8a/libSDL3.so",
            replacementLibraryNames = setOf("libmain.so", "libSDL3.so")
        )

        assertThat(shouldSkip).isTrue()
    }

    @Test
    fun `shouldSkipTemplateLibEntry keeps unrelated template entry`() {
        val shouldSkip = ApkBuilder.shouldSkipTemplateLibEntry(
            entryName = "assets/config.json",
            replacementLibraryNames = setOf("libmain.so", "libSDL3.so")
        )

        assertThat(shouldSkip).isFalse()
    }
}
