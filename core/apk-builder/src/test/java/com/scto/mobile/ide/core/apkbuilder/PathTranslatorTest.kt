package com.scto.mobile.ide.core.apkbuilder

import org.junit.Assert.assertEquals
import org.junit.Test

class PathTranslatorTest {

    @Test
    fun testToSandboxPath() {
        assertEquals("/sdcard/MobileIDEProjects/MyApp", PathTranslator.toSandboxPath("/storage/emulated/0/MobileIDEProjects/MyApp"))
        assertEquals("/sdcard/MobileIDEProjects/MyApp", PathTranslator.toSandboxPath("/storage/emulated/10/MobileIDEProjects/MyApp"))
        assertEquals("/sdcard/MobileIDEProjects/MyApp", PathTranslator.toSandboxPath("/sdcard/MobileIDEProjects/MyApp"))
    }

    @Test
    fun testToHostPath() {
        assertEquals("/storage/emulated/0/MobileIDEProjects/MyApp", PathTranslator.toHostPath("/sdcard/MobileIDEProjects/MyApp"))
    }
}
