package com.scto.mobile.ide.core.apkbuilder

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

data class KeystoreDetailInfo(
    val file: File,
    val aliasList: List<String>,
    val firstAlias: String?,
    val validFrom: String?,
    val validUntil: String?,
    val sha256Fingerprint: String?,
    val isPKCS12OrJKS: Boolean
)

object KeystoreManager {

    data class CreateParams(
        val name: String,
        val alias: String,
        val storePassword: String,
        val keyPassword: String,
        val validityYears: Int = 25,
        val commonName: String,
        val organizationUnit: String = "MobileIDE",
        val organization: String = "MobileIDE",
        val locality: String = "Local",
        val state: String = "State",
        val countryCode: String = "DE"
    )

    suspend fun createKeystore(
        projectPath: String,
        params: CreateParams
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val keystoresDir = File(projectPath, "keystores").apply { mkdirs() }
            val fileName = if (params.name.endsWith(".jks") || params.name.endsWith(".keystore")) params.name else "${params.name}.jks"
            val targetFile = File(keystoresDir, fileName)

            val keyPair = KeyPairGenerator.getInstance("RSA").run {
                initialize(2048)
                generateKeyPair()
            }
            val now = System.currentTimeMillis()
            val validityMillis = params.validityYears.toLong() * 365L * 24L * 60L * 60L * 1000L

            val dnString = "CN=${escapeDn(params.commonName)},OU=${escapeDn(params.organizationUnit)},O=${escapeDn(params.organization)},L=${escapeDn(params.locality)},ST=${escapeDn(params.state)},C=${escapeDn(params.countryCode)}"
            val subject = X500Name(dnString)

            val certBuilder = JcaX509v3CertificateBuilder(
                subject,
                BigInteger(160, SecureRandom()),
                Date(now - 60_000L),
                Date(now + validityMillis),
                subject,
                keyPair.public
            )

            val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
            val cert = JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, params.storePassword.toCharArray())
                setKeyEntry(
                    params.alias,
                    keyPair.private,
                    params.keyPassword.toCharArray(),
                    arrayOf(cert)
                )
            }

            targetFile.outputStream().use { output ->
                keyStore.store(output, params.storePassword.toCharArray())
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listKeystores(projectPath: String): List<File> {
        val rootDir = File(projectPath)
        if (!rootDir.exists()) return emptyList()

        val results = mutableListOf<File>()
        fun scan(dir: File) {
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.isDirectory) {
                    if (f.name != ".gradle" && f.name != ".git" && f.name != "build") {
                        scan(f)
                    }
                } else if (f.name.endsWith(".jks") || f.name.endsWith(".keystore") || f.name.endsWith(".p12")) {
                    results.add(f)
                }
            }
        }
        scan(rootDir)
        return results
    }

    fun getKeystoreDetails(keystoreFile: File, storePass: String): KeystoreDetailInfo? {
        if (!keystoreFile.exists()) return null
        return try {
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            FileInputStream(keystoreFile).use { input ->
                ks.load(input, storePass.toCharArray())
            }
            val aliases = ks.aliases().toList()
            val firstAlias = aliases.firstOrNull()
            var validFrom: String? = null
            var validUntil: String? = null
            var sha256: String? = null

            if (firstAlias != null) {
                val cert = ks.getCertificate(firstAlias) as? X509Certificate
                if (cert != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    validFrom = sdf.format(cert.notBefore)
                    validUntil = sdf.format(cert.notAfter)
                    val digest = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
                    sha256 = digest.joinToString(":") { "%02X".format(it) }
                }
            }

            KeystoreDetailInfo(
                file = keystoreFile,
                aliasList = aliases,
                firstAlias = firstAlias,
                validFrom = validFrom,
                validUntil = validUntil,
                sha256Fingerprint = sha256,
                isPKCS12OrJKS = true
            )
        } catch (e: Exception) {
            null
        }
    }

    fun validateKeystore(keystoreFile: File, storePass: String): Boolean {
        return getKeystoreDetails(keystoreFile, storePass) != null
    }

    fun deleteKeystore(keystoreFile: File): Boolean {
        return if (keystoreFile.exists()) keystoreFile.delete() else false
    }

    private fun escapeDn(value: String): String {
        return value.replace(",", "\\,").replace("=", "\\=")
    }
}
