package com.wuxianggujun.tinaide.core.apkbuilder

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

object ApkKeyStoreManager {

    private const val DEFAULT_EXTENSION = "p12"
    private const val DEFAULT_ORGANIZATION = "TinaIDE"
    private const val DEFAULT_ORGANIZATION_UNIT = "APK Builder"
    private const val DEFAULT_COUNTRY_CODE = "CN"
    private const val KEY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
    private const val KEY_SIZE = 2048
    private const val VALIDITY_DAYS = 36500L

    data class GenerateParams(
        val fileName: String,
        val storePassword: String,
        val keyAlias: String,
        val keyPassword: String,
        val commonName: String
    )

    suspend fun importKeyStore(
        context: Context,
        uri: Uri,
        targetDir: File
    ): File = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        val sourceName = queryDisplayName(context, uri)
            ?: "imported-keystore-${System.currentTimeMillis()}.$DEFAULT_EXTENSION"
        val safeName = ensureExtension(sanitizeFileName(sourceName))
        val targetFile = uniqueFile(targetDir, safeName)

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open selected keystore")
        inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        targetFile
    }

    suspend fun generateKeyStore(
        targetDir: File,
        params: GenerateParams
    ): DebugKeyStore.KeyStoreInfo = withContext(Dispatchers.IO) {
        targetDir.mkdirs()
        val safeFileName = ensureExtension(sanitizeFileName(params.fileName))
        generateKeyStoreAt(uniqueFile(targetDir, safeFileName), params)
    }

    internal fun generateKeyStoreAt(
        outputFile: File,
        params: GenerateParams
    ): DebugKeyStore.KeyStoreInfo {
        outputFile.parentFile?.mkdirs()
        val keyPair = KeyPairGenerator.getInstance(KEY_ALGORITHM).run {
            initialize(KEY_SIZE)
            generateKeyPair()
        }
        val now = System.currentTimeMillis()
        val subject = X500Name(
            buildDn(
                commonName = params.commonName,
                organizationUnit = DEFAULT_ORGANIZATION_UNIT,
                organization = DEFAULT_ORGANIZATION,
                countryCode = DEFAULT_COUNTRY_CODE
            )
        )
        val certificateBuilder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger(160, SecureRandom()),
            Date(now - 60_000L),
            Date(now + VALIDITY_DAYS * 24L * 60L * 60L * 1000L),
            subject,
            keyPair.public
        )
        val signer = JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
            .build(keyPair.private)
        val certificate = JcaX509CertificateConverter()
            .getCertificate(certificateBuilder.build(signer))

        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, params.storePassword.toCharArray())
            setKeyEntry(
                params.keyAlias,
                keyPair.private,
                params.keyPassword.toCharArray(),
                arrayOf(certificate)
            )
        }
        outputFile.outputStream().use { output ->
            keyStore.store(output, params.storePassword.toCharArray())
        }

        return DebugKeyStore.fromFile(
            file = outputFile,
            storePassword = params.storePassword,
            keyAlias = params.keyAlias,
            keyPassword = params.keyPassword
        )
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }

    private fun buildDn(
        commonName: String,
        organizationUnit: String,
        organization: String,
        countryCode: String
    ): String {
        return listOf(
            "CN=${escapeDnValue(commonName)}",
            "OU=${escapeDnValue(organizationUnit)}",
            "O=${escapeDnValue(organization)}",
            "C=${escapeDnValue(countryCode)}"
        ).joinToString(",")
    }

    private fun escapeDnValue(value: String): String {
        return buildString(value.length) {
            value.trim().forEach { char ->
                if (char in ",+\"\\<>;=") append('\\')
                append(char)
            }
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        val trimmed = fileName.trim()
        val sanitized = buildString(trimmed.length) {
            trimmed.forEach { char ->
                append(
                    when {
                        char.isLetterOrDigit() || char == '.' || char == '_' || char == '-' -> char
                        else -> '_'
                    }
                )
            }
        }.trim('_', '.')
        return sanitized.ifBlank { "apk-signing-key.$DEFAULT_EXTENSION" }
    }

    private fun ensureExtension(fileName: String): String {
        return if ('.' in fileName) fileName else "$fileName.$DEFAULT_EXTENSION"
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate

        val dotIndex = fileName.lastIndexOf('.')
        val baseName = if (dotIndex >= 0) fileName.substring(0, dotIndex) else fileName
        val extension = if (dotIndex >= 0) fileName.substring(dotIndex) else ""
        var index = 2
        while (candidate.exists()) {
            candidate = File(dir, "$baseName-$index$extension")
            index++
        }
        return candidate
    }
}
