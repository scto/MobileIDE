package com.scto.mobile.ide.integration

import org.junit.Assert.assertTrue
import org.junit.Test

class AdapterStubsTest {
    @Test
    fun testXedHostStub() {
        assertTrue(XedHost.snackbarHostState != null)
    }

    @Test
    fun testXedTerminalLauncherStub() {
        assertTrue(XedTerminalLauncher != null)
    }
}
