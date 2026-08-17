package com.scto.mobile.ide.core.tooling.api

import org.junit.Assert.assertEquals
import org.junit.Test

class LogRouterTest {

    @Test
    fun testClassifyInstallCommands() {
        assertEquals(LogChannel.INSTALL, LogRouter.classify("apt-get", arrayOf("install", "-y", "curl"), "apt_install"))
        assertEquals(LogChannel.INSTALL, LogRouter.classify("bash", arrayOf("typst-lsp.sh", "--install"), "Typst Installation"))
    }

    @Test
    fun testClassifyBuildCommands() {
        assertEquals(LogChannel.BUILD, LogRouter.classify("./gradlew", arrayOf("assembleRelease"), "gradle_build"))
    }

    @Test
    fun testClassifyUnclassifiedCommand() {
        assertEquals(LogChannel.IDE_LOGS, LogRouter.classify("zig", arrayOf("run", "main.zig"), "zig.run"))
    }
}
