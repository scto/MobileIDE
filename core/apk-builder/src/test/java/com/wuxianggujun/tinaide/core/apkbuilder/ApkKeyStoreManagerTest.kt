package com.wuxianggujun.tinaide.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ApkKeyStoreManagerTest {

    @Test
    fun generateKeyStore_createsLoadablePkcs12File() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val targetDir = File(context.cacheDir, "apk-keystore-test-${System.nanoTime()}").apply {
            mkdirs()
        }

        try {
            val keyStoreInfo = ApkKeyStoreManager.generateKeyStore(
                targetDir = targetDir,
                params = ApkKeyStoreManager.GenerateParams(
                    fileName = "release-signing",
                    storePassword = "password123",
                    keyAlias = "release",
                    keyPassword = "password123",
                    commonName = "Release Signing"
                )
            )

            assertThat(keyStoreInfo.file.exists()).isTrue()
            assertThat(keyStoreInfo.file.extension).isEqualTo("p12")

            val keyStore = keyStoreInfo.loadKeyStore()
            assertThat(keyStore.containsAlias("release")).isTrue()
            assertThat(keyStore.getKey("release", "password123".toCharArray())).isNotNull()
        } finally {
            targetDir.deleteRecursively()
        }
    }
}
