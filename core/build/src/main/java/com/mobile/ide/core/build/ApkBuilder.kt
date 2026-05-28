// Copyright 2025 Thomas Schmid
package com.mobile.ide.core.build

import android.content.Context

import com.Day.Studio.Function.ApkXmlEditor
import com.Day.Studio.Function.axmleditor.decode.AXMLDoc
import com.Day.Studio.Function.axmleditor.editor.PermissionEditor

import com.mcal.apksigner.ApkSigner

import com.mobile.ide.core.utils.LogCatcher

import java.io.*
import java.util.zip.*

object ApkBuilder {
    private const val ICON_RES_1 = "res/MO.webp"
    private const val ICON_RES_2 = "res/fq.webp"

    private data class AppConfig(
        var appName: String = "WebApp",
        var appPackage: String = "com.example.webapp",
        var versionName: String = "1.0.0",
        var versionCode: String = "1",
        var iconPath: String? = null,
        val permissions: MutableList<String> = mutableListOf()
    )

    fun bin(
        context: Context,
        mRootDir: String,
        projectPath: String,
        aname: String,
        pkg: String,
        ver: String,
        code: String,
        amph: String?,
        ps: Array<String>?
    ): String {
        val bf = File(projectPath, "build").apply { if (!exists()) mkdirs() }
        val templateApk = File(context.cacheDir, "webapp_1.0.apk")
        val rawZipFile = File(bf, "temp_raw.zip")
        val alignedZipFile = File(bf, "temp_aligned.apk")
        val finalApkFile = File(bf, "${aname}_release.apk")

        try {
            val config = AppConfig(aname, pkg, ver, code, if (!amph.isNullOrEmpty() && File(amph).exists()) amph else null).apply {
                ps?.let { permissions.addAll(it) }
            }

            if (!copyAssetFile(context, "webapp_1.0.apk", templateApk)) return "error: Template not found"
            
            mergeApk(templateApk, rawZipFile, projectPath, config)
            ZipAligner.align(rawZipFile, alignedZipFile)

            val signaturePath = File(mRootDir, "WebIDE.jks").absolutePath
            val signResult = signerApk(signaturePath, "WebIDE", "WebIDE", "WebIDE", alignedZipFile.absolutePath, finalApkFile.absolutePath)

            rawZipFile.delete(); alignedZipFile.delete()
            return if (signResult) finalApkFile.absolutePath else "error: Signing failed"
        } catch (e: Exception) { return "error: ${e.message}" }
    }

    private fun mergeApk(templateFile: File, outputFile: File, projectPath: String, config: AppConfig) {
        ZipFile(templateFile).use { zipFile ->
            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                zos.setLevel(5)
                zipFile.getEntry("resources.arsc")?.let { copyAsStored(zipFile, it, zos) }
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (name == "resources.arsc" || name.startsWith("META-INF/") || name.startsWith("assets/")) continue
                    if (name == "AndroidManifest.xml") { processManifest(zipFile, entry, zos, config); continue }
                    if (config.iconPath != null && (name == ICON_RES_1 || name == ICON_RES_2)) {
                        zos.putNextEntry(ZipEntry(name))
                        FileInputStream(File(config.iconPath!!)).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                        continue
                    }
                    zos.putNextEntry(ZipEntry(name))
                    zipFile.getInputStream(entry).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                File(projectPath, "src/main/assets").takeIf { it.exists() }?.let { addProjectFilesRecursively(zos, it, "assets") }
            }
        }
    }

    /**
     * Korrigierte Signier-Schnittstelle
     */
    fun signerApk(keyPath: String, pass: String, alias: String, keyPass: String, inPath: String, outPath: String): Boolean {
        return try {
            val signer = ApkSigner(File(inPath), File(outPath))
            // Wir konfigurieren die Flags hier, wenn nötig
            signer.v1SigningEnabled = true
            signer.v2SigningEnabled = true
            signer.signRelease(File(keyPath), pass, alias, keyPass)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    /*
    fun signerApk(keyPath: String, pass: String, alias: String, keyPass: String, inPath: String, outPath: String): Boolean {
        return try {
            val signer = ApkSigner(File(inPath), File(outPath))
            signer.signRelease(File(keyPath), pass, alias, keyPass)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    */
    
    private fun processManifest(zipFile: ZipFile, entry: ZipEntry, zos: ZipOutputStream, config: AppConfig) {
        val tempManifest = File.createTempFile("TempManifest", ".xml")
        zipFile.getInputStream(entry).use { inputStream -> FileOutputStream(tempManifest).use { inputStream.copyTo(it) } }
        try {
            ApkXmlEditor.setXmlPaht(tempManifest.absolutePath)
            ApkXmlEditor.setAppName(config.appName)
            ApkXmlEditor.setAppPack(config.appPackage)
            ApkXmlEditor.setAppbcode(config.versionCode.toIntOrNull() ?: 1)
            ApkXmlEditor.setAppbname(config.versionName)
            ApkXmlEditor.operation()
            config.permissions.forEach { setPermission(tempManifest.absolutePath, it, false) }
            ProviderAuthReplacer.fixProviderConflicts(tempManifest, config.appPackage)
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            FileInputStream(tempManifest).use { it.copyTo(zos) }
            zos.closeEntry()
        } finally { tempManifest.delete() }
    }

    fun setPermission(path: String, permission: String, remove: Boolean) {
        try {
            val file = File(path)
            val doc = AXMLDoc().apply { parse(FileInputStream(file)) }
            val pe = PermissionEditor(doc)
            val op = PermissionEditor.PermissionOpera(permission)
            pe.setEditorInfo(PermissionEditor.EditorInfo().with(if (remove) op.remove() else op.add()))
            pe.commit()
            FileOutputStream(file).use { doc.build(it) }
            doc.release()
        } catch (e: Exception) { LogCatcher.e("ApkBuilder", "Failed to modify permission: $permission", e) }
    }

    private fun addProjectFilesRecursively(zos: ZipOutputStream, file: File, zipPath: String) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { addProjectFilesRecursively(zos, it, "$zipPath/${it.name}") }
        } else {
            zos.putNextEntry(ZipEntry(zipPath))
            FileInputStream(file).use { fis -> fis.copyTo(zos) }
            zos.closeEntry()
        }
    }

    private fun copyAsStored(zipFile: ZipFile, entry: ZipEntry, zos: ZipOutputStream) {
        val data = zipFile.getInputStream(entry).use { it.readBytes() }
        val crc = CRC32().apply { update(data) }
        zos.putNextEntry(ZipEntry("resources.arsc").apply {
            method = ZipEntry.STORED
            size = data.size.toLong()
            compressedSize = data.size.toLong()
            setCrc(crc.value)
        })
        zos.write(data)
        zos.closeEntry()
    }

    private fun copyAssetFile(ctx: Context, name: String, dest: File): Boolean = try {
        ctx.assets.open(name).use { input -> FileOutputStream(dest).use { input.copyTo(it) } }
        true
    } catch (e: Exception) { false }
}