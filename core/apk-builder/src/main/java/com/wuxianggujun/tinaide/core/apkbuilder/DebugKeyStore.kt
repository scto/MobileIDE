package com.wuxianggujun.tinaide.core.apkbuilder

import android.content.Context
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.Certificate

/**
 * Manages a debug keystore for signing user-built APKs.
 *
 * A pre-generated `debug.keystore` is bundled in assets and copied to
 * internal storage on first use. Users can optionally supply their own
 * keystore via [fromFile].
 */
object DebugKeyStore {

    private const val TAG = "DebugKeyStore"
    private const val ASSET_NAME = "apk_templates/debug.keystore"
    private const val STORE_PASSWORD = "tinaide"
    private const val KEY_ALIAS = "tinaide-debug"
    private const val KEY_PASSWORD = "tinaide"
    private const val FALLBACK_COMMON_NAME = "TinaIDE Debug"
    private val COMMON_KEYSTORE_TYPES = listOf("PKCS12", "JKS", "BKS")

    /**
     * Returns the debug keystore, copying from assets if necessary.
     * Falls back to generating a fresh keystore when the bundled or cached
     * keystore is unavailable or corrupted.
     */
    @Synchronized
    fun getOrInstall(context: Context): KeyStoreInfo? {
        val targetDir = File(context.filesDir, "apk_templates").apply { mkdirs() }
        val ksFile = File(targetDir, "debug.keystore")
        val keyStoreInfo = defaultKeyStoreInfo(ksFile)

        if (validateDefaultKeyStore(keyStoreInfo)) {
            return keyStoreInfo
        }

        if (ksFile.exists()) {
            Timber.tag(TAG).w(
                "Cached debug keystore is invalid, reinstalling: %s",
                ksFile.absolutePath
            )
            deleteInvalidKeyStore(ksFile)
        }

        installBundled(context, ksFile)?.let { bundledKeyStore ->
            if (validateDefaultKeyStore(bundledKeyStore)) {
                return bundledKeyStore
            }

            Timber.tag(TAG).w(
                "Bundled debug keystore is invalid after install, generating fallback: %s",
                ksFile.absolutePath
            )
            deleteInvalidKeyStore(ksFile)
        }

        return generateFallbackKeyStore(ksFile)
    }

    private fun defaultKeyStoreInfo(file: File): KeyStoreInfo {
        return KeyStoreInfo(file, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
    }

    private fun installBundled(context: Context, ksFile: File): KeyStoreInfo? {
        return try {
            context.assets.open(ASSET_NAME).use { input ->
                ksFile.outputStream().use { output -> input.copyTo(output) }
            }
            Timber.tag(TAG).i("Debug keystore installed to: ${ksFile.absolutePath}")
            defaultKeyStoreInfo(ksFile)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Bundled debug keystore unavailable, will generate fallback")
            null
        }
    }

    private fun generateFallbackKeyStore(ksFile: File): KeyStoreInfo? {
        return try {
            val generated = ApkKeyStoreManager.generateKeyStoreAt(
                outputFile = ksFile,
                params = ApkKeyStoreManager.GenerateParams(
                    fileName = ksFile.name,
                    storePassword = STORE_PASSWORD,
                    keyAlias = KEY_ALIAS,
                    keyPassword = KEY_PASSWORD,
                    commonName = FALLBACK_COMMON_NAME
                )
            )
            if (!validateDefaultKeyStore(generated)) {
                deleteInvalidKeyStore(ksFile)
                return null
            }
            Timber.tag(TAG).i("Generated fallback debug keystore: ${ksFile.absolutePath}")
            generated
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to generate fallback debug keystore")
            null
        }
    }

    private fun validateDefaultKeyStore(keyStoreInfo: KeyStoreInfo): Boolean {
        if (!keyStoreInfo.file.isFile) return false

        return try {
            val keyStore = keyStoreInfo.loadKeyStore()
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                throw IOException("Debug keystore alias $KEY_ALIAS is missing")
            }
            keyStore.getKey(KEY_ALIAS, KEY_PASSWORD.toCharArray())
                ?: throw IOException("Debug keystore key $KEY_ALIAS is missing")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).w(
                e,
                "Debug keystore validation failed: %s",
                keyStoreInfo.file.absolutePath
            )
            false
        }
    }

    private fun deleteInvalidKeyStore(file: File) {
        if (!file.exists()) return

        val deleted = if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
        if (!deleted && file.exists()) {
            Timber.tag(TAG).w("Failed to delete invalid debug keystore: %s", file.absolutePath)
        }
    }

    /** Load a user-provided keystore file. */
    fun fromFile(
        file: File,
        storePassword: String,
        keyAlias: String,
        keyPassword: String
    ): KeyStoreInfo = KeyStoreInfo(file, storePassword, keyAlias, keyPassword)

    /**
     * Compute SHA-1 & SHA-256 fingerprints of the signing certificate associated with
     * `keyStoreInfo.keyAlias`. Returns null on any failure (wrong password, missing alias, etc.).
     */
    fun computeFingerprints(keyStoreInfo: KeyStoreInfo): CertificateFingerprints? {
        return try {
            val keyStore = keyStoreInfo.loadKeyStore()
            val cert: Certificate = keyStore.getCertificate(keyStoreInfo.keyAlias) ?: return null
            val encoded = cert.encoded
            CertificateFingerprints(
                sha1 = formatFingerprint(MessageDigest.getInstance("SHA-1").digest(encoded)),
                sha256 = formatFingerprint(MessageDigest.getInstance("SHA-256").digest(encoded))
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to compute fingerprints for ${keyStoreInfo.file.name}")
            null
        }
    }

    private fun formatFingerprint(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * 3)
        for ((index, byte) in bytes.withIndex()) {
            if (index > 0) builder.append(':')
            val v = byte.toInt() and 0xFF
            builder.append(HEX_CHARS[v ushr 4])
            builder.append(HEX_CHARS[v and 0x0F])
        }
        return builder.toString()
    }

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    data class CertificateFingerprints(
        val sha1: String,
        val sha256: String
    )

    data class KeyStoreInfo(
        val file: File,
        val storePassword: String,
        val keyAlias: String,
        val keyPassword: String
    ) {
        fun loadKeyStore(): KeyStore {
            val fileBytes = file.readBytes()
            val candidateTypes = candidateTypes(fileBytes)
            var lastError: Exception? = null

            for (type in candidateTypes) {
                try {
                    val ks = KeyStore.getInstance(type)
                    ByteArrayInputStream(fileBytes).use { input ->
                        ks.load(input, storePassword.toCharArray())
                    }
                    Timber.tag(TAG).d("Loaded keystore ${file.name} as $type")
                    return ks
                } catch (e: Exception) {
                    if (e is IOException || e is GeneralSecurityException) {
                        lastError = e
                        Timber.tag(TAG).d(e, "Failed to load keystore ${file.name} as $type")
                        continue
                    }
                    throw e
                }
            }

            throw IOException(
                "Unable to load keystore ${file.name} using supported types: ${candidateTypes.joinToString()}",
                lastError
            )
        }

        private fun candidateTypes(fileBytes: ByteArray): List<String> {
            val candidates = linkedSetOf<String>()
            sniffKeyStoreType(fileBytes)?.let(candidates::add)

            when (file.extension.lowercase()) {
                "p12", "pfx" -> candidates.add("PKCS12")
                "jks" -> candidates.add("JKS")
                "bks" -> candidates.add("BKS")
            }

            candidates.add(KeyStore.getDefaultType())
            COMMON_KEYSTORE_TYPES.forEach(candidates::add)
            return candidates.toList()
        }

        private fun sniffKeyStoreType(fileBytes: ByteArray): String? {
            if (fileBytes.size < 4) return null

            val b0 = fileBytes[0].toInt() and 0xFF
            val b1 = fileBytes[1].toInt() and 0xFF
            val b2 = fileBytes[2].toInt() and 0xFF
            val b3 = fileBytes[3].toInt() and 0xFF

            // PKCS#12 文件通常以 ASN.1 SEQUENCE 开头，JKS 则是固定魔数 FEEDFEED。
            return when {
                b0 == 0x30 && (b1 == 0x82 || b1 == 0x80) -> "PKCS12"
                b0 == 0xFE && b1 == 0xED && b2 == 0xFE && b3 == 0xED -> "JKS"
                else -> null
            }
        }
    }
}
