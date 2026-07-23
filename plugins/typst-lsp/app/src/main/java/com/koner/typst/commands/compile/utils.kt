package com.koner.typst.commands.compile

import com.koner.typst.R
import com.koner.typst.utils.TypstInstallationManager
import com.scto.mobile.ide.MainActivity
import com.scto.mobile.ide.exec.ShellUtils
import com.rk.extension.ActivityProvider
import com.rk.extension.ExtensionContext
import com.scto.mobile.ide.core.common.files.FileObject
import com.scto.mobile.ide.core.common.files.FileOperations
import com.scto.mobile.ide.core.common.files.toFileWrapper
import com.rk.resources.fillPlaceholders
import com.scto.mobile.ide.core.common.utils.toast
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun compile(context: ExtensionContext, fileObject: FileObject, targetType: String = "pdf") {
    val featureArgs = if (targetType == "html") arrayOf("--features", "html") else emptyArray()

    val workingDir = fileObject.getParentFile()?.getAbsolutePath()
    val result =
        ShellUtils.runUbuntu(
            workingDir,
            TypstInstallationManager.TYPST_PATH,
            "compile",
            "--format",
            targetType,
            *featureArgs,
            fileObject.getName(),
            timeoutSeconds = 5,
        )

    if (result.timedOut || result.exitCode != 0) {
        context.logError("Compile error: \n${result.error}")
        toast(context.resources.getString(R.string.compile_error).fillPlaceholders(result.error))
    } else {
        context.logInfo("Compile success: \n${result.output}")

        val compiledName = fileObject.getAbsolutePath().substringBeforeLast(".") + ".$targetType"
        val compiledFile = File(compiledName).toFileWrapper()

        toast(context.resources.getString(R.string.compile_success).fillPlaceholders(compiledName))

        withContext(Dispatchers.Main) {
            if (targetType == "pdf") {
                ActivityProvider.currentActivity?.let {
                    FileOperations.openWithExternalApp(
                        it,
                        compiledFile,
                    )
                }
                return@withContext
            }

            MainActivity.instance?.viewModel?.editorManager?.apply {
                openFile(compiledFile, null, true)
            }
        }
    }
}
