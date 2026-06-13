package com.wuxianggujun.tinaide.core.apkbuilder

import com.android.apksig.ApkSigner as AndroidApkSigner
import timber.log.Timber
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object ApkSigner {

    private const val TAG = "ApkSigner"
    private const val SIGNER_NAME = "TINAIDE"

    fun sign(
        input: File,
        output: File,
        keyStoreInfo: DebugKeyStore.KeyStoreInfo,
        minSdkVersion: Int? = null
    ) {
        Timber.tag(TAG).d("Signing APK with v2+v3: ${input.name}")

        val keyStore = keyStoreInfo.loadKeyStore()
        val signerConfig = buildSignerConfig(keyStore, keyStoreInfo)

        val builder = AndroidApkSigner.Builder(listOf(signerConfig))
            .setInputApk(input)
            .setOutputApk(output)
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setV4SigningEnabled(false)
            .setDebuggableApkPermitted(true)

        minSdkVersion?.let(builder::setMinSdkVersion)

        builder.build().sign()
        Timber.tag(TAG).i("APK signed with v2+v3: ${output.name}")
    }

    private fun buildSignerConfig(
        keyStore: KeyStore,
        keyStoreInfo: DebugKeyStore.KeyStoreInfo
    ): AndroidApkSigner.SignerConfig {
        val key = keyStore.getKey(
            keyStoreInfo.keyAlias,
            keyStoreInfo.keyPassword.toCharArray()
        ) as? PrivateKey
            ?: throw IllegalStateException("Private key not found for alias ${keyStoreInfo.keyAlias}")

        val certificateChain = keyStore.getCertificateChain(keyStoreInfo.keyAlias)
            ?.map { certificate ->
                certificate as? X509Certificate
                    ?: throw IllegalStateException(
                        "Certificate chain for alias ${keyStoreInfo.keyAlias} contains a non-X509 certificate"
                    )
            }
            .orEmpty()

        require(certificateChain.isNotEmpty()) {
            "Certificate chain not found for alias ${keyStoreInfo.keyAlias}"
        }

        @Suppress("DEPRECATION")
        return AndroidApkSigner.SignerConfig.Builder(
            SIGNER_NAME,
            key,
            certificateChain
        ).build()
    }
}
