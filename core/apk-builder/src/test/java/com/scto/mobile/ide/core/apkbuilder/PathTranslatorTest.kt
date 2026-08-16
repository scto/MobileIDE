package com.scto.mobile.ide.core.apkbuilder

import org.junit.Assert.assertEquals
import org.junit.Test

class PathTranslatorTest {

    @Test
    fun testToSandboxPath() {
        assertEquals("/sdcard/MobileIDEProjects/MyApp", PathTranslator.toSandboxPath("/storage/emulated/0/MobileIDEProjects/MyApp"))
        assertEquals("/sdcard/MobileIDEProjects/MyApp", PathTranslator.toSandboxPath("/storage/emulated/10/MobileIDEProjects/MyApp"))
        assertEquals("/sdcard/MobileIDEProjects/MyApp", PathTranslator.toSandboxPath("/sdcard/MobileIDEProjects/MyApp"))
        assertEquals("/sdcard/MobileIDEProjects/My App", PathTranslator.toSandboxPath("/storage/emulated/0/MobileIDEProjects/My App"))
        assertEquals("/storage/emulated/0/a/b/c/d", PathTranslator.toHostPath("/sdcard/a/b/c/d"))
    }

    @Test
    fun testToHostPath() {
        assertEquals("/storage/emulated/0/MobileIDEProjects/MyApp", PathTranslator.toHostPath("/sdcard/MobileIDEProjects/MyApp"))
        assertEquals("/storage/emulated/0/MobileIDEProjects/My App", PathTranslator.toHostPath("/sdcard/MobileIDEProjects/My App"))
    }
}
