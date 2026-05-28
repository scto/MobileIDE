package com.mcal.apksigner

import com.android.apksig.ApkSigner

import com.mcal.apksigner.utils.KeyStoreHelper

import java.io.File
import java.security.PrivateKey
import java.security.cert.X509Certificate

class ApkSigner(private val unsignedApkFile: File, private val signedApkFile: File) {
    var v1SigningEnabled = true
    var v2SigningEnabled = true
    var v3SigningEnabled = true
    var v4SigningEnabled = false

    fun signRelease(keyFile: File, password: String, alias: String, aliasPassword: String): Boolean {
        return try {
            val keystore = KeyStoreHelper.loadKeyStore(keyFile, password.toCharArray())
            val signerConfig = ApkSigner.SignerConfig.Builder(
                "CERT",
                keystore.getKey(alias, aliasPassword.toCharArray()) as PrivateKey,
                listOf(keystore.getCertificate(alias) as X509Certificate)
            ).build()
            runApkSigner(listOf(signerConfig))
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    fun signDebug(): Boolean {
        val pk8Stream = ApkSigner::class.java.getResourceAsStream("/keystore/testkey.pk8") ?: return false
        val x509Stream = ApkSigner::class.java.getResourceAsStream("/keystore/testkey.x509.pem") ?: return false
        val pk8File = File.createTempFile("testkey", "pk8").apply { writeBytes(pk8Stream.readBytes()) }
        val x509File = File.createTempFile("testkey", "x509.pem").apply { writeBytes(x509Stream.readBytes()) }
        return try {
            val signerConfig = ApkSigner.SignerConfig.Builder(
                "DEBUG",
                KeyStoreHelper.loadPrivateKeyFromPk8(pk8File),
                listOf(KeyStoreHelper.loadCertificateFromPem(x509File))
            ).build()
            runApkSigner(listOf(signerConfig))
        } catch (e: Exception) { e.printStackTrace(); false } finally { pk8File.delete(); x509File.delete() }
    }

    private fun runApkSigner(signerConfigs: List<ApkSigner.SignerConfig>): Boolean {
        return try {
            ApkSigner.Builder(signerConfigs)
                .setInputApk(unsignedApkFile)
                .setOutputApk(signedApkFile)
                .setV1SigningEnabled(v1SigningEnabled)
                .setV2SigningEnabled(v2SigningEnabled)
                .setV3SigningEnabled(v3SigningEnabled)
                .setV4SigningEnabled(v4SigningEnabled)
                .build()
                .sign()
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }
}