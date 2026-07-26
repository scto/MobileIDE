package com.scto.mobile.ide.core.tooling.impl

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object BuildHelper {

    private const val PREF_NAME = "MobileIDE_BuildVariants"

    fun saveLastBuildVariant(context: Context, projectPath: String, variant: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(projectPath, variant)
            .apply()
    }

    fun getLastBuildVariant(context: Context, projectPath: String): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(projectPath, "assembleDebug") ?: "assembleDebug"
    }

    fun findGeneratedApk(projectPath: String): File? {
        val rootDir = File(projectPath)
        if (!rootDir.exists()) return null

        val apkFiles = mutableListOf<File>()

        fun scanDir(dir: File) {
            val children = dir.listFiles() ?: return
            for (file in children) {
                if (file.isDirectory) {
                    if (file.name != ".gradle" && file.name != ".git") {
                        scanDir(file)
                    }
                } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                    apkFiles.add(file)
                }
            }
        }

        val buildOutputsDir = File(rootDir, "app/build/outputs/apk")
        if (buildOutputsDir.exists()) {
            scanDir(buildOutputsDir)
        }
        
        if (apkFiles.isEmpty()) {
            val genericBuildOutputs = File(rootDir, "build/outputs/apk")
            if (genericBuildOutputs.exists()) {
                scanDir(genericBuildOutputs)
            }
        }

        if (apkFiles.isEmpty()) {
            scanDir(rootDir)
        }

        return apkFiles.maxByOrNull { it.lastModified() }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Installation fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun openApkExternal(context: Context, apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "APK öffnen mit...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Datei konnte nicht geöffnet werden: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes >= 1024 * 1024 -> String.format("%.2f MB", sizeInBytes.toDouble() / (1024 * 1024))
            sizeInBytes >= 1024 -> String.format("%.2f KB", sizeInBytes.toDouble() / 1024)
            else -> "$sizeInBytes Bytes"
        }
    }
}
