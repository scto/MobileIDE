package com.scto.mobile.ide.settings.extension

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.NotificationCompat
import com.scto.mobile.ide.DefaultScope
import com.scto.mobile.ide.activities.settings.SettingsActivity
import com.scto.mobile.ide.crashhandler.CrashActivity
import com.scto.mobile.ide.features.extensions.EXTENSION_API_BASE
import com.scto.mobile.ide.features.extensions.Extension
import com.scto.mobile.ide.features.extensions.ExtensionError
import com.scto.mobile.ide.features.extensions.ExtensionId
import com.scto.mobile.ide.features.extensions.ICONPACKS_API_BASE
import com.scto.mobile.ide.features.extensions.InstallResult
import com.scto.mobile.ide.features.extensions.LocalExtension
import com.scto.mobile.ide.features.extensions.StoreExtension
import com.scto.mobile.ide.features.extensions.UpdatableExtension
import com.scto.mobile.ide.features.extensions.extensionManager
import com.scto.mobile.ide.features.extensions.loader.LoadScenario
import com.scto.mobile.ide.features.extensions.loader.installExtensionFromZip
import com.scto.mobile.ide.features.extensions.loader.load
import com.scto.mobile.ide.features.extensions.manager.ExtensionRegistry
import com.scto.mobile.ide.file.toFileObject
import com.scto.mobile.ide.resources.drawables
import com.scto.mobile.ide.resources.getFilledString
import com.scto.mobile.ide.resources.getString
import com.scto.mobile.ide.resources.strings
import com.scto.mobile.ide.utils.LoadingPopup
import com.scto.mobile.ide.utils.application
import com.scto.mobile.ide.utils.dialogRes
import com.scto.mobile.ide.utils.errorDialog
import com.scto.mobile.ide.utils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import java.io.File

fun getMissingDependencies(extension: Extension): List<ExtensionId> {
    val missing = linkedSetOf<ExtensionId>()
    val visited = mutableSetOf<ExtensionId>()

    fun collect(ext: Extension) {
        for (depId in ext.dependencies) {
            if (!extensionManager.isInstalled(depId)) {
                val depExt = extensionManager.getExtension(depId)
                if (depExt != null && visited.add(depId)) {
                    collect(depExt)
                }
                missing.add(depId)
            }
        }
    }

    collect(extension)
    return missing.toList()
}

fun getRecommendations(extension: Extension): List<ExtensionId> {
    return extension.recommendations.filter { !extensionManager.isInstalled(it) }
}

fun runExtensionUninstallAction(
    extension: Extension,
    updateInstallState: (InstallState) -> Unit,
    scope: CoroutineScope,
    activity: AppCompatActivity?,
) {
    dialogRes(
        activity = activity,
        title = strings.uninstall_ext_dialog.getString(),
        msg = strings.uninstall_ext_dialog_desc.getFilledString(extension.name),
        okRes = strings.uninstall,
        onOk = {
            scope.launch(Dispatchers.IO) {
                extensionManager.uninstallExtension(extension.id).onFailure { error ->
                    withContext(Dispatchers.Main) {
                        errorDialog(activity, error)
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    updateInstallState(InstallState.Idle)
                }
            }
        },
        onCancel = {},
    )
}

private fun showDownloadNotification(
    context: Context,
    id: String,
    title: String,
    progress: Float,
    isFinished: Boolean = false,
    errorMessage: String? = null,
) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "store_downloads"

    val channel =
        NotificationChannel(
                channelId,
                "Store Downloads",
                NotificationManager.IMPORTANCE_LOW,
            )
            .apply {
                description = "Notifications for store downloads and installations"
            }
    notificationManager.createNotificationChannel(channel)

    val builder =
        NotificationCompat.Builder(context, channelId).setSmallIcon(drawables.extension).setOnlyAlertOnce(true)

    if (isFinished) {
        if (errorMessage != null) {
            builder
                .setContentTitle(strings.install_failed.getString(context))
                .setContentText(errorMessage)
                .setOngoing(false)
                .setAutoCancel(true)
        } else {
            builder
                .setContentTitle(strings.installed.getString(context))
                .setContentText(title)
                .setOngoing(false)
                .setAutoCancel(true)
        }
    } else {
        builder.setContentTitle(title).setOngoing(true)
        if (progress >= 0f) {
            val percent = (progress * 100).toInt()
            builder.setContentText("$percent%").setProgress(100, percent, false)
        } else {
            builder.setContentText(strings.installing.getString(context)).setProgress(100, 0, true)
        }
    }

    runCatching {
        notificationManager.notify(id.hashCode(), builder.build())
    }
}

suspend fun installExtensionSequentially(
    extension: Extension,
    context: Context,
    activity: AppCompatActivity?,
): Boolean = withContext(Dispatchers.IO) {
    val storeExt = extension as? StoreExtension ?: return@withContext false
    val id = storeExt.id
    val name = storeExt.name

    withContext(Dispatchers.Main) {
        ExtensionRegistry.activeInstalls[id] = InstallState.Installing
        ExtensionRegistry.downloadProgress[id] = 0f
        showDownloadNotification(context, id, name, 0f)
    }

    var success = false
    var errorMsg: String? = null
    val tempFile = File(context.cacheDir, "ext_download_${id}.zip")

    try {
        var lastNotificationTime = 0L

        val downloadSuccess =
            ExtensionRegistry.downloadFileWithProgress(
                url = "$EXTENSION_API_BASE/$id/plugin.zip",
                destFile = tempFile,
                onProgress = { progress ->
                    DefaultScope.launch(Dispatchers.Main) {
                        ExtensionRegistry.downloadProgress[id] = progress
                    }
                    val now = System.currentTimeMillis()
                    if (now - lastNotificationTime > 300) {
                        lastNotificationTime = now
                        showDownloadNotification(context, id, name, progress)
                    }
                },
            )

        if (downloadSuccess) {
            showDownloadNotification(context, id, name, 1f)

            when (val result = extensionManager.installExtensionFromZip(tempFile)) {
                is InstallResult.Success -> {
                    extensionManager.setExtensionCrashed(result.extension, false)
                    val resultLoad = result.extension.load(application!!, LoadScenario.INSTALL)
                    if (resultLoad.isFailure) {
                        val error = resultLoad.exceptionOrNull()!!
                        extensionManager.setExtensionCrashed(result.extension, true)
                        errorMsg = error.message ?: "Failed to load extension"
                        withContext(Dispatchers.Main) {
                            activity?.let {
                                CrashActivity.start(
                                    context = it,
                                    extensionId = result.extension.id,
                                    extensionName = result.extension.name,
                                    extensionVersion = result.extension.version,
                                    extensionAuthor = result.extension.author.toString(),
                                    repository = result.extension.repository,
                                    error = error,
                                )
                            } ?: run {
                                errorDialog(null, msg = errorMsg)
                            }
                        }
                    }
                    success = errorMsg == null
                }

                is InstallResult.Error -> {
                    errorMsg =
                        when (result.error) {
                            ExtensionError.OUTDATED_CLIENT -> strings.outdated_client.getString(context)
                        }
                }

                is InstallResult.ValidationFailed -> {
                    errorMsg = result.error?.message ?: "Validation failed"
                }
            }
        } else {
            errorMsg = "Download failed"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        errorMsg = e.message ?: "Unknown error"
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }

        withContext(Dispatchers.Main) {
            ExtensionRegistry.activeInstalls.remove(id)
            ExtensionRegistry.downloadProgress.remove(id)

            if (success) {
                showDownloadNotification(context, id, name, 1f, isFinished = true)
            } else {
                showDownloadNotification(context, id, name, 0f, isFinished = true, errorMessage = errorMsg)
                errorDialog(activity, msg = errorMsg ?: "Unknown error")
            }
        }
    }
    success
}

fun runExtensionInstallAction(
    extension: Extension,
    updateInstallState: (InstallState) -> Unit,
    context: Context,
    activity: AppCompatActivity?,
) {
    updateInstallState(InstallState.Installing)
    DefaultScope.launch {
        val success = installExtensionSequentially(extension, context, activity)
        withContext(Dispatchers.Main) {
            if (success) {
                updateInstallState(InstallState.Installed)
            } else {
                updateInstallState(InstallState.Idle)
            }
        }
    }
}

suspend fun batchInstallExtensions(
    ids: List<ExtensionId>,
    context: Context,
    activity: AppCompatActivity?,
): Boolean {
    for (id in ids) {
        val extension = extensionManager.getExtension(id) ?: continue
        if (!extensionManager.isInstalled(id)) {
            val success = installExtensionSequentially(extension, context, activity)
            if (!success) return false
        }
    }
    return true
}

fun runExtensionUpdateAction(
    extension: UpdatableExtension,
    updateInstallState: (InstallState) -> Unit,
    context: Context,
    activity: AppCompatActivity?,
) {
    val store = extension.store
    val id = store.id
    val name = store.name

    ExtensionRegistry.activeInstalls[id] = InstallState.Updating
    ExtensionRegistry.downloadProgress[id] = 0f
    updateInstallState(InstallState.Updating)

    showDownloadNotification(context, id, name, 0f)

    DefaultScope.launch(Dispatchers.IO) {
        var success = false
        var errorMsg: String? = null
        val tempFile = File(context.cacheDir, "ext_download_${id}.zip")

        try {
            var lastNotificationTime = 0L

            val downloadSuccess =
                ExtensionRegistry.downloadFileWithProgress(
                    url = "$EXTENSION_API_BASE/$id/plugin.zip",
                    destFile = tempFile,
                    onProgress = { progress ->
                        DefaultScope.launch(Dispatchers.Main) {
                            ExtensionRegistry.downloadProgress[id] = progress
                        }
                        val now = System.currentTimeMillis()
                        if (now - lastNotificationTime > 300) {
                            lastNotificationTime = now
                            showDownloadNotification(context, id, name, progress)
                        }
                    },
                )

            if (downloadSuccess) {
                showDownloadNotification(context, id, name, 1f)

                when (val result = extensionManager.installExtensionFromZip(tempFile)) {
                    is InstallResult.Success -> {
                        extensionManager.setExtensionCrashed(result.extension, false)
                        result.extension.load(application!!, LoadScenario.UPDATE).onFailure { error ->
                            extensionManager.setExtensionCrashed(result.extension, true)
                            errorMsg = error.message ?: "Failed to load extension"
                            withContext(Dispatchers.Main) {
                                activity?.let {
                                    CrashActivity.start(
                                        context = it,
                                        extensionId = result.extension.id,
                                        extensionName = result.extension.name,
                                        extensionVersion = result.extension.version,
                                        extensionAuthor = result.extension.author.toString(),
                                        repository = result.extension.repository,
                                        error = error,
                                    )
                                }
                                    ?: run {
                                        errorDialog(activity, msg = errorMsg)
                                    }
                            }
                        }
                        success = errorMsg == null
                    }

                    is InstallResult.Error -> {
                        errorMsg =
                            when (result.error) {
                                ExtensionError.OUTDATED_CLIENT -> strings.outdated_client.getString(context)
                            }
                    }

                    is InstallResult.ValidationFailed -> {
                        errorMsg = result.error?.message ?: "Validation failed"
                    }
                }
            } else {
                errorMsg = "Download failed"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = e.message ?: "Unknown error"
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }

            withContext(Dispatchers.Main) {
                ExtensionRegistry.activeInstalls.remove(id)
                ExtensionRegistry.downloadProgress.remove(id)

                if (success) {
                    showDownloadNotification(context, id, name, 1f, isFinished = true)
                    updateInstallState(InstallState.Installed)
                } else {
                    showDownloadNotification(context, id, name, 0f, isFinished = true, errorMessage = errorMsg)
                    updateInstallState(InstallState.Idle)
                    errorDialog(activity, msg = errorMsg ?: "Unknown error")
                }
            }
        }
    }
}

fun runIconPackInstallAction(
    id: String,
    name: String,
    context: Context,
    activity: AppCompatActivity?,
) {
    ExtensionRegistry.activeInstalls[id] = InstallState.Installing
    ExtensionRegistry.downloadProgress[id] = 0f

    showDownloadNotification(context, id, name, 0f)

    DefaultScope.launch(Dispatchers.IO) {
        var success = false
        var errorMsg: String? = null
        val tempFile = File(context.cacheDir, "iconpack_${id}.zip")

        try {
            var lastNotificationTime = 0L

            val downloadSuccess =
                ExtensionRegistry.downloadFileWithProgress(
                    url = "$ICONPACKS_API_BASE/$id/iconpack.zip",
                    destFile = tempFile,
                    onProgress = { progress ->
                        DefaultScope.launch(Dispatchers.Main) {
                            ExtensionRegistry.downloadProgress[id] = progress
                        }
                        val now = System.currentTimeMillis()
                        if (now - lastNotificationTime > 300) {
                            lastNotificationTime = now
                            showDownloadNotification(context, id, name, progress)
                        }
                    },
                )

            if (downloadSuccess) {
                showDownloadNotification(context, id, name, 1f)
                runCatching {
                    com.scto.mobile.ide.App.Companion.iconPackManager.installIconPack(tempFile)
                }
                    .onSuccess {
                        success = true
                    }
                    .onFailure {
                        errorMsg = it.message ?: "Failed to install icon pack"
                    }
            } else {
                errorMsg = "Download failed"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = e.message ?: "Unknown error"
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }

            withContext(Dispatchers.Main) {
                ExtensionRegistry.activeInstalls.remove(id)
                ExtensionRegistry.downloadProgress.remove(id)

                if (success) {
                    showDownloadNotification(context, id, name, 1f, isFinished = true)
                } else {
                    showDownloadNotification(context, id, name, 0f, isFinished = true, errorMessage = errorMsg)
                    errorDialog(activity, msg = errorMsg ?: "Unknown error")
                }
            }
        }
    }
}

fun installExtensionFromUri(scope: CoroutineScope, uri: Uri?, activity: AppCompatActivity?) {
    var loading: LoadingPopup? = null

    scope.launch(Dispatchers.IO) {
        runCatching {
            if (uri == null) return@runCatching

            val fileObject = uri.toFileObject(expectedIsFile = true)
            val exists = fileObject.exists()
            val canRead = fileObject.canRead()
            val isZip = fileObject.getName().endsWith(".zip")

            if (exists && canRead && isZip) {
                withContext(Dispatchers.Main) {
                    loading = LoadingPopup(activity).show()
                    loading.setMessage(strings.installing.getString())
                }

                val result = extensionManager.installExtensionFromZip(fileObject)

                if (result is InstallResult.Success) {
                    extensionManager.setExtensionCrashed(result.extension, false)
                    val loadScenario = if (result.performedUpdate) LoadScenario.UPDATE else LoadScenario.INSTALL
                    result.extension.load(application!!, loadScenario).onFailure { error ->
                        extensionManager.setExtensionCrashed(result.extension, true)
                        withContext(Dispatchers.Main) {
                            activity?.let {
                                CrashActivity.start(
                                    context = it,
                                    extensionId = result.extension.id,
                                    extensionName = result.extension.name,
                                    extensionVersion = result.extension.version,
                                    extensionAuthor = result.extension.author.toString(),
                                    repository = result.extension.repository,
                                    error = error,
                                )
                            }
                                ?: run {
                                    errorDialog(activity, msg = error.message ?: strings.unknown_error.getString())
                                }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    handleInstallResult(result, activity)
                    loading?.hide()
                }
            } else {
                withContext(Dispatchers.Main) {
                    errorDialog(
                        activity,
                        msg =
                            "Install criteria failed \nis_zip = $isZip\ncan_read = $canRead\n exists = $exists\nuri = ${fileObject.getAbsolutePath()}",
                    )
                }
            }
        }
            .onFailure { error ->
                withContext(Dispatchers.Main) {
                    loading?.hide()
                    errorDialog(activity, error)
                }
            }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun handleInstallResult(
    result: InstallResult,
    activity: Activity?,
    onError: () -> Unit = {},
    onSuccess: (LocalExtension) -> Unit = {},
) =
    when (result) {
        is InstallResult.Error -> {
            when (result.error) {
                ExtensionError.OUTDATED_CLIENT ->
                    errorDialog(activity, strings.install_failed.getString(), strings.outdated_client.getString())
            }
            onError()
        }

        is InstallResult.Success -> {
            toast(strings.installed)
            onSuccess(result.extension)
        }

        is InstallResult.ValidationFailed -> {
            val e = result.error
            if (e is MissingFieldException) {
                val fields = e.missingFields.joinToString("\n") { "• $it" }
                dialogRes(
                    SettingsActivity.instance,
                    strings.extension_validation_failed.getString(),
                    strings.manifest_missing_fields.getFilledString(fields),
                    cancelable = false,
                )
                onError()
            } else {
                errorDialog(
                    activity,
                    e?.localizedMessage ?: strings.unknown_error.getString(),
                    strings.extension_validation_failed.getString(),
                )
                onError()
            }
        }
    }

@Composable
fun rememberInstallState(extension: Extension): InstallState =
    remember(
        extension,
        ExtensionRegistry.activeInstalls[extension.id],
    ) {
        val active = ExtensionRegistry.activeInstalls[extension.id]
        active
            ?: if (extensionManager.isInstalled(extension.id)) {
                if (extension is UpdatableExtension && extension.hasUpdate()) {
                    InstallState.Updatable
                } else {
                    InstallState.Installed
                }
            } else {
                InstallState.Idle
            }
    }
