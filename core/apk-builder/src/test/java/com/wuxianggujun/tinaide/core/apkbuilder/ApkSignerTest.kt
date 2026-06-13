package com.wuxianggujun.tinaide.core.apkbuilder

import com.android.apksig.ApkVerifier
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ApkSignerTest {

    @Test
    fun sign_outputsApkVerifiedByV2AndV3OnSupportedPlatforms() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val workingDir = File(context.cacheDir, "apk-signer-test-${System.nanoTime()}").apply {
            mkdirs()
        }

        try {
            val inputApk = File(workingDir, "input.apk").also(::createUnsignedApk)
            val outputApk = File(workingDir, "output.apk")
            val keyStoreInfo = ApkKeyStoreManager.generateKeyStore(
                targetDir = workingDir,
                params = ApkKeyStoreManager.GenerateParams(
                    fileName = "signing",
                    storePassword = "password123",
                    keyAlias = "signing",
                    keyPassword = "password123",
                    commonName = "Signing Test"
                )
            )

            ApkSigner.sign(
                input = inputApk,
                output = outputApk,
                keyStoreInfo = keyStoreInfo,
                minSdkVersion = 28
            )

            val v2Result = ApkVerifier.Builder(outputApk)
                .setMinCheckedPlatformVersion(24)
                .build()
                .verify()
            val v3Result = ApkVerifier.Builder(outputApk)
                .setMinCheckedPlatformVersion(28)
                .build()
                .verify()

            assertThat(v2Result.isVerified).isTrue()
            assertThat(v2Result.isVerifiedUsingV1Scheme).isFalse()
            assertThat(v2Result.isVerifiedUsingV2Scheme).isTrue()

            assertThat(v3Result.isVerified).isTrue()
            assertThat(v3Result.isVerifiedUsingV1Scheme).isFalse()
            assertThat(v3Result.isVerifiedUsingV3Scheme).isTrue()
        } finally {
            workingDir.deleteRecursively()
        }
    }

    private fun createUnsignedApk(file: File) {
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            writeZipEntry(zip, "AndroidManifest.xml", byteArrayOf(0x03, 0x00, 0x08, 0x00))
            writeZipEntry(zip, "classes.dex", ByteArray(64) { index -> index.toByte() })
            writeZipEntry(zip, "lib/arm64-v8a/libmain.so", ByteArray(128) { 0x42 })
        }
    }

    private fun writeZipEntry(
        zip: ZipOutputStream,
        name: String,
        data: ByteArray
    ) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(data)
        zip.closeEntry()
    }
}
