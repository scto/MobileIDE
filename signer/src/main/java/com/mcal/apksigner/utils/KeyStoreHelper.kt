package com.mcal.apksigner.utils

import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.io.File
import java.io.FileInputStream
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Locale

object KeyStoreHelper {
    init {
        // Hier wird nun der offizielle BouncyCastleProvider registriert
        Security.addProvider(BouncyCastleProvider())
    }

    @JvmStatic
    fun loadPrivateKeyFromPk8(pk8File: File): PrivateKey {
        val bytes = pk8File.readBytes()
        val spec = PKCS8EncodedKeySpec(bytes)
        val kf = KeyFactory.getInstance("RSA", "BC")
        return kf.generatePrivate(spec)
    }

    @JvmStatic
    fun loadCertificateFromPem(pemFile: File): X509Certificate {
        val cf = CertificateFactory.getInstance("X.509")
        return FileInputStream(pemFile).use {
            cf.generateCertificate(it) as X509Certificate
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun loadJks(jksFile: File?, password: CharArray): KeyStore {
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(jksFile?.let { FileInputStream(it) }, password)
        return keyStore
    }

    @JvmStatic
    @Throws(Exception::class)
    fun loadBks(bksFile: File?, password: CharArray): KeyStore {
        val keyStore = KeyStore.getInstance("BKS", "BC")
        keyStore.load(bksFile?.let { FileInputStream(it) }, password)
        return keyStore
    }

    @JvmStatic
    @Throws(Exception::class)
    fun loadKeyStore(keystoreFile: File, password: CharArray): KeyStore {
        return if (keystoreFile.path.lowercase(Locale.getDefault()).endsWith(".bks")) {
            loadBks(keystoreFile, password)
        } else {
            loadJks(keystoreFile, password)
        }
    }

    @JvmStatic
    fun validateKeystorePassword(keystoreFile: File, password: String): Boolean {
        return try {
            loadKeyStore(keystoreFile, password.toCharArray())
            true
        } catch (e: Exception) {
            false
        }
    }
}