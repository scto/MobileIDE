package com.wuxianggujun.tinaide.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DebugKeyStoreTest {

    @Test
    fun loadKeyStore_readsBundledPkcs12FileWithKeystoreExtension() {
        val keystoreFile = findProjectRoot()
            .resolve("app/src/main/assets/apk_templates/debug.keystore")

        val keyStore = DebugKeyStore.fromFile(
            file = keystoreFile,
            storePassword = "tinaide",
            keyAlias = "tinaide-debug",
            keyPassword = "tinaide"
        ).loadKeyStore()

        assertThat(keyStore.containsAlias("tinaide-debug")).isTrue()
        assertThat(keyStore.getKey("tinaide-debug", "tinaide".toCharArray())).isNotNull()
    }

    @Test
    fun getOrInstall_recoversCorruptedCachedDebugKeystore() {
        val context = RuntimeEnvironment.getApplication()
        val keystoreFile = File(context.filesDir, "apk_templates/debug.keystore").apply {
            parentFile?.mkdirs()
            writeText("corrupted-keystore")
        }

        try {
            val keyStoreInfo = DebugKeyStore.getOrInstall(context)

            assertThat(keyStoreInfo).isNotNull()
            assertThat(keystoreFile.readBytes().contentEquals("corrupted-keystore".encodeToByteArray()))
                .isFalse()

            val keyStore = requireNotNull(keyStoreInfo).loadKeyStore()
            assertThat(keyStore.containsAlias("tinaide-debug")).isTrue()
            assertThat(keyStore.getKey("tinaide-debug", "tinaide".toCharArray())).isNotNull()
        } finally {
            keystoreFile.parentFile?.deleteRecursively()
        }
    }

    private fun findProjectRoot(): File {
        val userDir = requireNotNull(System.getProperty("user.dir")) { "user.dir is not set" }
        var dir = File(userDir).absoluteFile
        while (true) {
            val candidate = dir.resolve("app/src/main/assets/apk_templates/debug.keystore")
            if (dir.resolve("settings.gradle.kts").exists() && candidate.exists()) {
                return dir
            }
            dir = dir.parentFile
                ?: error("Cannot locate project root from $userDir")
        }
    }
}
